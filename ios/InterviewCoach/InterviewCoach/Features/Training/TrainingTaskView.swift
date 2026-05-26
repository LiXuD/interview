import SwiftUI

struct TrainingTaskView: View {
    let task: TrainingTaskDTO
    let targetId: String
    let onUpdate: (TrainingTaskDTO) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var feedback: TrainingFeedbackDTO?
    @State private var answerText = ""
    @State private var currentTask: TrainingTaskDTO
    @State private var isLoading = false
    @State private var errorMessage: String?

    init(task: TrainingTaskDTO, targetId: String, onUpdate: @escaping (TrainingTaskDTO) -> Void) {
        self.task = task
        self.targetId = targetId
        self.onUpdate = onUpdate
        self._currentTask = State(initialValue: task)
    }

    var body: some View {
        NavigationStack {
            Form {
                if let errorMessage {
                    Section {
                        ErrorBanner(message: errorMessage)
                    }
                }

                Section("任务") {
                    Text(currentTask.title)
                        .font(.headline)
                    Text(currentTask.description)
                        .font(.body)
                }

                if let feedback {
                    feedbackSection(feedback)
                } else if currentTask.status == "pending" {
                    Section("你的回答") {
                        TextEditor(text: $answerText)
                            .frame(minHeight: 120)
                    }

                    Section {
                        Button {
                            Task { await submitAnswer() }
                        } label: {
                            Label("提交回答", systemImage: "arrow.right.circle.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .disabled(answerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                    }
                }

                if feedback != nil && currentTask.status != "completed" {
                    Section {
                        Button {
                            Task { await completeTask() }
                        } label: {
                            Label("标记完成", systemImage: "checkmark.circle.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .disabled(isLoading)
                    }
                }

                if currentTask.status == "completed" {
                    Section {
                        Label("已完成", systemImage: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .navigationTitle("训练任务")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
            .loadingOverlay(isLoading: isLoading)
        }
    }

    private func feedbackSection(_ fb: TrainingFeedbackDTO) -> some View {
        Group {
            Section("评分") {
                HStack {
                    Text("得分")
                        .font(.headline)
                    Spacer()
                    Text("\(fb.score)")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundStyle(scoreColor(fb.score))
                }
            }

            Section("反馈") {
                Text(fb.feedback)
                    .font(.body)
            }

            if !fb.problems.isEmpty {
                Section("问题") {
                    ForEach(fb.problems, id: \.self) { item in
                        Label(item, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                    }
                }
            }

            Section("优化后的回答") {
                Text(fb.rewrittenAnswer)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }

            Section("追问") {
                Text(fb.followUpQuestion)
                    .font(.body)
                    .italic()
            }

            if !fb.recommendedReviewPoints.isEmpty {
                Section("建议复习") {
                    ForEach(fb.recommendedReviewPoints, id: \.self) { item in
                        Label(item, systemImage: "book.fill")
                            .foregroundStyle(.blue)
                    }
                }
            }
        }
    }

    @MainActor
    private func submitAnswer() async {
        isLoading = true
        errorMessage = nil
        do {
            try await AiRuntimeStatusGuard.requireCoreAiAvailable()
            let trimmed = answerText.trimmingCharacters(in: .whitespacesAndNewlines)
            feedback = try await APIClient.shared.request(
                "POST",
                path: "/api/training-tasks/\(task.id)/answer",
                body: TrainingTaskAnswerRequestDTO(answer: trimmed)
            )
            currentTask = TrainingTaskDTO(
                id: currentTask.id,
                title: currentTask.title,
                description: currentTask.description,
                status: "in_progress",
                feedback: feedback?.feedback,
                completedAt: nil
            )
            onUpdate(currentTask)
        } catch {
            errorMessage = "提交失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    @MainActor
    private func completeTask() async {
        isLoading = true
        errorMessage = nil
        do {
            currentTask = try await APIClient.shared.request(
                "PATCH",
                path: "/api/training-tasks/\(task.id)/complete"
            )
            onUpdate(currentTask)
        } catch {
            errorMessage = "完成失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func scoreColor(_ score: Int) -> Color {
        if score >= 80 { return .green }
        if score >= 60 { return .orange }
        return .red
    }
}

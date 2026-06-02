import SwiftUI

struct TrainingTaskView: View {
    let task: TrainingTaskDTO
    let targetId: String
    let onUpdate: (TrainingTaskDTO) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var feedback: TrainingFeedbackDTO?
    @State private var adaptiveSession: AdaptiveTrainingSessionDTO?
    @State private var answerText = ""
    @State private var adaptiveAnswerText = ""
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
                } else if let adaptiveSession {
                    adaptiveTrainingSection(adaptiveSession)
                } else if currentTask.status == "pending" {
                    Section("自适应训练") {
                        Button {
                            Task { await startAdaptiveTraining() }
                        } label: {
                            Label("开始 2-4 轮自适应训练", systemImage: "sparkles")
                                .frame(maxWidth: .infinity)
                        }
                        .disabled(isLoading)
                    }

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

    private func adaptiveTrainingSection(_ session: AdaptiveTrainingSessionDTO) -> some View {
        Group {
            Section("自适应训练") {
                LabeledContent("轮次", value: "\(session.roundIndex)/\(session.maxRounds)")
                LabeledContent("动作", value: actionTitle(session.lastAction))
                if session.status == "completed" {
                    Label("训练已完成", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                }
            }

            ForEach(session.rounds) { round in
                Section("第 \(round.roundIndex) 轮") {
                    Text(round.question)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(round.answer)
                    Text(round.feedback)
                        .foregroundStyle(.secondary)
                    if !round.problems.isEmpty {
                        ForEach(round.problems, id: \.self) { problem in
                            Label(problem, systemImage: "exclamationmark.triangle.fill")
                                .foregroundStyle(.orange)
                        }
                    }
                }
            }

            if session.status == "completed" {
                Section("总结") {
                    Text(session.summary ?? "本次训练已完成。")
                }
            } else {
                if let question = session.currentQuestion {
                    Section("教练追问") {
                        Text(question)
                            .italic()
                    }
                }

                Section("你的回答") {
                    TextEditor(text: $adaptiveAnswerText)
                        .frame(minHeight: 120)
                }

                Section {
                    Button {
                        Task { await submitAdaptiveAnswer() }
                    } label: {
                        Label("提交本轮回答", systemImage: "arrow.right.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(adaptiveAnswerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
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
                completedAt: nil,
                dayIndex: currentTask.dayIndex
            )
            onUpdate(currentTask)
        } catch {
            errorMessage = "提交失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    @MainActor
    private func startAdaptiveTraining() async {
        isLoading = true
        errorMessage = nil
        do {
            try await AiRuntimeStatusGuard.requireCoreAiAvailable()
            adaptiveSession = try await APIClient.shared.request(
                "POST",
                path: "/api/training-tasks/\(task.id)/adaptive-sessions/start"
            )
            currentTask = TrainingTaskDTO(
                id: currentTask.id,
                title: currentTask.title,
                description: currentTask.description,
                status: "in_progress",
                feedback: nil,
                completedAt: nil,
                dayIndex: currentTask.dayIndex
            )
            onUpdate(currentTask)
        } catch {
            errorMessage = "开始失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    @MainActor
    private func submitAdaptiveAnswer() async {
        guard let adaptiveSession else { return }
        isLoading = true
        errorMessage = nil
        do {
            try await AiRuntimeStatusGuard.requireCoreAiAvailable()
            let trimmed = adaptiveAnswerText.trimmingCharacters(in: .whitespacesAndNewlines)
            let updated: AdaptiveTrainingSessionDTO = try await APIClient.shared.request(
                "POST",
                path: "/api/training-sessions/\(adaptiveSession.id)/answers",
                body: AdaptiveTrainingAnswerRequestDTO(answer: trimmed)
            )
            self.adaptiveSession = updated
            adaptiveAnswerText = ""
            if updated.status == "completed" {
                currentTask = TrainingTaskDTO(
                    id: currentTask.id,
                    title: currentTask.title,
                    description: currentTask.description,
                    status: "completed",
                    feedback: updated.summary,
                    completedAt: Date().ISO8601Format(),
                    dayIndex: currentTask.dayIndex
                )
                onUpdate(currentTask)
            }
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

    private func actionTitle(_ action: String?) -> String {
        switch action {
        case "continue": return "继续追问"
        case "pass": return "已达标"
        case "switch": return "换角度"
        case "stop": return "先讲解"
        default: return "准备中"
        }
    }
}

import SwiftUI

struct TrainingPlanView: View {
    let target: InterviewTargetDTO

    @Environment(\.dismiss) private var dismiss
    @State private var plan: TrainingPlanDTO?
    @State private var selectedTask: TrainingTaskDTO?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if let plan {
                    planListView(plan)
                } else if isLoading {
                    ProgressView("处理中...")
                } else if !isLoading {
                    generateView
                }
            }
            .navigationTitle("训练计划")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
            .loadingOverlay(isLoading: isLoading)
            .sheet(item: $selectedTask) { task in
                TrainingTaskView(task: task, targetId: target.id) { updatedTask in
                    if let plan {
                        var tasks = plan.tasks
                        if let idx = tasks.firstIndex(where: { $0.id == updatedTask.id }) {
                            tasks[idx] = updatedTask
                        }
                        self.plan = TrainingPlanDTO(
                            id: plan.id,
                            targetId: plan.targetId,
                            tasks: tasks,
                            createdAt: plan.createdAt
                        )
                    }
                }
            }
            .task {
                await loadPlan()
            }
        }
    }

    private var generateView: some View {
        Form {
            if let errorMessage {
                Section {
                    ErrorBanner(message: errorMessage)
                }
            }

            Section {
                ContentUnavailableView(
                    "训练计划",
                    systemImage: "figure.run",
                    description: Text("基于测评结果的短板分析，AI 将生成针对性的 1 天训练任务。")
                )
            }

            Section {
                Button {
                    Task { await generatePlan() }
                } label: {
                    Label("生成训练计划", systemImage: "sparkles")
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func planListView(_ plan: TrainingPlanDTO) -> some View {
        Form {
            if let errorMessage {
                Section {
                    ErrorBanner(message: errorMessage) {
                        Task { await loadPlan() }
                    }
                }
            }

            Section("训练任务") {
                ForEach(plan.tasks, id: \.id) { task in
                    Button {
                        selectedTask = task
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(task.title)
                                    .font(.body)
                                    .foregroundStyle(.primary)
                                Text(task.description)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                            }
                            Spacer()
                            taskStatusIcon(task.status)
                        }
                    }
                }
            }

            Section {
                Button {
                    Task { await generatePlan() }
                } label: {
                    Label("重新生成", systemImage: "arrow.clockwise")
                        .frame(maxWidth: .infinity)
                }
                .disabled(isLoading)
            }
        }
    }

    private func taskStatusIcon(_ status: String) -> some View {
        Group {
            switch status {
            case "completed":
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            case "in_progress":
                Image(systemName: "circle.lefthalf.filled")
                    .foregroundStyle(.orange)
            default:
                Image(systemName: "circle")
                    .foregroundStyle(.secondary)
            }
        }
    }

    @MainActor
    private func loadPlan() async {
        isLoading = true
        errorMessage = nil
        do {
            plan = try await APIClient.shared.request(
                "GET",
                path: "/api/training-plans/\(target.id)"
            )
        } catch {
            // No plan yet — show generate view
        }
        isLoading = false
    }

    @MainActor
    private func generatePlan() async {
        isLoading = true
        errorMessage = nil
        do {
            plan = try await APIClient.shared.request(
                "POST",
                path: "/api/training-plans/generate",
                body: TrainingPlanGenerateRequestDTO(targetId: target.id)
            )
        } catch {
            errorMessage = "生成失败: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

import SwiftUI

enum CoachAgentRecommendedAction: Equatable {
    case assessment
    case training
    case mockInterview
    case progress

    var title: String {
        switch self {
        case .assessment: return "开始技术测评"
        case .training: return "查看训练计划"
        case .mockInterview: return "开始模拟面试"
        case .progress: return "查看进步追踪"
        }
    }

    var systemImage: String {
        switch self {
        case .assessment: return "checkmark.shield"
        case .training: return "figure.run"
        case .mockInterview: return "person.wave.2"
        case .progress: return "chart.line.uptrend.xyaxis"
        }
    }
}

struct CoachAgentView: View {
    let targetId: String
    let targetTitle: String
    let onRecommendedAction: (CoachAgentRecommendedAction) -> Void

    @State private var agent: CoachAgentDTO?
    @State private var isLoading = false
    @State private var errorMessage: String?

    init(
        targetId: String,
        targetTitle: String,
        onRecommendedAction: @escaping (CoachAgentRecommendedAction) -> Void = { _ in }
    ) {
        self.targetId = targetId
        self.targetTitle = targetTitle
        self.onRecommendedAction = onRecommendedAction
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("加载教练状态...")
                } else if let agent {
                    agentContent(agent)
                } else if let errorMessage {
                    ContentUnavailableView {
                        Label("加载失败", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(errorMessage)
                    } actions: {
                        Button("重试") {
                            Task { await loadAgent() }
                        }
                    }
                } else {
                    ContentUnavailableView(
                        "暂无教练状态",
                        systemImage: "brain.head.profile",
                        description: Text("创建岗位目标后，AI 教练将自动开始跟踪你的面试准备进度。")
                    )
                }
            }
            .navigationTitle("AI 教练")
            .navigationBarTitleDisplayMode(.inline)
            .task { await loadAgent() }
        }
    }

    private func agentContent(_ agent: CoachAgentDTO) -> some View {
        List {
            // Current Goal Section
            if let goal = agent.currentGoal, !goal.isEmpty {
                Section("当前目标") {
                    Text(goal)
                        .font(.body)
                }
            }

            // Focus Dimensions Section
            if !agent.activeFocusDimensions.isEmpty {
                Section("重点能力维度") {
                    ForEach(agent.activeFocusDimensions, id: \.self) { dimension in
                        Label(dimension, systemImage: "target")
                            .font(.body)
                    }
                }
            }

            // Recommended Action Section
            if let action = agent.nextRecommendedAction, !action.isEmpty {
                Section("推荐下一步") {
                    Text(action)
                        .font(.body)
                        .foregroundStyle(.blue)
                    if let recommendedAction = recommendedAction(for: action, stage: agent.currentStage) {
                        Button {
                            onRecommendedAction(recommendedAction)
                        } label: {
                            Label(recommendedAction.title, systemImage: recommendedAction.systemImage)
                        }
                    }
                }
            }

            // Last Decision Summary Section
            if let summary = agent.lastDecisionSummary, !summary.isEmpty {
                Section("最近教练判断") {
                    Text(summary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            // Agent Status Section
            Section("教练状态") {
                LabeledContent("阶段", value: stageDisplayName(agent.currentStage))
                LabeledContent("状态", value: statusDisplayName(agent.status))
                if let eventType = agent.lastEventType {
                    LabeledContent("最近事件", value: eventDisplayName(eventType))
                }
                if let lastRun = agent.lastRunAt {
                    LabeledContent("上次运行", value: formatDate(lastRun))
                }
            }
        }
    }

    @MainActor
    private func loadAgent() async {
        isLoading = true
        errorMessage = nil
        do {
            agent = try await APIClient.shared.request(
                "GET",
                path: "/api/targets/\(targetId)/coach-agent"
            )
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func stageDisplayName(_ stage: String) -> String {
        switch stage {
        case "targetSetup": return "目标设定"
        case "profileConfirmation": return "简历确认"
        case "assessment": return "测评阶段"
        case "training": return "训练阶段"
        case "mockInterview": return "模拟面试"
        case "review": return "复盘阶段"
        default: return stage
        }
    }

    private func statusDisplayName(_ status: String) -> String {
        switch status {
        case "active": return "活跃"
        case "paused": return "暂停"
        case "completed": return "已完成"
        default: return status
        }
    }

    private func eventDisplayName(_ event: String) -> String {
        switch event {
        case "TARGET_CREATED": return "创建目标"
        case "RESUME_SUMMARY_CONFIRMED": return "简历确认"
        case "ASSESSMENT_COMPLETED": return "测评完成"
        case "TRAINING_TASK_COMPLETED": return "训练任务完成"
        case "TRAINING_SESSION_COMPLETED": return "训练会话完成"
        case "MOCK_INTERVIEW_COMPLETED": return "模拟面试完成"
        case "MEMORY_CORRECTED": return "记忆纠错"
        case "APP_SESSION_STARTED": return "App 启动"
        default: return event
        }
    }

    private func recommendedAction(for action: String, stage: String) -> CoachAgentRecommendedAction? {
        let normalized = "\(action) \(stage)".lowercased()

        if containsAny(normalized, keywords: ["mock", "模拟", "mock_interview", "mockinterview"]) {
            return .mockInterview
        }
        if containsAny(normalized, keywords: ["assessment", "测评", "评估", "答题"]) {
            return .assessment
        }
        if containsAny(normalized, keywords: ["training", "训练", "练习", "计划"]) {
            return .training
        }
        if containsAny(normalized, keywords: ["progress", "进步", "追踪", "复盘", "分析"]) {
            return .progress
        }

        switch stage {
        case "assessment": return .assessment
        case "training": return .training
        case "mockInterview": return .mockInterview
        case "review": return .progress
        default: return nil
        }
    }

    private func containsAny(_ text: String, keywords: [String]) -> Bool {
        keywords.contains { text.contains($0) }
    }

    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MM/dd HH:mm"
            return displayFormatter.string(from: date)
        }
        return dateString
    }
}

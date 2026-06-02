import SwiftUI

struct ProgressDashboardView: View {
    let targetId: String
    let targetTitle: String

    @State private var dashboard: ProgressDashboardDTO?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("加载中...")
                } else if let dashboard {
                    dashboardContent(dashboard)
                } else if let errorMessage {
                    ContentUnavailableView {
                        Label("加载失败", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(errorMessage)
                    } actions: {
                        Button("重试") {
                            Task { await loadDashboard() }
                        }
                    }
                } else {
                    ContentUnavailableView(
                        "暂无数据",
                        systemImage: "chart.line.uptrend.xyaxis",
                        description: Text("完成测评和训练后，将展示进步追踪数据。")
                    )
                }
            }
            .navigationTitle("进步追踪")
            .navigationBarTitleDisplayMode(.inline)
            .task { await loadDashboard() }
        }
    }

    private func dashboardContent(_ dashboard: ProgressDashboardDTO) -> some View {
        List {
            // Latest Score Section
            if let score = dashboard.latestAssessmentScore {
                Section("最新测评分数") {
                    HStack {
                        Text("总分")
                            .font(.headline)
                        Spacer()
                        Text("\(score)")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundStyle(scoreColor(score))
                    }
                }
            }

            // Training Completion Section
            Section("训练完成率") {
                HStack {
                    VStack(alignment: .leading) {
                        Text("已完成 \(dashboard.trainingCompletion.completedTasks)/\(dashboard.trainingCompletion.totalTasks) 个任务")
                            .font(.body)
                        ProgressView(value: dashboard.trainingCompletion.completionRate)
                            .tint(dashboard.trainingCompletion.completionRate >= 1.0 ? .green : .blue)
                    }
                    Spacer()
                    Text("\(Int(dashboard.trainingCompletion.completionRate * 100))%")
                        .font(.title2)
                        .fontWeight(.semibold)
                }
            }

            // Dimension Summary Section
            Section("能力维度") {
                ForEach(dashboard.dimensionSummary) { dimension in
                    HStack {
                        Text(dimensionDisplayName(dimension.name))
                            .font(.body)
                        Spacer()
                        if let score = dimension.latestScore {
                            Text("\(score)")
                                .fontWeight(.semibold)
                                .foregroundStyle(scoreColor(score))
                        }
                        if let trend = dimension.trend {
                            Image(systemName: trendIcon(trend))
                                .foregroundStyle(trendColor(trend))
                                .font(.caption)
                        }
                    }
                }
            }

            // Recent Weaknesses Section
            if !dashboard.recentWeaknesses.isEmpty {
                Section("最近短板") {
                    ForEach(dashboard.recentWeaknesses, id: \.self) { weakness in
                        Label(weakness, systemImage: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }
                }
            }

            // Score Trend Section
            if !dashboard.scoreTrend.isEmpty {
                Section("分数趋势") {
                    ForEach(dashboard.scoreTrend, id: \.createdAt) { entry in
                        HStack {
                            Text(entry.source == "assessment" ? "测评" : "模拟面试")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Spacer()
                            Text("\(entry.score)")
                                .fontWeight(.medium)
                            Text(formatDate(entry.createdAt))
                                .font(.caption2)
                                .foregroundStyle(.tertiary)
                        }
                    }
                }
            }
        }
    }

    @MainActor
    private func loadDashboard() async {
        isLoading = true
        errorMessage = nil
        do {
            dashboard = try await APIClient.shared.request(
                "GET",
                path: "/api/progress?targetId=\(targetId)"
            )
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func dimensionDisplayName(_ name: String) -> String {
        switch name {
        case "technicalDepth": return "技术深度"
        case "projectSpecificity": return "项目 specificity"
        case "systemThinking": return "系统思维"
        case "tradeoffAwareness": return "权衡意识"
        case "failureHandling": return "故障处理"
        case "communicationClarity": return "表达清晰度"
        case "businessContext": return "业务理解"
        default: return name
        }
    }

    private func trendIcon(_ trend: String) -> String {
        switch trend {
        case "improving": return "arrow.up.circle.fill"
        case "declining": return "arrow.down.circle.fill"
        case "stable": return "minus.circle.fill"
        default: return "questionmark.circle"
        }
    }

    private func trendColor(_ trend: String) -> Color {
        switch trend {
        case "improving": return .green
        case "declining": return .red
        case "stable": return .orange
        default: return .gray
        }
    }

    private func scoreColor(_ score: Int) -> Color {
        if score >= 80 { return .green }
        if score >= 60 { return .orange }
        return .red
    }

    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MM/dd"
            return displayFormatter.string(from: date)
        }
        return dateString
    }
}

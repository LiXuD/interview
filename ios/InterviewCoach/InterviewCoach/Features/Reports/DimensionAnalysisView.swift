import SwiftUI

struct DimensionAnalysisView: View {
    let targetId: String

    @State private var analysis: DimensionAnalysisDTO?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("加载中...")
                } else if let analysis {
                    analysisListView(analysis)
                } else if let errorMessage {
                    ContentUnavailableView {
                        Label("加载失败", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(errorMessage)
                    } actions: {
                        Button("重试") {
                            Task { await loadAnalysis() }
                        }
                    }
                } else {
                    ContentUnavailableView(
                        "暂无数据",
                        systemImage: "chart.bar",
                        description: Text("完成测评和模拟面试后，将展示能力维度分析。")
                    )
                }
            }
            .navigationTitle("能力维度分析")
            .navigationBarTitleDisplayMode(.inline)
            .task { await loadAnalysis() }
        }
    }

    private func analysisListView(_ analysis: DimensionAnalysisDTO) -> some View {
        List {
            ForEach(analysis.dimensions) { dimension in
                Section(dimensionDisplayName(dimension.name)) {
                    if let score = dimension.latestScore {
                        HStack {
                            Text("最新分数")
                            Spacer()
                            Text("\(score)")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundStyle(scoreColor(score))
                        }
                    }

                    if let trend = dimension.trend {
                        HStack {
                            Text("趋势")
                            Spacer()
                            Label(trendDisplayName(trend), systemImage: trendIcon(trend))
                                .foregroundStyle(trendColor(trend))
                        }
                    }

                    if !dimension.weaknesses.isEmpty {
                        Section("短板") {
                            ForEach(dimension.weaknesses, id: \.self) { weakness in
                                Text(weakness)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    if !dimension.nextFocus.isEmpty {
                        Section("下一步") {
                            ForEach(dimension.nextFocus, id: \.self) { focus in
                                Label(focus, systemImage: "arrow.right.circle")
                                    .font(.caption)
                                    .foregroundStyle(.blue)
                            }
                        }
                    }
                }
            }
        }
    }

    @MainActor
    private func loadAnalysis() async {
        isLoading = true
        errorMessage = nil
        do {
            analysis = try await APIClient.shared.request(
                "GET",
                path: "/api/dimension-analysis?targetId=\(targetId)"
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

    private func trendDisplayName(_ trend: String) -> String {
        switch trend {
        case "improving": return "上升"
        case "declining": return "下降"
        case "stable": return "稳定"
        default: return "数据不足"
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
}

import SwiftUI

struct AssessmentReportContent: Decodable {
    let totalScore: Int
    let dimensions: [DimensionScoreDTO]
    let strengths: [String]
    let weaknesses: [String]
    let nextActions: [String]
    let questionScores: [AssessmentQuestionScoreDTO]?
}

struct MockInterviewReportContent: Decodable {
    let overallScore: Int
    let dimensionScores: [DimensionScoreDTO]
    let summary: String
    let strengths: [String]
    let weaknesses: [String]
    let improvedAnswers: [String]
    let likelyFollowUpPoints: [String]
    let nextTrainingTasks: [String]
}

enum ParsedReportContent {
    case assessment(AssessmentReportContent)
    case mockInterview(MockInterviewReportContent)
    case raw(String)

    private static let decoder = JSONDecoder()

    static func parse(from report: ReportDTO) -> ParsedReportContent {
        guard let data = report.content.data(using: .utf8) else {
            return .raw(report.content)
        }
        switch report.type {
        case "assessment":
            if let content = try? Self.decoder.decode(AssessmentReportContent.self, from: data) {
                return .assessment(content)
            }
        case "mockInterview":
            if let content = try? Self.decoder.decode(MockInterviewReportContent.self, from: data) {
                return .mockInterview(content)
            }
        default:
            break
        }
        return .raw(report.content)
    }
}

struct ReportListView: View {
    let targetId: String

    @Environment(\.dismiss) private var dismiss
    @State private var reports: [ReportDTO] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("加载中...")
                } else if reports.isEmpty && errorMessage == nil {
                    ContentUnavailableView(
                        "暂无报告",
                        systemImage: "doc.text",
                        description: Text("完成测评或模拟面试后，报告将出现在这里。")
                    )
                } else {
                    reportList
                }
            }
            .navigationTitle("历史报告")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
            .task {
                await loadReports()
            }
        }
    }

    private var reportList: some View {
        List {
            if let errorMessage {
                Section {
                    ErrorBanner(message: errorMessage) {
                        Task { await loadReports() }
                    }
                }
            }

            let grouped = Dictionary(grouping: reports) { $0.type }
            let sortedTypes = grouped.keys.sorted { typeLabel($0) < typeLabel($1) }

            ForEach(sortedTypes, id: \.self) { type in
                Section(typeLabel(type)) {
                    ForEach(grouped[type] ?? [], id: \.id) { report in
                        NavigationLink {
                            ReportDetailView(report: report)
                        } label: {
                            reportRow(report)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Report row

    @ViewBuilder
    private func reportRow(_ report: ReportDTO) -> some View {
        let parsed = ParsedReportContent.parse(from: report)
        HStack(spacing: 12) {
            scoreBadge(parsed)
            VStack(alignment: .leading, spacing: 4) {
                Text(summaryText(parsed))
                    .font(.subheadline)
                    .lineLimit(2)
                Text(DateHelper.formatISODate(report.createdAt))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
    }

    @ViewBuilder
    private func scoreBadge(_ parsed: ParsedReportContent) -> some View {
        let score: Int? = switch parsed {
        case .assessment(let c): c.totalScore
        case .mockInterview(let c): c.overallScore
        case .raw: nil
        }
        if let score {
            ZStack {
                Circle()
                    .fill(scoreColor(score).opacity(0.15))
                    .frame(width: 40, height: 40)
                Text("\(score)")
                    .font(.caption.bold())
                    .foregroundStyle(scoreColor(score))
            }
        }
    }

    private func summaryText(_ parsed: ParsedReportContent) -> String {
        switch parsed {
        case .assessment(let c):
            let top = c.strengths.first ?? "共 \(c.dimensions.count) 个维度"
            return "总分 \(c.totalScore) · \(top)"
        case .mockInterview(let c):
            let snippet = c.summary.prefix(60)
            return "总分 \(c.overallScore) · \(snippet)"
        case .raw(let content):
            return String(content.trimmingCharacters(in: .whitespacesAndNewlines).prefix(80))
        }
    }

    // MARK: - Helpers

    private func loadReports() async {
        isLoading = true
        errorMessage = nil
        do {
            reports = try await APIClient.shared.request(
                "GET",
                path: "/api/reports?targetId=\(targetId)"
            )
        } catch {
            errorMessage = "加载失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func typeLabel(_ type: String) -> String {
        switch type {
        case "assessment": return "测评报告"
        case "mockInterview": return "面试报告"
        default: return type
        }
    }

    private func scoreColor(_ score: Int) -> Color {
        if score >= 80 { return .green }
        if score >= 60 { return .orange }
        return .red
    }
}

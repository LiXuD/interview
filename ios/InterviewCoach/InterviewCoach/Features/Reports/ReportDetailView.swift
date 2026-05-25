import SwiftUI

struct ReportDetailView: View {
    let report: ReportDTO

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header
                parsedContent
            }
            .padding()
        }
        .navigationTitle("报告详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Header

    private var header: some View {
        HStack {
            Label(typeLabel, systemImage: typeIcon)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(typeColor.opacity(0.15))
                .foregroundStyle(typeColor)
                .clipShape(Capsule())

            Spacer()

            Text(DateHelper.formatISODate(report.createdAt))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Parsed content

    @ViewBuilder
    private var parsedContent: some View {
        let parsed = ParsedReportContent.parse(from: report)
        switch parsed {
        case .assessment(let c):
            assessmentContent(c)
        case .mockInterview(let c):
            mockInterviewContent(c)
        case .raw(let text):
            Text(text)
                .font(.body)
                .textSelection(.enabled)
        }
    }

    // MARK: - Assessment report

    @ViewBuilder
    private func assessmentContent(_ c: AssessmentReportContent) -> some View {
        scoreSection(score: c.totalScore, label: "测评总分")

        if !c.dimensions.isEmpty {
            dimensionSection(c.dimensions)
        }

        if !c.strengths.isEmpty {
            bulletSection(title: "优势", items: c.strengths, color: .green)
        }
        if !c.weaknesses.isEmpty {
            bulletSection(title: "不足", items: c.weaknesses, color: .orange)
        }
        if !c.nextActions.isEmpty {
            bulletSection(title: "下一步行动", items: c.nextActions, color: .blue)
        }
    }

    // MARK: - Mock interview report

    @ViewBuilder
    private func mockInterviewContent(_ c: MockInterviewReportContent) -> some View {
        scoreSection(score: c.overallScore, label: "面试总分")

        if !c.summary.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text("总结")
                    .font(.subheadline.bold())
                Text(c.summary)
                    .font(.body)
                    .textSelection(.enabled)
            }
        }

        if !c.dimensionScores.isEmpty {
            dimensionSection(c.dimensionScores)
        }

        if !c.strengths.isEmpty {
            bulletSection(title: "优势", items: c.strengths, color: .green)
        }
        if !c.weaknesses.isEmpty {
            bulletSection(title: "不足", items: c.weaknesses, color: .orange)
        }
        if !c.improvedAnswers.isEmpty {
            bulletSection(title: "改进建议", items: c.improvedAnswers, color: .purple)
        }
        if !c.nextTrainingTasks.isEmpty {
            bulletSection(title: "后续训练", items: c.nextTrainingTasks, color: .blue)
        }
    }

    // MARK: - Shared components

    private func scoreSection(score: Int, label: String) -> some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(scoreColor(score).opacity(0.15))
                    .frame(width: 56, height: 56)
                Text("\(score)")
                    .font(.title2.bold())
                    .foregroundStyle(scoreColor(score))
            }
            Text(label)
                .font(.headline)
        }
    }

    private func dimensionSection(_ dimensions: [DimensionScoreDTO]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("维度评分")
                .font(.subheadline.bold())
            ForEach(dimensions, id: \.name) { dim in
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(dim.name)
                            .font(.subheadline)
                        Spacer()
                        Text("\(dim.score)")
                            .font(.subheadline.bold())
                            .foregroundStyle(scoreColor(dim.score))
                    }
                    ProgressView(value: Double(dim.score) / 100.0)
                        .tint(scoreColor(dim.score))
                    if !dim.reason.isEmpty {
                        Text(dim.reason)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(10)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
    }

    private func bulletSection(title: String, items: [String], color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.subheadline.bold())
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                HStack(alignment: .top, spacing: 8) {
                    Circle()
                        .fill(color)
                        .frame(width: 6, height: 6)
                        .padding(.top, 6)
                    Text(item)
                        .font(.body)
                        .textSelection(.enabled)
                }
            }
        }
    }

    // MARK: - Type helpers

    private var typeLabel: String {
        switch report.type {
        case "assessment": return "测评报告"
        case "mockInterview": return "面试报告"
        default: return report.type
        }
    }

    private var typeIcon: String {
        switch report.type {
        case "assessment": return "checkmark.shield"
        case "mockInterview": return "person.wave.2"
        default: return "doc.text"
        }
    }

    private var typeColor: Color {
        switch report.type {
        case "assessment": return .blue
        case "mockInterview": return .purple
        default: return .gray
        }
    }

    private func scoreColor(_ score: Int) -> Color {
        if score >= 80 { return .green }
        if score >= 60 { return .orange }
        return .red
    }
}

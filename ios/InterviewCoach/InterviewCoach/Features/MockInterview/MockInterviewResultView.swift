import SwiftUI

struct MockInterviewResultView: View {
    let report: MockInterviewReportDTO

    var body: some View {
        Form {
            Section("总评") {
                HStack {
                    Text("得分")
                        .font(.headline)
                    Spacer()
                    Text("\(report.overallScore)")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundStyle(scoreColor(report.overallScore))
                }
                Text(report.summary)
                    .font(.body)
            }

            Section("维度评分") {
                ForEach(report.dimensionScores, id: \.name) { dim in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(dim.name)
                                .font(.body)
                            Spacer()
                            Text("\(dim.score)")
                                .font(.headline)
                                .foregroundStyle(scoreColor(dim.score))
                        }
                        Text(dim.reason)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if !report.strengths.isEmpty {
                Section("优势") {
                    ForEach(report.strengths, id: \.self) { item in
                        Label(item, systemImage: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                    }
                }
            }

            if !report.weaknesses.isEmpty {
                Section("待改进") {
                    ForEach(report.weaknesses, id: \.self) { item in
                        Label(item, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                    }
                }
            }

            if !report.improvedAnswers.isEmpty {
                Section("优化回答示例") {
                    ForEach(Array(report.improvedAnswers.enumerated()), id: \.offset) { _, item in
                        Text(item)
                            .font(.body)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if !report.nextTrainingTasks.isEmpty {
                Section("建议训练") {
                    ForEach(report.nextTrainingTasks, id: \.self) { item in
                        Label(item, systemImage: "book.fill")
                            .foregroundStyle(.blue)
                    }
                }
            }
        }
    }

    private func scoreColor(_ score: Int) -> Color {
        if score >= 80 { return .green }
        if score >= 60 { return .orange }
        return .red
    }
}

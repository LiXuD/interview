import SwiftUI

struct AssessmentResultView: View {
    let result: AssessmentResultDTO
    let targetTitle: String

    var body: some View {
        Form {
            Section("总评") {
                HStack {
                    Text("综合评分")
                        .font(.headline)
                    Spacer()
                    Text("\(result.totalScore)")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundStyle(scoreColor(result.totalScore))
                }
            }

            Section("能力维度") {
                ForEach(result.dimensions, id: \.name) { dim in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(dim.name)
                                .font(.headline)
                            Spacer()
                            Text("\(dim.score)")
                                .font(.title3)
                                .fontWeight(.semibold)
                                .foregroundStyle(scoreColor(dim.score))
                        }
                        ProgressView(value: Double(dim.score), total: 100)
                            .tint(scoreColor(dim.score))
                        Text(dim.reason)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }

            if !result.strengths.isEmpty {
                Section("优势") {
                    ForEach(result.strengths, id: \.self) { item in
                        Label(item, systemImage: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                    }
                }
            }

            if !result.weaknesses.isEmpty {
                Section("待改进") {
                    ForEach(result.weaknesses, id: \.self) { item in
                        Label(item, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                    }
                }
            }

            if !result.nextActions.isEmpty {
                Section("下一步行动") {
                    ForEach(result.nextActions, id: \.self) { item in
                        Label(item, systemImage: "arrow.right.circle")
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

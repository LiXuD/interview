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

            if let questionScores = result.questionScores, !questionScores.isEmpty {
                Section("逐题诊断") {
                    ForEach(questionScores, id: \.questionIndex) { qs in
                        DisclosureGroup {
                            perQuestionDetail(qs)
                        } label: {
                            questionScoreHeader(qs)
                        }
                    }
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

    // MARK: - Per-question diagnosis

    private func questionScoreHeader(_ qs: AssessmentQuestionScoreDTO) -> some View {
        HStack {
            Text("第 \(qs.questionIndex + 1) 题")
                .font(.subheadline.bold())
            Spacer()
            Text("\(qs.score) 分")
                .font(.subheadline.bold())
                .foregroundStyle(scoreColor(qs.score))
        }
    }

    @ViewBuilder
    private func perQuestionDetail(_ qs: AssessmentQuestionScoreDTO) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(qs.feedback)
                .font(.body)

            if !qs.contentHighlights.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("亮点")
                        .font(.caption.bold())
                        .foregroundStyle(.green)
                    ForEach(qs.contentHighlights, id: \.self) { item in
                        HStack(alignment: .top, spacing: 6) {
                            Circle().fill(.green).frame(width: 5, height: 5).padding(.top, 5)
                            Text(item).font(.caption)
                        }
                    }
                }
            }

            if !qs.problems.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("不足")
                        .font(.caption.bold())
                        .foregroundStyle(.orange)
                    ForEach(qs.problems, id: \.self) { item in
                        HStack(alignment: .top, spacing: 6) {
                            Circle().fill(.orange).frame(width: 5, height: 5).padding(.top, 5)
                            Text(item).font(.caption)
                        }
                    }
                }
            }

            if !qs.contentGaps.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("内容缺失")
                        .font(.caption.bold())
                        .foregroundStyle(.red)
                    ForEach(qs.contentGaps, id: \.self) { item in
                        HStack(alignment: .top, spacing: 6) {
                            Circle().fill(.red).frame(width: 5, height: 5).padding(.top, 5)
                            Text(item).font(.caption)
                        }
                    }
                }
            }

            if !qs.followUpRisks.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("追问风险")
                        .font(.caption.bold())
                        .foregroundStyle(.orange)
                    ForEach(qs.followUpRisks, id: \.self) { item in
                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(.caption2)
                                .foregroundStyle(.orange)
                            Text(item).font(.caption)
                        }
                    }
                }
            }

            if let structure = qs.answerStructure {
                VStack(alignment: .leading, spacing: 4) {
                    Text("回答结构诊断")
                        .font(.caption.bold())
                    structureRow("背景", value: structure.background)
                    structureRow("任务", value: structure.task)
                    structureRow("行动", value: structure.action)
                    structureRow("结果", value: structure.result)
                    structureRow("权衡", value: structure.tradeoff)
                    structureRow("复盘", value: structure.review)
                }
            }

            if !qs.improvedExample.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("改进示范")
                        .font(.caption.bold())
                        .foregroundStyle(.blue)
                    Text(qs.improvedExample)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private func structureRow(_ label: String, value: String) -> some View {
        HStack(alignment: .top, spacing: 4) {
            Text(label)
                .font(.caption2.bold())
                .frame(width: 28, alignment: .leading)
            Text(value)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private func scoreColor(_ score: Int) -> Color {
        if score >= 80 { return .green }
        if score >= 60 { return .orange }
        return .red
    }
}

import SwiftUI

struct JobBriefView: View {
    let target: InterviewTargetDTO

    @Environment(\.dismiss) private var dismiss
    @State private var brief: JobBriefDTO?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button {
                        Task { await generateBrief() }
                    } label: {
                        Label(brief == nil ? "生成岗位画像" : "重新生成岗位画像", systemImage: "sparkles")
                    }
                    .disabled(isLoading)
                } footer: {
                    Text("基于当前岗位 JD 和已确认的候选人摘要生成。")
                }

                if let brief {
                    Section("岗位概览") {
                        Text(brief.roleSummary)
                        LabeledContent("置信度", value: percentText(brief.confidence))
                    }

                    SkillMapSection(skills: brief.skillMap)
                    BulletListSection(title: "必备技能", items: brief.mustHaveSkills)
                    BulletListSection(title: "加分技能", items: brief.niceToHaveSkills)
                    BulletListSection(title: "业务背景", items: brief.businessContext)
                    BulletListSection(title: "面试主题", items: brief.interviewTopics)
                    BulletListSection(title: "候选人匹配", items: brief.candidateMatch)
                    BulletListSection(title: "风险点", items: brief.riskAreas)
                } else if !isLoading {
                    Section {
                        ContentUnavailableView(
                            "暂无岗位画像",
                            systemImage: "doc.text.magnifyingglass",
                            description: Text("请先确认候选人简历摘要，然后生成岗位画像。")
                        )
                    }
                }
            }
            .navigationTitle("岗位画像")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
            .task {
                await loadExistingBrief()
            }
            .overlay {
                if isLoading {
                    ProgressView("处理中...")
                        .padding(20)
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
                }
            }
        }
    }

    private func loadExistingBrief() async {
        guard brief == nil else { return }
        isLoading = true
        errorMessage = nil
        do {
            let result: JobBriefDTO = try await APIClient.shared.request(
                "GET",
                path: "/api/job-briefs/\(target.id)"
            )
            brief = result
        } catch APIError.serverError(404, _) {
            brief = nil
        } catch {
            errorMessage = "读取失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func generateBrief() async {
        isLoading = true
        errorMessage = nil
        do {
            let result: JobBriefDTO = try await APIClient.shared.request(
                "POST",
                path: "/api/job-briefs/generate",
                body: JobBriefGenerateRequestDTO(targetId: target.id)
            )
            brief = result
        } catch {
            errorMessage = "生成失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func percentText(_ value: Double) -> String {
        "\(Int((value * 100).rounded()))%"
    }
}

private struct SkillMapSection: View {
    let skills: [SkillMapItemDTO]

    var body: some View {
        Section("技能地图") {
            ForEach(skills, id: \.name) { skill in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(skill.name)
                            .font(.headline)
                        Spacer()
                        Text(importanceText(skill.importance))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Text("当前水平: \(userLevelText(skill.userLevel))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(skill.gap)
                        .font(.subheadline)
                }
                .padding(.vertical, 4)
            }
        }
    }

    private func importanceText(_ value: String) -> String {
        switch value {
        case "required": return "必备"
        case "important": return "重要"
        case "bonus": return "加分"
        default: return value
        }
    }

    private func userLevelText(_ value: String) -> String {
        switch value {
        case "unknown": return "待确认"
        case "weak": return "薄弱"
        case "basic": return "基础"
        case "solid": return "扎实"
        case "strong": return "强"
        default: return value
        }
    }
}

private struct BulletListSection: View {
    let title: String
    let items: [String]

    var body: some View {
        Section(title) {
            if items.isEmpty {
                Text("暂无")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(items, id: \.self) { item in
                    Text(item)
                }
            }
        }
    }
}

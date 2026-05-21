import SwiftUI

struct ProfileInputView: View {
    let targetId: String
    let targetTitle: String
    let authService: AuthService

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var resumeText = ""
    @State private var projectText = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var draft: CandidateProfileDraftDTO?
    @State private var showConsent = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("简历原文仅临时发送到后端生成摘要，不会落库存储。生成后可编辑确认摘要内容。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } header: {
                    Text("隐私说明")
                }

                Section {
                    TextField("粘贴简历或项目经历", text: $resumeText, axis: .vertical)
                        .lineLimit(8...20)
                } header: {
                    Text("简历 / 项目经历")
                } footer: {
                    Text("原文仅在生成摘要时临时上传，后端不会保存原文。")
                }

                Section {
                    TextField("可选：补充项目经历", text: $projectText, axis: .vertical)
                        .lineLimit(5...15)
                } header: {
                    Text("补充信息（可选）")
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                            .font(.caption)
                    }
                }

                Section {
                    Button("生成摘要") {
                        showConsent = true
                    }
                    .disabled(resumeText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                }
            }
            .navigationTitle("候选人简历")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                }
            }
            .alert("确认上传", isPresented: $showConsent) {
                Button("取消", role: .cancel) {}
                Button("同意并生成") {
                    Task { await generateDraft() }
                }
            } message: {
                Text("简历原文将临时发送到后端进行 AI 摘要生成。后端不会保存原文，仅在内存中使用。")
            }
            .navigationDestination(item: $draft) { draftValue in
                ProfileConfirmView(
                    targetId: targetId,
                    targetTitle: targetTitle,
                    draft: draftValue,
                    authService: authService
                )
            }
            .overlay {
                if isLoading {
                    ProgressView("生成摘要中...")
                        .padding(20)
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
                }
            }
        }
    }

    private func generateDraft() async {
        isLoading = true
        errorMessage = nil
        do {
            let result: CandidateProfileDraftDTO = try await APIClient.shared.request(
                "POST",
                path: "/api/profiles/draft-summary",
                body: CandidateProfileDraftRequestDTO(
                    resumeText: resumeText.isEmpty ? nil : resumeText,
                    projectRawText: projectText.isEmpty ? nil : projectText
                )
            )
            draft = result
        } catch {
            errorMessage = "生成失败: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

extension CandidateProfileDraftDTO: @retroactive Hashable {
    public func hash(into hasher: inout Hasher) {
        hasher.combine(summary)
        hasher.combine(rawTextLength)
    }
}

extension CandidateProfileDraftDTO: @retroactive Identifiable {
    public var id: String { "\(summary)_\(rawTextLength)" }
}

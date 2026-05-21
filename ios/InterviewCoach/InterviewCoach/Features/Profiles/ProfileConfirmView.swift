import SwiftUI
import SwiftData

struct ProfileConfirmView: View {
    let targetId: String
    let targetTitle: String
    let draft: CandidateProfileDraftDTO
    let authService: AuthService

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var summary: String
    @State private var skillText: String
    @State private var projectText: String
    @State private var experienceText: String
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var isConfirmed = false

    init(targetId: String, targetTitle: String, draft: CandidateProfileDraftDTO, authService: AuthService) {
        self.targetId = targetId
        self.targetTitle = targetTitle
        self.draft = draft
        self.authService = authService
        _summary = State(initialValue: draft.summary)
        _skillText = State(initialValue: draft.skills.joined(separator: "\n"))
        _projectText = State(initialValue: draft.projects.joined(separator: "\n"))
        _experienceText = State(initialValue: draft.experience.joined(separator: "\n"))
    }

    var body: some View {
        Form {
            Section {
                Text("请确认或编辑以下摘要内容。确认后将保存到服务器。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } header: {
                Text("摘要确认")
            }

            Section {
                TextEditor(text: $summary)
                    .frame(minHeight: 100)
            } header: {
                Text("个人摘要")
            }

            Section {
                TextEditor(text: $skillText)
                    .frame(minHeight: 80)
            } header: {
                Text("技能（每行一项）")
            }

            Section {
                TextEditor(text: $projectText)
                    .frame(minHeight: 80)
            } header: {
                Text("项目经历（每行一项）")
            }

            Section {
                TextEditor(text: $experienceText)
                    .frame(minHeight: 80)
            } header: {
                Text("工作经历（每行一项）")
            }

            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                        .font(.caption)
                }
            }

            Section {
                Button("确认保存") {
                    Task { await confirm() }
                }
                .disabled(summary.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
            }
        }
        .navigationTitle("确认简历摘要")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(isLoading)
        .overlay {
            if isLoading {
                ProgressView("保存中...")
                    .padding(20)
                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
            }
        }
        .alert("保存成功", isPresented: $isConfirmed) {
            Button("完成") { dismiss() }
        } message: {
            Text("候选人简历摘要已保存。")
        }
    }

    private func confirm() async {
        isLoading = true
        errorMessage = nil
        do {
            let result: CandidateProfileDTO = try await APIClient.shared.request(
                "POST",
                path: "/api/profiles/confirm",
                body: CandidateProfileConfirmRequestDTO(
                    targetId: targetId,
                    summary: summary.trimmingCharacters(in: .whitespacesAndNewlines),
                    skills: parseLines(skillText),
                    projects: parseLines(projectText),
                    experience: parseLines(experienceText)
                )
            )
            if let userId = authService.currentUser?.id {
                CandidateProfileLocal.sync(result, userId: userId, in: modelContext)
            }
            isConfirmed = true
        } catch {
            errorMessage = "保存失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func parseLines(_ text: String) -> [String] {
        text.components(separatedBy: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }
}

import SwiftUI
import SwiftData

struct SettingsView: View {
    @ObservedObject var authService: AuthService
    @Environment(\.modelContext) private var modelContext
    @State private var showDeleteConfirmation = false
    @State private var isDeleting = false
    @State private var deleteLocalCoachingMemoryArchive = false

    var body: some View {
        List {
            NavigationLink {
                AiProviderListView()
            } label: {
                Label("AI Provider", systemImage: "cpu")
            }

            NavigationLink {
                PrivacyPolicyView()
            } label: {
                Label("隐私政策", systemImage: "hand.raised")
            }

            Section {
                Text("配置自定义 OpenAI-compatible Provider 后，所有 AI 调用（岗位画像、测评、训练、模拟面试）将通过你的 Provider 执行。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if authService.hasUnimportedMemories {
                Section("教练记忆") {
                    NavigationLink {
                        CoachingMemoryImportView(currentUserId: authService.currentUser?.id ?? "")
                    } label: {
                        Label("审查待导入的教练记忆", systemImage: "square.and.arrow.down")
                    }
                } footer: {
                    Text("检测到本机有未导入的历史教练记忆。你可以选择导入到当前账号，或拒绝导入。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Section {
                Toggle("同时删除本机教练记忆文件", isOn: $deleteLocalCoachingMemoryArchive)

                Button(role: .destructive) {
                    showDeleteConfirmation = true
                } label: {
                    Label("删除账号", systemImage: "trash")
                }
                .disabled(authService.isLoading)
            } footer: {
                Text("删除账号将清除服务端所有数据，包括目标岗位、画像、测评记录、训练计划、模拟面试、报告和远端教练记忆。本机教练记忆文件默认保留，只有打开上方选项才会同时删除。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if let error = authService.errorMessage {
                Section {
                    ErrorBanner(message: error)
                }
            }
        }
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
        .loadingOverlay(isLoading: isDeleting)
        .confirmationDialog("确认删除账号", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            Button("删除账号", role: .destructive) {
                isDeleting = true
                Task {
                    await authService.deleteAccount(deleteLocalMemories: deleteLocalCoachingMemoryArchive)
                    isDeleting = false
                }
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("删除后你的所有数据将被永久清除，包括目标岗位、画像、测评、训练、模拟面试、报告和远端教练记忆。此操作不可撤销。")
        }
    }
}

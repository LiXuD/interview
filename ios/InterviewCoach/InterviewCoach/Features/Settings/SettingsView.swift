import SwiftUI

struct SettingsView: View {
    @ObservedObject var authService: AuthService
    @State private var showDeleteConfirmation = false
    @State private var isDeleting = false

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

            Section {
                Button(role: .destructive) {
                    showDeleteConfirmation = true
                } label: {
                    Label("删除账号", systemImage: "trash")
                }
                .disabled(authService.isLoading)
            } footer: {
                Text("删除账号将清除服务端所有数据，包括目标岗位、画像、测评记录、训练计划、模拟面试和报告。此操作不可撤销。")
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
                    await authService.deleteAccount()
                    isDeleting = false
                }
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("删除后你的所有数据将被永久清除，包括目标岗位、画像、测评、训练、模拟面试和报告。此操作不可撤销。")
        }
    }
}

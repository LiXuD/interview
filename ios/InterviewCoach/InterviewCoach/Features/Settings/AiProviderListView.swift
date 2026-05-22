import SwiftUI

struct AiProviderListView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var providers: [AiProviderDTO] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showCreate = false

    var body: some View {
        List {
            if let errorMessage {
                Section {
                    ErrorBanner(message: errorMessage) {
                        Task { await loadProviders() }
                    }
                }
            }

            if providers.isEmpty && !isLoading {
                Section {
                    ContentUnavailableView(
                        "AI Provider",
                        systemImage: "cpu",
                        description: Text("添加你自己的 OpenAI-compatible Provider，模型调用将通过你的 Provider 执行。")
                    )
                }
            }

            Section {
                ForEach(providers, id: \.id) { provider in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(provider.name)
                                .font(.headline)
                            Text("\(provider.model) · \(provider.openaiApiMode)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(provider.baseUrl)
                                .font(.caption2)
                                .foregroundStyle(.tertiary)
                        }
                        Spacer()
                        if provider.isDefault {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                        }
                    }
                    .swipeActions(edge: .trailing) {
                        Button("删除", role: .destructive) {
                            Task { await deleteProvider(id: provider.id) }
                        }
                        if !provider.isDefault {
                            Button("设为默认") {
                                Task { await setDefault(id: provider.id) }
                            }
                            .tint(.blue)
                        }
                    }
                }
            }
        }
        .navigationTitle("AI Provider")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreate = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showCreate) {
            AiProviderCreateView(onCreated: {
                Task { await loadProviders() }
            })
        }
        .loadingOverlay(isLoading: isLoading, message: "加载中...")
        .task {
            await loadProviders()
        }
    }

    private func loadProviders() async {
        isLoading = true
        errorMessage = nil
        do {
            providers = try await APIClient.shared.request("GET", path: "/api/ai-providers")
        } catch {
            errorMessage = "加载失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func deleteProvider(id: String) async {
        errorMessage = nil
        do {
            try await APIClient.shared.requestNoContent("DELETE", path: "/api/ai-providers/\(id)")
            providers.removeAll { $0.id == id }
        } catch {
            errorMessage = "删除失败: \(error.localizedDescription)"
        }
    }

    private func setDefault(id: String) async {
        errorMessage = nil
        do {
            let updated: AiProviderDTO = try await APIClient.shared.request(
                "PATCH", path: "/api/ai-providers/\(id)/default")
            providers = providers.map { p in
                AiProviderDTO(
                    id: p.id, name: p.name, baseUrl: p.baseUrl,
                    model: p.model, openaiApiMode: p.openaiApiMode,
                    isDefault: p.id == updated.id, createdAt: p.createdAt)
            }
        } catch {
            errorMessage = "设置失败: \(error.localizedDescription)"
        }
    }
}

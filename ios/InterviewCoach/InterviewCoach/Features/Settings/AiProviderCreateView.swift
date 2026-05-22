import SwiftUI

struct AiProviderCreateView: View {
    let onCreated: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var baseUrl = "https://api.openai.com/v1/"
    @State private var apiKey = ""
    @State private var model = "gpt-4o"
    @State private var openaiApiMode = "chatCompletions"
    @State private var isTesting = false
    @State private var isCreating = false
    @State private var testResult: String?
    @State private var testSuccess = false
    @State private var errorMessage: String?

    private let apiModes = ["chatCompletions", "responses"]

    var body: some View {
        NavigationStack {
            Form {
                if let errorMessage {
                    Section {
                        ErrorBanner(message: errorMessage)
                    }
                }

                Section("基本信息") {
                    TextField("名称（如 OpenAI GPT-4o）", text: $name)
                    TextField("Base URL", text: $baseUrl)
                        .textContentType(.URL)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                }

                Section("模型配置") {
                    SecureField("API Key", text: $apiKey)
                        .textContentType(.password)
                        .autocorrectionDisabled()
                    TextField("模型名称（如 gpt-4o）", text: $model)
                        .autocorrectionDisabled()
                    Picker("API 模式", selection: $openaiApiMode) {
                        ForEach(apiModes, id: \.self) { mode in
                            Text(mode).tag(mode)
                        }
                    }
                }

                Section {
                    Button {
                        Task { await testConnection() }
                    } label: {
                        Label(isTesting ? "测试中..." : "测试连接",
                              systemImage: isTesting ? "arrow.triangle.2.circlepath" : "wifi")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(isTesting || baseUrl.isEmpty || apiKey.isEmpty || model.isEmpty)

                    if let testResult {
                        HStack {
                            Image(systemName: testSuccess ? "checkmark.circle.fill" : "xmark.circle.fill")
                                .foregroundStyle(testSuccess ? .green : .red)
                            Text(testResult)
                                .font(.caption)
                                .foregroundStyle(testSuccess ? .green : .red)
                        }
                    }
                }

                Section {
                    Button {
                        Task { await createProvider() }
                    } label: {
                        Label("创建 Provider", systemImage: "plus.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(name.isEmpty || baseUrl.isEmpty || apiKey.isEmpty || model.isEmpty || isCreating)
                }
            }
            .navigationTitle("添加 Provider")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                }
            }
            .loadingOverlay(isLoading: isTesting || isCreating)
        }
    }

    private func testConnection() async {
        isTesting = true
        testResult = nil
        errorMessage = nil
        do {
            let request = AiProviderTestRequestDTO(
                baseUrl: baseUrl, apiKey: apiKey, model: model, openaiApiMode: openaiApiMode)
            let response: AiProviderTestResponseDTO = try await APIClient.shared.request(
                "POST", path: "/api/ai-providers/test", body: request)
            testSuccess = response.success
            testResult = response.message ?? (response.success ? "连接成功" : "连接失败")
        } catch {
            testSuccess = false
            testResult = "测试失败: \(error.localizedDescription)"
        }
        isTesting = false
    }

    private func createProvider() async {
        isCreating = true
        errorMessage = nil
        do {
            let request = AiProviderCreateRequestDTO(
                name: name, baseUrl: baseUrl, apiKey: apiKey,
                model: model, openaiApiMode: openaiApiMode)
            let _: AiProviderDTO = try await APIClient.shared.request(
                "POST", path: "/api/ai-providers", body: request)
            onCreated()
            dismiss()
        } catch {
            errorMessage = "创建失败: \(error.localizedDescription)"
            isCreating = false
        }
    }
}

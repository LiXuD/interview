import SwiftUI

struct TargetCreateView: View {
    let onCreate: (InterviewTargetDTO) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var jd = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("岗位信息") {
                    TextField("岗位名称", text: $title)
                    TextField("粘贴 JD 描述", text: $jd, axis: .vertical)
                        .lineLimit(5...15)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle("新建目标岗位")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("创建") {
                        Task { await create() }
                    }
                    .disabled(title.isEmpty || jd.isEmpty || isLoading)
                }
            }
            .overlay {
                if isLoading {
                    ProgressView()
                        .padding(20)
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
                }
            }
        }
    }

    private func create() async {
        isLoading = true
        errorMessage = nil
        do {
            let dto: InterviewTargetDTO = try await APIClient.shared.request(
                "POST",
                path: "/api/targets",
                body: InterviewTargetCreateRequestDTO(title: title, jd: jd)
            )
            onCreate(dto)
            dismiss()
        } catch {
            errorMessage = "创建失败: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

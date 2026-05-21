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
#if DEBUG
            .onAppear {
                applyDebugPrefillIfNeeded()
            }
#endif
        }
    }

#if DEBUG
    private func applyDebugPrefillIfNeeded() {
        guard title.isEmpty, jd.isEmpty, let prefill = DebugTargetPrefill.current else {
            return
        }

        title = prefill.title
        jd = prefill.jd
    }
#endif

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

#if DEBUG
private struct DebugTargetPrefill {
    let title: String
    let jd: String

    static var current: DebugTargetPrefill? {
        let processInfo = ProcessInfo.processInfo
        let arguments = processInfo.arguments
        guard arguments.contains("-ICDebugPrefillTarget") else {
            return nil
        }

        return DebugTargetPrefill(
            title: value(after: "-ICDebugTargetTitle", in: arguments)
                ?? processInfo.environment["IC_DEBUG_TARGET_TITLE"]
                ?? "银行统一支付平台 Java 后端",
            jd: value(after: "-ICDebugTargetJD", in: arguments)
                ?? processInfo.environment["IC_DEBUG_TARGET_JD"]
                ?? "负责 Spring Boot、PostgreSQL、Redis 支付清结算系统，关注高可用、事务一致性、幂等与性能优化。"
        )
    }

    private static func value(after flag: String, in arguments: [String]) -> String? {
        guard let index = arguments.firstIndex(of: flag) else {
            return nil
        }

        let valueIndex = arguments.index(after: index)
        guard valueIndex < arguments.endIndex else {
            return nil
        }

        let value = arguments[valueIndex]
        return value.isEmpty ? nil : value
    }
}
#endif

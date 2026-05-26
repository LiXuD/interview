import Foundation

enum AiRuntimeStatusGuard {
  static func requireCoreAiAvailable() async throws {
    let status: AiRuntimeStatusDTO = try await APIClient.shared.request(
      "GET",
      path: "/api/ai-providers/status"
    )
    guard status.coreAiAvailable else {
      throw AiRuntimeUnavailableError(status: status)
    }
  }
}

struct AiRuntimeUnavailableError: LocalizedError {
  let status: AiRuntimeStatusDTO

  var errorDescription: String? {
    "当前未连接真实 AI，不能进入真实教练流程。请配置用户 Provider 或平台 AI。当前状态：\(status.status)"
  }
}

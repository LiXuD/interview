import SwiftUI
import SwiftData

struct AppRootView: View {
  @ObservedObject var authService: AuthService
  @Environment(\.modelContext) private var modelContext
  @State private var connectionState: BackendConnectionState = .checking

  var body: some View {
    NavigationStack {
      if authService.isAuthenticated {
        TargetListView(authService: authService)
          .toolbar {
            ToolbarItem(placement: .topBarLeading) {
              connectionBadge
            }
            ToolbarItem(placement: .topBarTrailing) {
              Menu {
                Button("退出登录", role: .destructive) {
                  authService.logout()
                }
                Button("重新检查连接") {
                  Task { await refreshHealth() }
                }
              } label: {
                Image(systemName: "ellipsis.circle")
              }
            }
          }
      } else {
        DevLoginView(authService: authService)
      }
    }
    .task {
      authService.setModelContext(modelContext)
      if authService.isAuthenticated {
        await initializeSession()
      }
    }
    .onChange(of: authService.isAuthenticated) { _, newValue in
      if newValue {
        Task { await refreshHealth() }
      }
    }
  }

  private var connectionBadge: some View {
    Label(connectionState.shortTitle, systemImage: connectionState.systemImage)
      .font(.caption2)
      .foregroundStyle(connectionState.tint)
      .padding(.horizontal, 8)
      .padding(.vertical, 4)
      .background(connectionState.tint.opacity(0.12))
      .clipShape(Capsule())
  }

  @MainActor
  private func initializeSession() async {
    async let _: () = authService.fetchCurrentUser()
    async let _: () = refreshHealth()
  }

  @MainActor
  private func refreshHealth() async {
    connectionState = .checking

    do {
      let response: HealthResponseDTO = try await APIClient.shared.request("GET", path: "/api/health", authorized: false)
      connectionState = .connected(service: response.service)
    } catch {
      connectionState = .failed
    }
  }
}

private enum BackendConnectionState: Equatable {
  case checking
  case connected(service: String)
  case failed

  var title: String {
    switch self {
    case .checking:
      return "Checking backend..."
    case .connected:
      return "Backend connected"
    case .failed:
      return "Backend unavailable"
    }
  }

  var shortTitle: String {
    switch self {
    case .checking:
      return "检查中"
    case .connected:
      return "已连接"
    case .failed:
      return "未连接"
    }
  }

  var message: String {
    switch self {
    case .checking:
      return "正在请求 http://127.0.0.1:18080/api/health。"
    case .connected(let service):
      return "\(service) 已响应，iOS 到后端的最小链路已打通。"
    case .failed:
      return "请先启动后端服务，然后重新检查连接。"
    }
  }

  var systemImage: String {
    switch self {
    case .checking:
      return "arrow.triangle.2.circlepath"
    case .connected:
      return "checkmark.circle.fill"
    case .failed:
      return "xmark.octagon.fill"
    }
  }

  var tint: Color {
    switch self {
    case .checking:
      return .orange
    case .connected:
      return .green
    case .failed:
      return .red
    }
  }
}

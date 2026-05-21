import SwiftUI

struct AppRootView: View {
  @ObservedObject var authService: AuthService
  @State private var connectionState: BackendConnectionState = .checking

  var body: some View {
    NavigationStack {
      if authService.isAuthenticated {
        VStack(alignment: .leading, spacing: 24) {
          VStack(alignment: .leading, spacing: 8) {
            Text("AI 技术岗面试教练")
              .font(.largeTitle)
              .fontWeight(.semibold)

            if let user = authService.currentUser {
              Text("欢迎，\(user.username)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }
          }

          connectionPanel

          Button("退出登录") {
            authService.logout()
          }
          .buttonStyle(.bordered)
          .tint(.red)

          Spacer()
        }
        .padding(24)
        .navigationTitle("Interview Coach")
      } else {
        DevLoginView(authService: authService)
      }
    }
    .task {
      if authService.isAuthenticated {
        await authService.fetchCurrentUser()
        await refreshHealth()
      }
    }
    .onChange(of: authService.isAuthenticated) { _, newValue in
      if newValue {
        Task {
          await authService.fetchCurrentUser()
          await refreshHealth()
        }
      }
    }
  }

  private var connectionPanel: some View {
    VStack(alignment: .leading, spacing: 12) {
      Label(connectionState.title, systemImage: connectionState.systemImage)
        .font(.headline)
        .foregroundStyle(connectionState.tint)

      Text(connectionState.message)
        .font(.body)
        .foregroundStyle(.secondary)

      Button("重新检查") {
        Task {
          await refreshHealth()
        }
      }
      .buttonStyle(.borderedProminent)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(20)
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
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

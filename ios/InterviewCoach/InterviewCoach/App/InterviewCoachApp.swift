import SwiftUI

@main
struct InterviewCoachApp: App {
  @StateObject private var authService = AuthService()

  var body: some Scene {
    WindowGroup {
      AppRootView(authService: authService)
    }
  }
}

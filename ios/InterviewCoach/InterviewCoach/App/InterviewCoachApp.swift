import SwiftUI
import SwiftData

@main
struct InterviewCoachApp: App {
  @StateObject private var authService = AuthService()

  var body: some Scene {
    WindowGroup {
      AppRootView(authService: authService)
    }
    .modelContainer(for: [TargetLocal.self, CandidateProfileLocal.self, CoachingMemoryArchiveLocal.self])
  }
}

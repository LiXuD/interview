import Foundation
import SwiftUI
import SwiftData

@MainActor
final class AuthService: ObservableObject {
    @Published var isAuthenticated: Bool
    @Published var currentUser: UserDTO?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var hasUnimportedMemories = false

    private let apiClient = APIClient.shared
    private var modelContext: ModelContext?

    init() {
        self.isAuthenticated = KeychainHelper.loadToken() != nil
    }

    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    func devLogin(username: String) async {
        await performLogin(path: "/api/auth/dev-login", body: LoginRequestDTO(username: username))
    }

    func appleLogin(identityToken: String, fullName: String?, nonce: String? = nil) async {
        await performLogin(path: "/api/auth/apple", body: AppleLoginRequestDTO(identityToken: identityToken, fullName: fullName, nonce: nonce))
    }

    private func performLogin<Body: Encodable>(path: String, body: Body) async {
        isLoading = true
        errorMessage = nil
        do {
            let response: LoginResponseDTO = try await apiClient.request(
                "POST",
                path: path,
                body: body,
                authorized: false
            )
            KeychainHelper.saveToken(response.token)
            currentUser = UserDTO(id: response.userId, username: response.username)
            isAuthenticated = true
            checkForUnimportedMemories()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func logout() {
        _ = KeychainHelper.deleteToken()
        clearLocalData()
        currentUser = nil
        isAuthenticated = false
        hasUnimportedMemories = false
    }

    func deleteAccount(deleteLocalMemories: Bool = false) async {
        isLoading = true
        errorMessage = nil
        do {
            try await apiClient.requestNoContent("DELETE", path: "/api/me")
            if deleteLocalMemories, let context = modelContext {
                CoachingMemoryArchiveLocal.deleteAll(in: context)
            }
            logout()
        } catch APIError.unauthorized {
            logout()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func checkForUnimportedMemories() {
        guard let context = modelContext else {
            hasUnimportedMemories = false
            return
        }
        let descriptor = FetchDescriptor<CoachingMemoryArchiveLocal>(
            predicate: #Predicate { $0.importConfirmedAt == nil }
        )
        let count = (try? context.fetchCount(descriptor)) ?? 0
        hasUnimportedMemories = count > 0
    }

    private func clearLocalData() {
        guard let context = modelContext else { return }
        try? context.delete(model: TargetLocal.self)
        try? context.delete(model: CandidateProfileLocal.self)
        // CoachingMemoryArchiveLocal is intentionally NOT deleted here.
        // It is only deleted when the user explicitly opts in via deleteAccount(deleteLocalMemories: true).
        try? context.save()
    }

    func fetchCurrentUser() async {
        do {
            let user: UserDTO = try await apiClient.request("GET", path: "/api/me")
            currentUser = user
            checkForUnimportedMemories()
        } catch APIError.unauthorized {
            logout()
        } catch {
            // Network error — keep token, user can retry
        }
    }
}

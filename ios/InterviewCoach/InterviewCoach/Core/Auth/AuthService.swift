import Foundation
import SwiftUI
import SwiftData

@MainActor
final class AuthService: ObservableObject {
    @Published var isAuthenticated: Bool
    @Published var currentUser: UserDTO?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let apiClient = APIClient.shared
    private var modelContext: ModelContext?

    init() {
        self.isAuthenticated = KeychainHelper.loadToken() != nil
    }

    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    func devLogin(username: String) async {
        isLoading = true
        errorMessage = nil
        do {
            let response: LoginResponseDTO = try await apiClient.request(
                "POST",
                path: "/api/auth/dev-login",
                body: LoginRequestDTO(username: username),
                authorized: false
            )
            KeychainHelper.saveToken(response.token)
            currentUser = UserDTO(id: response.userId, username: response.username)
            isAuthenticated = true
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func appleLogin(identityToken: String, fullName: String?) async {
        isLoading = true
        errorMessage = nil
        do {
            let response: LoginResponseDTO = try await apiClient.request(
                "POST",
                path: "/api/auth/apple",
                body: AppleLoginRequestDTO(identityToken: identityToken, fullName: fullName),
                authorized: false
            )
            KeychainHelper.saveToken(response.token)
            currentUser = UserDTO(id: response.userId, username: response.username)
            isAuthenticated = true
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
    }

    func deleteAccount() async {
        isLoading = true
        errorMessage = nil
        do {
            try await apiClient.requestNoContent("DELETE", path: "/api/me")
            logout()
        } catch APIError.unauthorized {
            logout()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func clearLocalData() {
        guard let context = modelContext else { return }
        try? context.delete(model: TargetLocal.self)
        try? context.delete(model: CandidateProfileLocal.self)
        try? context.save()
    }

    func fetchCurrentUser() async {
        do {
            let user: UserDTO = try await apiClient.request("GET", path: "/api/me")
            currentUser = user
        } catch APIError.unauthorized {
            _ = KeychainHelper.deleteToken()
            isAuthenticated = false
        } catch {
            // Network error — keep token, user can retry
        }
    }
}

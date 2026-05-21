import Foundation
import SwiftUI

@MainActor
final class AuthService: ObservableObject {
    @Published var isAuthenticated: Bool
    @Published var currentUser: UserDTO?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let apiClient = APIClient.shared

    init() {
        self.isAuthenticated = KeychainHelper.loadToken() != nil
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

    func logout() {
        _ = KeychainHelper.deleteToken()
        currentUser = nil
        isAuthenticated = false
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

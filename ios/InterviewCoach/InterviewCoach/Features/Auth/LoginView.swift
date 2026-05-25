import SwiftUI
import AuthenticationServices
import CryptoKit

struct LoginView: View {
    @ObservedObject var authService: AuthService
    @State private var showDevLogin = false
    @State private var currentRawNonce: String?

    var body: some View {
        VStack(spacing: 32) {
            Spacer()

            Text("AI 技术岗面试教练")
                .font(.largeTitle)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)

            Text("AI 驱动的面试能力提升工具")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Spacer()

#if DEBUG
            Button("开发模式登录") {
                showDevLogin = true
            }
            .buttonStyle(.borderedProminent)
            .disabled(authService.isLoading)
            .sheet(isPresented: $showDevLogin) {
                NavigationStack {
                    DevLoginView(authService: authService)
                        .toolbar {
                            ToolbarItem(placement: .topBarLeading) {
                                Button("关闭") { showDevLogin = false }
                            }
                    }
                }
            }
#else
            SignInWithAppleButton(.signIn) { request in
                guard currentRawNonce == nil else { return }
                request.requestedScopes = [.fullName]
                let rawNonce = generateNonce()
                currentRawNonce = rawNonce
                request.nonce = sha256(rawNonce)
            } onCompletion: { result in
                handleAppleSignIn(result: result, rawNonce: currentRawNonce)
                currentRawNonce = nil
            }
            .signInWithAppleButtonStyle(.black)
            .frame(height: 50)
            .cornerRadius(10)
            .disabled(authService.isLoading)
#endif

            if let error = authService.errorMessage {
                Text(error)
                    .foregroundStyle(.red)
                    .font(.caption)
                    .multilineTextAlignment(.center)
            }

            if authService.isLoading {
                ProgressView()
            }

            Spacer()
                .frame(height: 20)
        }
        .padding(24)
    }

    private func handleAppleSignIn(result: Result<ASAuthorization, Error>, rawNonce: String?) {
        switch result {
        case .success(let authorization):
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let tokenData = credential.identityToken,
                  let identityToken = String(data: tokenData, encoding: .utf8) else {
                authService.errorMessage = "无法获取 Apple 登录凭证"
                return
            }
            var fullName: String?
            if let name = credential.fullName {
                let formatted = PersonNameComponentsFormatter().string(from: name)
                if !formatted.isEmpty {
                    fullName = formatted
                }
            }
            Task {
                await authService.appleLogin(identityToken: identityToken, fullName: fullName, nonce: rawNonce)
            }
        case .failure(let error):
            if (error as? ASAuthorizationError)?.code != .canceled {
                authService.errorMessage = error.localizedDescription
            }
        }
    }

    private func generateNonce() -> String {
        let length = 32
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remainingLength = length
        while remainingLength > 0 {
            let randoms: [UInt8] = (0..<16).map { _ in
                var random: UInt8 = 0
                let status = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                if status != errSecSuccess {
                    fatalError("Unable to generate nonce")
                }
                return random
            }
            for random in randoms {
                if remainingLength == 0 { break }
                if random < charset.count {
                    result.append(charset[Int(random)])
                    remainingLength -= 1
                }
            }
        }
        return result
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashed = SHA256.hash(data: inputData)
        return hashed.map { String(format: "%02x", $0) }.joined()
    }
}

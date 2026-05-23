import SwiftUI
import AuthenticationServices

struct LoginView: View {
    @ObservedObject var authService: AuthService
    @State private var showDevLogin = false

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

            SignInWithAppleButton(.signIn) { request in
                request.requestedScopes = [.fullName]
            } onCompletion: { result in
                handleAppleSignIn(result: result)
            }
            .signInWithAppleButtonStyle(.black)
            .frame(height: 50)
            .cornerRadius(10)
            .disabled(authService.isLoading)

            if let error = authService.errorMessage {
                Text(error)
                    .foregroundStyle(.red)
                    .font(.caption)
                    .multilineTextAlignment(.center)
            }

            if authService.isLoading {
                ProgressView()
            }

            #if DEBUG
            Divider()
                .padding(.horizontal, 40)

            Button("开发模式登录") {
                showDevLogin = true
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
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
            #endif

            Spacer()
                .frame(height: 20)
        }
        .padding(24)
    }

    private func handleAppleSignIn(result: Result<ASAuthorization, Error>) {
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
                await authService.appleLogin(identityToken: identityToken, fullName: fullName)
            }
        case .failure(let error):
            if (error as NSError).code != ASAuthorizationError.canceled.rawValue {
                authService.errorMessage = error.localizedDescription
            }
        }
    }
}

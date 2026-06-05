import SwiftUI

struct DevLoginView: View {
    @ObservedObject var authService: AuthService
    @State private var username = ""

    var body: some View {
        VStack(spacing: 24) {
            Text("AI 面试教练")
                .font(.largeTitle)
                .fontWeight(.semibold)

            Text("开发阶段登录")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            TextField("输入用户名", text: $username)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

            if let error = authService.errorMessage {
                Text(error)
                    .foregroundStyle(.red)
                    .font(.caption)
            }

            Button("登录") {
                Task { await authService.devLogin(username: username) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(username.trimmingCharacters(in: .whitespaces).isEmpty || authService.isLoading)

            if authService.isLoading {
                ProgressView()
            }

            Spacer()
        }
        .padding(24)
    }
}

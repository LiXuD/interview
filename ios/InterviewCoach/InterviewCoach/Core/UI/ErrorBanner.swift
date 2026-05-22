import SwiftUI

struct ErrorBanner: View {
    let message: String
    var retryAction: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .top) {
            Text(message)
                .font(.caption)
                .foregroundStyle(.red)
            if let retryAction {
                Spacer()
                Button("重试", action: retryAction)
                    .font(.caption)
            }
        }
    }
}

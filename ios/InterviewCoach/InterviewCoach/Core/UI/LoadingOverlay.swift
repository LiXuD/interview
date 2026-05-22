import SwiftUI

struct LoadingOverlay: ViewModifier {
    let isLoading: Bool
    let message: String

    func body(content: Content) -> some View {
        content.overlay {
            if isLoading {
                ProgressView(message)
                    .padding(20)
                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
            }
        }
    }
}

extension View {
    func loadingOverlay(isLoading: Bool, message: String = "处理中...") -> some View {
        modifier(LoadingOverlay(isLoading: isLoading, message: message))
    }
}

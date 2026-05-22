import SwiftUI

struct OnboardingView: View {
    @Binding var hasCompletedOnboarding: Bool
    @State private var currentPage = 0

    private let pages = OnboardingPage.allPages

    var body: some View {
        VStack {
            TabView(selection: $currentPage) {
                ForEach(Array(pages.enumerated()), id: \.offset) { index, page in
                    pageView(page)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))

            Button {
                if currentPage < pages.count - 1 {
                    withAnimation { currentPage += 1 }
                } else {
                    hasCompletedOnboarding = true
                }
            } label: {
                Text(currentPage < pages.count - 1 ? "下一步" : "开始使用")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(.blue)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }

    private func pageView(_ page: OnboardingPage) -> some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: page.icon)
                .font(.system(size: 64))
                .foregroundStyle(.blue)

            Text(page.title)
                .font(.title)
                .fontWeight(.bold)

            Text(page.description)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            if !page.details.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(page.details, id: \.self) { detail in
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                                .font(.caption)
                            Text(detail)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.horizontal, 40)
            }

            Spacer()
        }
    }
}

private struct OnboardingPage: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let description: String
    let details: [String]

    static let allPages: [OnboardingPage] = [
        OnboardingPage(
            icon: "person.wave.2",
            title: "AI 面试教练",
            description: "你的专属技术岗面试准备助手。从岗位分析到模拟面试，AI 全程陪伴。",
            details: []
        ),
        OnboardingPage(
            icon: "arrow.triangle.branch",
            title: "核心流程",
            description: "四个步骤，完成一次完整的面试准备闭环：",
            details: [
                "创建目标岗位，粘贴 JD",
                "输入简历经历，AI 生成摘要",
                "生成岗位画像，完成 5 题测评",
                "训练 + 模拟面试，查看复盘报告"
            ]
        ),
        OnboardingPage(
            icon: "hand.raised",
            title: "隐私保护",
            description: "你的数据安全是我们的第一优先级。",
            details: [
                "简历原文仅在设备本地存储",
                "AI 摘要生成后原文不落服务器",
                "API Key 加密保存，不明文返回",
                "删除账号将清除所有远端数据"
            ]
        )
    ]
}

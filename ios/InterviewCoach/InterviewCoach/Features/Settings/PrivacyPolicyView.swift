import SwiftUI

struct PrivacyPolicyView: View {
    var body: some View {
        List {
            Section("数据存储") {
                Text("简历原文仅保存在你的设备本地（SwiftData），不会上传到服务器持久化存储。")
                Text("AI Provider 的 API Key 在服务端使用 AES-GCM 加密保存，不会以明文返回给客户端。")
            }

            Section("简历隐私") {
                Text("当你使用「生成摘要」功能时，简历原文会临时发送到后端进行 AI 摘要生成。")
                Text("后端仅在内存中使用原文，不会写入数据库，也不会记录到日志。")
                Text("你确认摘要后，服务端只保存结构化摘要内容，不保留原文。")
            }

            Section("账号删除") {
                Text("删除账号将永久清除服务端所有数据，包括目标岗位、候选人画像、测评记录、训练计划、模拟面试和报告。")
                Text("本地 SwiftData 数据和 Keychain 中的登录凭证也会一并清除。")
            }

            Section("数据隔离") {
                Text("不同用户之间无法互相访问彼此的数据。所有业务查询均包含用户身份校验。")
            }
        }
        .navigationTitle("隐私政策")
        .navigationBarTitleDisplayMode(.inline)
    }
}

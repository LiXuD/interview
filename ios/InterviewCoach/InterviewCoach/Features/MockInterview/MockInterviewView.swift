import SwiftUI

struct MockInterviewView: View {
    let target: InterviewTargetDTO

    @Environment(\.dismiss) private var dismiss
    @State private var session: MockInterviewSessionDTO?
    @State private var report: MockInterviewReportDTO?
    @State private var answerText = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if let report {
                    MockInterviewResultView(report: report)
                } else if let session {
                    interviewView(session)
                } else if !isLoading {
                    startView
                }
            }
            .navigationTitle("模拟面试")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
            .loadingOverlay(isLoading: isLoading)
        }
    }

    private var startView: some View {
        Form {
            if let errorMessage {
                Section {
                    ErrorBanner(message: errorMessage)
                }
            }

            Section {
                ContentUnavailableView(
                    "模拟面试",
                    systemImage: "person.wave.2",
                    description: Text("AI 将基于岗位画像和你的经历进行文字模拟面试，面试结束后生成复盘报告。")
                )
            }

            Section {
                Button {
                    Task { await startInterview() }
                } label: {
                    Label("开始面试", systemImage: "play.circle.fill")
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func interviewView(_ session: MockInterviewSessionDTO) -> some View {
        Form {
            if let errorMessage {
                Section {
                    ErrorBanner(message: errorMessage)
                }
            }

            Section {
                LabeledContent("轮次", value: "\(session.conversationTurns)")
                LabeledContent("状态") {
                    Text(session.status == "in_progress" ? "进行中" : "已完成")
                        .foregroundStyle(session.status == "in_progress" ? .orange : .green)
                }
            }

            if let question = session.currentQuestion {
                Section("面试官提问") {
                    Text(question)
                        .font(.body)
                        .italic()
                }
            }

            if session.status == "in_progress" {
                Section("你的回答") {
                    TextEditor(text: $answerText)
                        .frame(minHeight: 100)
                }

                Section {
                    Button {
                        Task { await submitAnswer() }
                    } label: {
                        Label("提交回答", systemImage: "arrow.right.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(answerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                }

                Section {
                    Button {
                        Task { await finishInterview() }
                    } label: {
                        Label("结束面试", systemImage: "flag.checkered")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(isLoading)
                }
            }
        }
    }

    private func startInterview() async {
        isLoading = true
        errorMessage = nil
        do {
            try await AiRuntimeStatusGuard.requireCoreAiAvailable()
            session = try await APIClient.shared.request(
                "POST",
                path: "/api/mock-interviews/start",
                body: MockInterviewStartRequestDTO(targetId: target.id)
            )
        } catch {
            errorMessage = "开始失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func submitAnswer() async {
        guard let session else { return }
        isLoading = true
        errorMessage = nil
        do {
            try await AiRuntimeStatusGuard.requireCoreAiAvailable()
            let trimmed = answerText.trimmingCharacters(in: .whitespacesAndNewlines)
            self.session = try await APIClient.shared.request(
                "POST",
                path: "/api/mock-interviews/\(session.id)/answer",
                body: MockInterviewAnswerRequestDTO(answer: trimmed)
            )
            answerText = ""
        } catch {
            errorMessage = "提交失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func finishInterview() async {
        guard let session else { return }
        isLoading = true
        errorMessage = nil
        do {
            try await AiRuntimeStatusGuard.requireCoreAiAvailable()
            report = try await APIClient.shared.request(
                "POST",
                path: "/api/mock-interviews/\(session.id)/finish"
            )
            self.session = nil
        } catch {
            errorMessage = "结束失败: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

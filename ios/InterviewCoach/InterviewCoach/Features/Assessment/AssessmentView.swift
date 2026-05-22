import SwiftUI

struct AssessmentView: View {
    let target: InterviewTargetDTO

    @Environment(\.dismiss) private var dismiss
    @State private var session: AssessmentSessionDTO?
    @State private var result: AssessmentResultDTO?
    @State private var answerText = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if let result {
                    AssessmentResultView(result: result, targetTitle: target.title)
                } else if let session {
                    assessmentFlowView(session)
                } else if !isLoading {
                    startView
                }
            }
            .navigationTitle("技术测评")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
            .overlay {
                if isLoading {
                    ProgressView("处理中...")
                        .padding(20)
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
                }
            }
        }
    }

    private var startView: some View {
        Form {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }

            Section {
                ContentUnavailableView(
                    "技术测评",
                    systemImage: "checkmark.shield",
                    description: Text("基于岗位画像和简历，AI 将生成 5 道技术面试题，逐题作答后自动评分。")
                )
            }

            Section {
                Button {
                    Task { await startAssessment() }
                } label: {
                    Label("开始测评", systemImage: "play.fill")
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func assessmentFlowView(_ session: AssessmentSessionDTO) -> some View {
        Form {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }

            Section {
                LabeledContent("进度", value: "\(session.questionIndex) / \(session.totalQuestions)")
                ProgressView(value: Double(session.questionIndex), total: Double(session.totalQuestions))
            }

            if let question = session.currentQuestion {
                Section("第 \(session.questionIndex + 1) 题") {
                    Text(question)
                        .font(.body)
                }

                Section("你的回答") {
                    TextEditor(text: $answerText)
                        .frame(minHeight: 100)
                }

                Section {
                    Button {
                        Task { await submitAnswer(sessionId: session.id) }
                    } label: {
                        Label("提交回答", systemImage: "arrow.right.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(answerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                }
            } else if session.questionIndex >= session.totalQuestions {
                Section("答题完成") {
                    Text("你已回答全部 \(session.totalQuestions) 道题目。")
                        .foregroundStyle(.secondary)
                }

                Section {
                    Button {
                        Task { await finishAssessment(sessionId: session.id) }
                    } label: {
                        Label("查看评分结果", systemImage: "chart.bar.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(isLoading)
                }
            }
        }
    }

    private func startAssessment() async {
        isLoading = true
        errorMessage = nil
        do {
            let startReq = AssessmentStartRequestDTO(targetId: target.id)
            session = try await APIClient.shared.request(
                "POST",
                path: "/api/assessments/start",
                body: startReq
            )
        } catch {
            errorMessage = "启动测评失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func submitAnswer(sessionId: String) async {
        isLoading = true
        errorMessage = nil
        do {
            let trimmed = answerText.trimmingCharacters(in: .whitespacesAndNewlines)
            session = try await APIClient.shared.request(
                "POST",
                path: "/api/assessments/\(sessionId)/answers",
                body: AssessmentAnswerRequestDTO(answer: trimmed)
            )
            answerText = ""
        } catch {
            errorMessage = "提交失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func finishAssessment(sessionId: String) async {
        isLoading = true
        errorMessage = nil
        do {
            result = try await APIClient.shared.request(
                "POST",
                path: "/api/assessments/\(sessionId)/finish"
            )
        } catch {
            errorMessage = "评分失败: \(error.localizedDescription)"
        }
        isLoading = false
    }
}

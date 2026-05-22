import SwiftUI
import SwiftData

struct TargetDetailView: View {
    @ObservedObject var authService: AuthService
    let onDelete: () -> Void
    let onUpdate: (InterviewTargetDTO) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var isEditing = false
    @State private var editTitle = ""
    @State private var editJd = ""
    @State private var currentTarget: InterviewTargetDTO
    @State private var showDeleteConfirm = false
    @State private var showProfile = false
    @State private var showJobBrief = false
    @State private var showAssessment = false
    @State private var showTraining = false
    @State private var showMockInterview = false
    @State private var isLoading = false
    @State private var errorMessage: String?

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    init(target: InterviewTargetDTO, authService: AuthService, onDelete: @escaping () -> Void, onUpdate: @escaping (InterviewTargetDTO) -> Void) {
        self.authService = authService
        self.onDelete = onDelete
        self.onUpdate = onUpdate
        self._currentTarget = State(initialValue: target)
    }

    var body: some View {
        Form {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                        .font(.caption)
                }
            }

            Section("岗位信息") {
                if isEditing {
                    TextField("岗位名称", text: $editTitle)
                    TextField("JD 描述", text: $editJd, axis: .vertical)
                        .lineLimit(5...15)
                } else {
                    LabeledContent("名称", value: currentTarget.title)
                    LabeledContent("状态") {
                        Text(TargetStatusHelper.label(currentTarget.status))
                            .foregroundStyle(TargetStatusHelper.color(currentTarget.status))
                    }
                    LabeledContent("JD", value: currentTarget.jd)
                }
            }

            Section("时间") {
                LabeledContent("创建时间", value: formatDate(currentTarget.createdAt))
                LabeledContent("更新时间", value: formatDate(currentTarget.updatedAt))
            }

            if !isEditing {
                Section {
                    Button {
                        showProfile = true
                    } label: {
                        Label("候选人简历", systemImage: "person.text.rectangle")
                    }
                    Button {
                        showJobBrief = true
                    } label: {
                        Label("岗位画像", systemImage: "doc.text.magnifyingglass")
                    }
                    Button {
                        showAssessment = true
                    } label: {
                        Label("技术测评", systemImage: "checkmark.shield")
                    }
                    Button {
                        showTraining = true
                    } label: {
                        Label("训练计划", systemImage: "figure.run")
                    }
                    Button {
                        showMockInterview = true
                    } label: {
                        Label("模拟面试", systemImage: "person.wave.2")
                    }
                }
            }

            if !isEditing {
                Section {
                    Button("删除此岗位", role: .destructive) {
                        showDeleteConfirm = true
                    }
                }
            }
        }
        .navigationTitle(currentTarget.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if isEditing {
                    Button("保存") {
                        Task { await save() }
                    }
                    .disabled(editTitle.isEmpty || editJd.isEmpty || isLoading)
                } else {
                    Button("编辑") {
                        editTitle = currentTarget.title
                        editJd = currentTarget.jd
                        isEditing = true
                    }
                }
            }
            if isEditing {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") {
                        isEditing = false
                    }
                }
            }
        }
        .confirmationDialog("确定删除此岗位目标？", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("删除", role: .destructive) {
                Task { await deleteTarget() }
            }
        }
        .sheet(isPresented: $showProfile) {
            ProfileInputView(
                targetId: currentTarget.id,
                targetTitle: currentTarget.title,
                authService: authService
            )
        }
        .sheet(isPresented: $showJobBrief) {
            JobBriefView(target: currentTarget)
        }
        .sheet(isPresented: $showAssessment) {
            AssessmentView(target: currentTarget)
        }
        .sheet(isPresented: $showTraining) {
            TrainingPlanView(target: currentTarget)
        }
        .sheet(isPresented: $showMockInterview) {
            MockInterviewView(target: currentTarget)
        }
        .overlay {
            if isLoading {
                ProgressView()
                    .padding(20)
                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
            }
        }
    }

    private func save() async {
        isLoading = true
        errorMessage = nil
        do {
            let updated: InterviewTargetDTO = try await APIClient.shared.request(
                "PATCH",
                path: "/api/targets/\(currentTarget.id)",
                body: InterviewTargetUpdateRequestDTO(title: editTitle, jd: editJd, status: nil)
            )
            currentTarget = updated
            onUpdate(updated)
            isEditing = false
            TargetLocal.sync(updated, in: modelContext)
            try? modelContext.save()
        } catch {
            errorMessage = "保存失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func deleteTarget() async {
        isLoading = true
        errorMessage = nil
        do {
            try await APIClient.shared.requestNoContent("DELETE", path: "/api/targets/\(currentTarget.id)")
            TargetLocal.delete(currentTarget.id, in: modelContext)
            onDelete()
            dismiss()
        } catch {
            errorMessage = "删除失败: \(error.localizedDescription)"
        }
        isLoading = false
    }

    private func formatDate(_ isoString: String) -> String {
        let formatter = Self.isoFormatter
        if let date = formatter.date(from: isoString) {
            return date.formatted(date: .abbreviated, time: .shortened)
        }
        formatter.formatOptions = [.withInternetDateTime]
        if let date = formatter.date(from: isoString) {
            return date.formatted(date: .abbreviated, time: .shortened)
        }
        return isoString
    }
}

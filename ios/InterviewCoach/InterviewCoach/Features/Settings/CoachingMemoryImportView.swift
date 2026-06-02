import SwiftUI
import SwiftData

struct CoachingMemoryImportView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    let currentUserId: String

    @Query private var allArchives: [CoachingMemoryArchiveLocal]
    @State private var selectedIds: Set<String> = []
    @State private var isImporting = false

    private var pendingArchives: [CoachingMemoryArchiveLocal] {
        allArchives.filter { $0.importConfirmedAt == nil }
    }

    var body: some View {
        List {
            if pendingArchives.isEmpty {
                Section {
                    Text("没有待导入的本机教练记忆。")
                        .foregroundStyle(.secondary)
                }
            } else {
                Section("待导入的教练记忆") {
                    ForEach(pendingArchives, id: \.archiveId) { archive in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(archive.source)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(archive.summary)
                                    .lineLimit(2)
                            }
                            Spacer()
                            if selectedIds.contains(archive.archiveId) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(.blue)
                            } else {
                                Image(systemName: "circle")
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            toggleSelection(archive.archiveId)
                        }
                    }
                }

                Section {
                    Button("全选") {
                        selectedIds = Set(pendingArchives.map(\.archiveId))
                    }
                    Button("取消全选") {
                        selectedIds.removeAll()
                    }
                }

                Section {
                    Button {
                        importSelected()
                    } label: {
                        Label("导入选中 (\(selectedIds.count))", systemImage: "square.and.arrow.down")
                    }
                    .disabled(selectedIds.isEmpty || isImporting)

                    Button(role: .destructive) {
                        rejectAll()
                    } label: {
                        Label("全部拒绝", systemImage: "xmark.circle")
                    }
                    .disabled(isImporting)
                }
            }
        }
        .navigationTitle("教练记忆导入")
        .navigationBarTitleDisplayMode(.inline)
        .loadingOverlay(isLoading: isImporting)
    }

    private func toggleSelection(_ id: String) {
        if selectedIds.contains(id) {
            selectedIds.remove(id)
        } else {
            selectedIds.insert(id)
        }
    }

    private func importSelected() {
        isImporting = true
        let now = ISO8601DateFormatter().string(from: Date())
        for archive in pendingArchives where selectedIds.contains(archive.archiveId) {
            archive.userId = currentUserId
            archive.importConfirmedAt = now
        }
        try? modelContext.save()
        isImporting = false
        dismiss()
    }

    private func rejectAll() {
        for archive in pendingArchives {
            modelContext.delete(archive)
        }
        try? modelContext.save()
        dismiss()
    }
}

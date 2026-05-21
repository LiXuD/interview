import SwiftUI
import SwiftData

struct TargetListView: View {
    @ObservedObject var authService: AuthService
    @Environment(\.modelContext) private var modelContext
    @State private var targets: [InterviewTargetDTO] = []
    @State private var isLoading = false
    @State private var showCreate = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if isLoading && targets.isEmpty {
                ProgressView("加载中...")
            } else if targets.isEmpty {
                ContentUnavailableView("暂无岗位目标", systemImage: "briefcase",
                    description: Text("点击右上角 + 创建第一个目标岗位"))
            }

            ForEach(targets, id: \.id) { target in
                NavigationLink(destination: TargetDetailView(target: target, authService: authService, onDelete: {
                    targets.removeAll { $0.id == target.id }
                }, onUpdate: { updated in
                    if let index = targets.firstIndex(where: { $0.id == updated.id }) {
                        targets[index] = updated
                    }
                })) {
                    targetRow(target)
                }
            }

            if let errorMessage {
                Text(errorMessage)
                    .foregroundStyle(.red)
                    .font(.caption)
            }
        }
        .navigationTitle("目标岗位")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreate = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showCreate) {
            TargetCreateView(onCreate: { newTarget in
                targets.insert(newTarget, at: 0)
                TargetLocal.sync(newTarget, in: modelContext)
                try? modelContext.save()
            })
        }
        .refreshable {
            await fetchTargets()
        }
        .task {
            await fetchTargets()
        }
    }

    private func targetRow(_ target: InterviewTargetDTO) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(target.title)
                .font(.headline)

            HStack(spacing: 8) {
                Text(TargetStatusHelper.label(target.status))
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(TargetStatusHelper.color(target.status).opacity(0.15))
                    .foregroundStyle(TargetStatusHelper.color(target.status))
                    .clipShape(Capsule())

                Text(target.jd.prefix(50) + (target.jd.count > 50 ? "..." : ""))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }

    private func fetchTargets() async {
        isLoading = true
        errorMessage = nil
        do {
            let fetched: [InterviewTargetDTO] = try await APIClient.shared.request("GET", path: "/api/targets")
            targets = fetched
            TargetLocal.syncAll(fetched, in: modelContext)
        } catch {
            errorMessage = "加载失败: \(error.localizedDescription)"
            loadFromLocal()
        }
        isLoading = false
    }

    private func loadFromLocal() {
        guard let userId = authService.currentUser?.id else {
            targets = []
            return
        }
        let descriptor = FetchDescriptor<TargetLocal>(
            predicate: #Predicate<TargetLocal> { $0.userId == userId },
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        if let locals = try? modelContext.fetch(descriptor) {
            targets = locals.map { local in
                InterviewTargetDTO(
                    id: local.remoteId,
                    userId: local.userId,
                    title: local.title,
                    jd: local.jd,
                    status: local.status,
                    createdAt: local.createdAt,
                    updatedAt: local.updatedAt
                )
            }
        }
    }
}

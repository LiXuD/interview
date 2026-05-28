import Foundation
import SwiftData

@Model
final class CoachingMemoryArchiveLocal {
    @Attribute(.unique) var archiveId: String
    var userId: String
    var targetId: String
    var summary: String
    var source: String
    var archivedAt: String
    var importConfirmedAt: String?

    init(
        archiveId: String = UUID().uuidString,
        userId: String,
        targetId: String,
        summary: String,
        source: String,
        archivedAt: String,
        importConfirmedAt: String? = nil
    ) {
        self.archiveId = archiveId
        self.userId = userId
        self.targetId = targetId
        self.summary = summary
        self.source = source
        self.archivedAt = archivedAt
        self.importConfirmedAt = importConfirmedAt
    }

    static func deleteAll(in context: ModelContext) {
        try? context.delete(model: CoachingMemoryArchiveLocal.self)
        try? context.save()
    }
}

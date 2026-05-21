import Foundation
import SwiftData

@Model
final class TargetLocal {
    @Attribute(.unique) var remoteId: String
    var userId: String
    var title: String
    var jd: String
    var status: String
    var createdAt: String
    var updatedAt: String

    init(remoteId: String, userId: String, title: String, jd: String, status: String = "active", createdAt: String, updatedAt: String) {
        self.remoteId = remoteId
        self.userId = userId
        self.title = title
        self.jd = jd
        self.status = status
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }

    convenience init(from dto: InterviewTargetDTO) {
        self.init(
            remoteId: dto.id,
            userId: dto.userId,
            title: dto.title,
            jd: dto.jd,
            status: dto.status,
            createdAt: dto.createdAt,
            updatedAt: dto.updatedAt
        )
    }

    func apply(from dto: InterviewTargetDTO) {
        self.userId = dto.userId
        self.title = dto.title
        self.jd = dto.jd
        self.status = dto.status
        self.updatedAt = dto.updatedAt
    }

    static func sync(_ dto: InterviewTargetDTO, in context: ModelContext) {
        let remoteId = dto.id
        let descriptor = FetchDescriptor<TargetLocal>(predicate: #Predicate { $0.remoteId == remoteId })
        if let existing = try? context.fetch(descriptor).first {
            existing.apply(from: dto)
        } else {
            context.insert(TargetLocal(from: dto))
        }
    }

    static func syncAll(_ dtos: [InterviewTargetDTO], in context: ModelContext) {
        for dto in dtos {
            sync(dto, in: context)
        }
        try? context.save()
    }

    static func delete(_ remoteId: String, in context: ModelContext) {
        let descriptor = FetchDescriptor<TargetLocal>(predicate: #Predicate { $0.remoteId == remoteId })
        if let existing = try? context.fetch(descriptor).first {
            context.delete(existing)
            try? context.save()
        }
    }
}

import Foundation
import SwiftData

@Model
final class CandidateProfileLocal {
    @Attribute(.unique) var remoteId: String
    var targetId: String
    var userId: String
    var summary: String
    var skills: [String]
    var projects: [String]
    var experience: [String]
    var confirmedAt: String

    init(remoteId: String, targetId: String, userId: String, summary: String, skills: [String], projects: [String], experience: [String], confirmedAt: String) {
        self.remoteId = remoteId
        self.targetId = targetId
        self.userId = userId
        self.summary = summary
        self.skills = skills
        self.projects = projects
        self.experience = experience
        self.confirmedAt = confirmedAt
    }

    convenience init(from dto: CandidateProfileDTO, userId: String) {
        self.init(
            remoteId: dto.id,
            targetId: dto.targetId,
            userId: userId,
            summary: dto.summary,
            skills: dto.skills,
            projects: dto.projects,
            experience: dto.experience,
            confirmedAt: dto.confirmedAt
        )
    }

    func apply(from dto: CandidateProfileDTO, userId: String) {
        self.remoteId = dto.id
        self.userId = userId
        self.summary = dto.summary
        self.skills = dto.skills
        self.projects = dto.projects
        self.experience = dto.experience
        self.confirmedAt = dto.confirmedAt
    }

    static func sync(_ dto: CandidateProfileDTO, userId: String, in context: ModelContext) {
        let targetId = dto.targetId
        let descriptor = FetchDescriptor<CandidateProfileLocal>(predicate: #Predicate { $0.targetId == targetId })
        if let existing = try? context.fetch(descriptor).first {
            existing.apply(from: dto, userId: userId)
        } else {
            context.insert(CandidateProfileLocal(from: dto, userId: userId))
        }
    }

    static func delete(_ targetId: String, in context: ModelContext) {
        let descriptor = FetchDescriptor<CandidateProfileLocal>(predicate: #Predicate { $0.targetId == targetId })
        if let existing = try? context.fetch(descriptor).first {
            context.delete(existing)
        }
    }
}

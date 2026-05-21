import Foundation

struct JobBriefGenerateRequestDTO: Encodable {
  let targetId: String
}

struct SkillMapItemDTO: Decodable, Equatable {
  let name: String
  let importance: String
  let userLevel: String
  let gap: String
}

struct JobBriefDTO: Decodable, Equatable {
  let targetId: String
  let roleSummary: String
  let skillMap: [SkillMapItemDTO]
  let mustHaveSkills: [String]
  let niceToHaveSkills: [String]
  let businessContext: [String]
  let interviewTopics: [String]
  let candidateMatch: [String]
  let riskAreas: [String]
  let confidence: Double
}

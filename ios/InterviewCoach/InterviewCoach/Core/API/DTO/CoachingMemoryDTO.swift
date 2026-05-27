import Foundation

struct CoachingMemoryDTO: Decodable, Equatable, Identifiable {
  let id: String
  let targetId: String
  let sourceType: String
  let sourceId: String
  let observedStrengths: [CoachingMemoryItemDTO]
  let observedWeaknesses: [CoachingMemoryItemDTO]
  let recurringProblems: [CoachingMemoryItemDTO]
  let verifiedExperience: [CoachingMemoryItemDTO]
  let unverifiedClaims: [CoachingMemoryItemDTO]
  let recommendedNextFocus: [CoachingMemoryItemDTO]
  let avoidRepeating: [CoachingMemoryItemDTO]
  let createdAt: String
}

struct CoachingMemoryItemDTO: Decodable, Equatable {
  let content: String
  let source: String
  let confidence: String
}

struct CoachingMemoryCorrectionRequestDTO: Encodable {
  let field: String
  let itemIndex: Int
  let source: String
  let content: String
}

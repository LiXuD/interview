import Foundation

struct DimensionAnalysisDTO: Decodable, Equatable {
    let targetId: String
    let dimensions: [DimensionDetailDTO]
}

struct DimensionDetailDTO: Decodable, Equatable, Identifiable {
    var id: String { name }
    let name: String
    let latestScore: Int?
    let trend: String?
    let scoreHistory: [DimensionScoreEntryDTO]
    let weaknesses: [String]
    let evidenceSources: [String]
    let nextFocus: [String]
}

struct DimensionScoreEntryDTO: Decodable, Equatable {
    let score: Int
    let source: String
    let createdAt: String
}

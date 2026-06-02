import Foundation

struct ProgressDashboardDTO: Decodable, Equatable {
    let targetId: String
    let latestAssessmentScore: Int?
    let scoreTrend: [ScoreTrendEntryDTO]
    let trainingCompletion: TrainingCompletionDTO
    let dimensionSummary: [DimensionSummaryDTO]
    let recentWeaknesses: [String]
}

struct ScoreTrendEntryDTO: Decodable, Equatable {
    let score: Int
    let source: String
    let createdAt: String
}

struct TrainingCompletionDTO: Decodable, Equatable {
    let totalTasks: Int
    let completedTasks: Int
    let completionRate: Double
}

struct DimensionSummaryDTO: Decodable, Equatable, Identifiable {
    var id: String { name }
    let name: String
    let latestScore: Int?
    let trend: String?
}

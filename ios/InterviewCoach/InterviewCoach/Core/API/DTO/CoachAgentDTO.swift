import Foundation

struct CoachAgentDTO: Decodable, Equatable {
    let id: String
    let targetId: String
    let status: String
    let currentStage: String
    let currentGoal: String?
    let activeFocusDimensions: [String]
    let nextRecommendedAction: String?
    let lastEventType: String?
    let lastDecisionSummary: String?
    let lastRunAt: String?
    let createdAt: String
    let updatedAt: String
}

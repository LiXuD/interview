import Foundation

struct TrainingPlanGenerateRequestDTO: Encodable {
  let targetId: String
}

struct TrainingTaskDTO: Decodable, Equatable, Identifiable {
  let id: String
  let title: String
  let description: String
  let status: String
  let feedback: String?
  let completedAt: String?
  let dayIndex: Int
}

struct TrainingPlanDTO: Decodable, Equatable {
  let id: String
  let targetId: String
  let tasks: [TrainingTaskDTO]
  let totalDays: Int
  let status: String
  let createdAt: String
}

struct TrainingTaskAnswerRequestDTO: Encodable {
  let answer: String
}

struct TrainingFeedbackDTO: Decodable, Equatable {
  let taskId: String
  let score: Int
  let feedback: String
  let problems: [String]
  let rewrittenAnswer: String
  let followUpQuestion: String
  let recommendedReviewPoints: [String]
}

struct AdaptiveTrainingAnswerRequestDTO: Encodable {
  let answer: String
}

struct AdaptiveTrainingRoundDTO: Decodable, Equatable, Identifiable {
  var id: Int { roundIndex }
  let roundIndex: Int
  let question: String
  let answer: String
  let action: String
  let score: Int
  let feedback: String
  let problems: [String]
}

struct AdaptiveTrainingSessionDTO: Decodable, Equatable {
  let id: String
  let taskId: String
  let status: String
  let roundIndex: Int
  let minRounds: Int
  let maxRounds: Int
  let currentQuestion: String?
  let lastAction: String?
  let summary: String?
  let rounds: [AdaptiveTrainingRoundDTO]
}

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
}

struct TrainingPlanDTO: Decodable, Equatable {
  let id: String
  let targetId: String
  let tasks: [TrainingTaskDTO]
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

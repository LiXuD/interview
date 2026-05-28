import Foundation

struct MockInterviewStartRequestDTO: Encodable {
  let targetId: String
}

struct MockInterviewSessionDTO: Decodable, Equatable {
  let id: String
  let targetId: String
  let status: String
  let currentQuestion: String?
  let conversationTurns: Int
}

struct MockInterviewAnswerRequestDTO: Encodable {
  let answer: String
}

struct MockInterviewReportDTO: Decodable, Equatable {
  let mockInterviewId: String
  let overallScore: Int
  let dimensionScores: [DimensionScoreDTO]
  let summary: String
  let strengths: [String]
  let weaknesses: [String]
  let improvedAnswers: [String]
  let likelyFollowUpPoints: [String]
  let nextTrainingTasks: [String]
}

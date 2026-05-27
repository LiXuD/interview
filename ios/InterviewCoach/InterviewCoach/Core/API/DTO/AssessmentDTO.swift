import Foundation

struct AssessmentStartRequestDTO: Encodable {
  let targetId: String
}

struct AssessmentSessionDTO: Decodable, Equatable {
  let id: String
  let targetId: String
  let status: String
  let questionIndex: Int
  let totalQuestions: Int
  let currentQuestion: AssessmentQuestionDTO?
  let questions: [AssessmentQuestionDTO]
  let questionScores: [AssessmentQuestionScoreDTO]
}

struct AssessmentQuestionDTO: Decodable, Equatable {
  let question: String
  let dimension: String
  let difficulty: String
  let intent: String
  let rubric: [String]
}

struct AssessmentQuestionScoreDTO: Decodable, Equatable {
  let questionIndex: Int
  let score: Int
  let dimension: String
  let feedback: String
  let problems: [String]
  let improvedExample: String
}

struct AssessmentAnswerRequestDTO: Encodable {
  let answer: String
}

struct DimensionScoreDTO: Decodable, Equatable {
  let name: String
  let score: Int
  let reason: String
}

struct AssessmentResultDTO: Decodable, Equatable {
  let assessmentId: String
  let totalScore: Int
  let dimensions: [DimensionScoreDTO]
  let strengths: [String]
  let weaknesses: [String]
  let nextActions: [String]
}

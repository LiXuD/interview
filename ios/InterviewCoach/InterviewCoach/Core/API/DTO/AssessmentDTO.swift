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

struct AnswerStructureDTO: Decodable, Equatable {
  let background: String
  let task: String
  let action: String
  let result: String
  let tradeoff: String
  let review: String
}

struct AssessmentQuestionScoreDTO: Decodable, Equatable {
  let questionIndex: Int
  let score: Int
  let dimension: String
  let feedback: String
  let problems: [String]
  let improvedExample: String
  let answerStructure: AnswerStructureDTO?
  let followUpRisks: [String]
  let contentHighlights: [String]
  let contentGaps: [String]

  private enum CodingKeys: String, CodingKey {
    case questionIndex
    case score
    case dimension
    case feedback
    case problems
    case improvedExample
    case answerStructure
    case followUpRisks
    case contentHighlights
    case contentGaps
  }

  init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    questionIndex = try container.decode(Int.self, forKey: .questionIndex)
    score = try container.decode(Int.self, forKey: .score)
    dimension = try container.decode(String.self, forKey: .dimension)
    feedback = try container.decode(String.self, forKey: .feedback)
    problems = try container.decodeIfPresent([String].self, forKey: .problems) ?? []
    improvedExample = try container.decode(String.self, forKey: .improvedExample)
    answerStructure = try container.decodeIfPresent(AnswerStructureDTO.self, forKey: .answerStructure)
    followUpRisks = try container.decodeIfPresent([String].self, forKey: .followUpRisks) ?? []
    contentHighlights = try container.decodeIfPresent([String].self, forKey: .contentHighlights) ?? []
    contentGaps = try container.decodeIfPresent([String].self, forKey: .contentGaps) ?? []
  }
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
  let questionScores: [AssessmentQuestionScoreDTO]?
}

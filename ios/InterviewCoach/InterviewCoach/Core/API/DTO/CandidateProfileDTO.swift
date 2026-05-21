import Foundation

struct CandidateProfileDraftRequestDTO: Encodable {
  let resumeText: String?
  let projectRawText: String?
}

struct CandidateProfileDraftDTO: Decodable, Equatable {
  let summary: String
  let skills: [String]
  let projects: [String]
  let experience: [String]
  let rawTextLength: Int
}

struct CandidateProfileConfirmRequestDTO: Encodable {
  let targetId: String
  let summary: String
  let skills: [String]
  let projects: [String]
  let experience: [String]
}

struct CandidateProfileDTO: Decodable, Equatable {
  let id: String
  let targetId: String
  let summary: String
  let skills: [String]
  let projects: [String]
  let experience: [String]
  let confirmedAt: String
}

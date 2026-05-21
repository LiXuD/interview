import Foundation

struct InterviewTargetCreateRequestDTO: Encodable {
  let title: String
  let jd: String
}

struct InterviewTargetUpdateRequestDTO: Encodable {
  let title: String?
  let jd: String?
  let status: String?
}

struct InterviewTargetDTO: Decodable, Equatable {
  let id: String
  let userId: String
  let title: String
  let jd: String
  let status: String
  let createdAt: String
  let updatedAt: String
}

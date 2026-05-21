import Foundation

struct ReportDTO: Decodable, Equatable {
  let id: String
  let targetId: String
  let type: String
  let content: String
  let createdAt: String
}

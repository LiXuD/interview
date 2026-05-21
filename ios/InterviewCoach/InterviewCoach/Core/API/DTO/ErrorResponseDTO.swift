import Foundation

struct ErrorResponseDTO: Decodable, Equatable {
  let code: String
  let message: String
  let requestId: String
}

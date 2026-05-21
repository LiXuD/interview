import Foundation

struct HealthResponseDTO: Decodable, Equatable {
  let status: String
  let service: String
}

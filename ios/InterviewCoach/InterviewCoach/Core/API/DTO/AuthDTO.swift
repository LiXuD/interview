import Foundation

struct LoginRequestDTO: Encodable {
  let username: String
}

struct LoginResponseDTO: Decodable, Equatable {
  let token: String
  let userId: String
  let username: String
}

struct UserDTO: Decodable, Equatable {
  let id: String
  let username: String
}

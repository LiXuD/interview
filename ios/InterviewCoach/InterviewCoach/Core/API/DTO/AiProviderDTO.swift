import Foundation

struct AiProviderCreateRequestDTO: Encodable {
  let name: String
  let baseUrl: String
  let apiKey: String
  let model: String
  let openaiApiMode: String
}

struct AiProviderDTO: Decodable, Equatable {
  let id: String
  let name: String
  let baseUrl: String
  let model: String
  let openaiApiMode: String
  let isDefault: Bool
  let createdAt: String
}

struct AiProviderTestRequestDTO: Encodable {
  let baseUrl: String
  let apiKey: String
  let model: String
  let openaiApiMode: String
}

struct AiProviderTestResponseDTO: Decodable, Equatable {
  let success: Bool
  let message: String?
}

struct AiProviderModelsRequestDTO: Encodable {
  let baseUrl: String
  let apiKey: String
}

struct AiProviderModelsResponseDTO: Decodable, Equatable {
  let models: [String]
}

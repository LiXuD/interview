import Foundation

struct BackendHealthClient {
  private let healthURL = URL(string: "http://127.0.0.1:18080/api/health")!

  func fetchHealth() async throws -> HealthResponseDTO {
    let (data, response) = try await URLSession.shared.data(from: healthURL)

    guard let httpResponse = response as? HTTPURLResponse,
          (200..<300).contains(httpResponse.statusCode) else {
      throw BackendHealthError.unexpectedStatusCode
    }

    return try JSONDecoder().decode(HealthResponseDTO.self, from: data)
  }
}

enum BackendHealthError: Error {
  case unexpectedStatusCode
}

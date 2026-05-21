import Foundation

actor APIClient {
    static let shared = APIClient()

    private let baseURL = URL(string: "http://127.0.0.1:18080")!
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    func request<Response: Decodable>(
        _ method: String,
        path: String,
        authorized: Bool = true
    ) async throws -> Response {
        try await perform(method, path: path, body: nil as String?, authorized: authorized)
    }

    func request<Body: Encodable, Response: Decodable>(
        _ method: String,
        path: String,
        body: Body,
        authorized: Bool = true
    ) async throws -> Response {
        try await perform(method, path: path, body: body, authorized: authorized)
    }

    func requestNoContent<Body: Encodable>(
        _ method: String,
        path: String,
        body: Body? = nil as String?,
        authorized: Bool = true
    ) async throws {
        var urlRequest = try buildRequest(method, path: path, authorized: authorized)
        if let body = body {
            urlRequest.httpBody = try encoder.encode(body)
        }
        let (_, response) = try await URLSession.shared.data(for: urlRequest)
        try validateResponse(response)
    }

    private func perform<Body: Encodable, Response: Decodable>(
        _ method: String,
        path: String,
        body: Body?,
        authorized: Bool
    ) async throws -> Response {
        var urlRequest = try buildRequest(method, path: path, authorized: authorized)
        if let body = body {
            urlRequest.httpBody = try encoder.encode(body)
        }
        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        try validateResponse(response)
        if (response as? HTTPURLResponse)?.statusCode == 204 {
            return EmptyResponse() as! Response
        }
        return try decoder.decode(Response.self, from: data)
    }

    private func buildRequest(_ method: String, path: String, authorized: Bool) throws -> URLRequest {
        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authorized, let token = KeychainHelper.loadToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    private func validateResponse(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        if http.statusCode == 401 {
            throw APIError.unauthorized
        }
        guard (200..<300).contains(http.statusCode) else {
            throw APIError.serverError(http.statusCode)
        }
    }
}

enum APIError: Error, LocalizedError {
    case invalidResponse
    case unauthorized
    case serverError(Int)

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid response"
        case .unauthorized: return "Authentication required"
        case .serverError(let code): return "Server error: \(code)"
        }
    }
}

struct EmptyResponse: Decodable {}

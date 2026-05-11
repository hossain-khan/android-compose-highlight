import Foundation

protocol Fetchable {
    associatedtype Resource: Decodable
    var baseURL: URL { get }
    func fetch(id: Int) async throws -> Resource
}

struct User: Decodable, Identifiable {
    let id: Int
    let name: String
    let email: String?
}

enum NetworkError: LocalizedError {
    case badStatus(Int)
    case decodingFailed(Error)

    var errorDescription: String? {
        switch self {
        case .badStatus(let code): return "Server returned HTTP \(code)"
        case .decodingFailed(let e): return "Decode error: \(e.localizedDescription)"
        }
    }
}

struct UserService: Fetchable {
    typealias Resource = User
    let baseURL = URL(string: "https://api.example.com")!

    func fetch(id: Int) async throws -> User {
        let url = baseURL.appendingPathComponent("users/\(id)")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            throw NetworkError.badStatus((response as? HTTPURLResponse)?.statusCode ?? -1)
        }
        do {
            return try JSONDecoder().decode(User.self, from: data)
        } catch {
            throw NetworkError.decodingFailed(error)
        }
    }
}
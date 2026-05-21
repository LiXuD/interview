import SwiftUI

enum TargetStatusHelper {
    static func label(_ status: String) -> String {
        switch status {
        case "active": return "进行中"
        case "completed": return "已完成"
        case "archived": return "已归档"
        default: return status
        }
    }

    static func color(_ status: String) -> Color {
        switch status {
        case "active": return .green
        case "completed": return .blue
        case "archived": return .gray
        default: return .secondary
        }
    }
}

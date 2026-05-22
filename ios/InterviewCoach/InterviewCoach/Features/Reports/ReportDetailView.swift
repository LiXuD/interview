import SwiftUI

struct ReportDetailView: View {
    let report: ReportDTO

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Label(typeLabel, systemImage: typeIcon)
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(typeColor.opacity(0.15))
                        .foregroundStyle(typeColor)
                        .clipShape(Capsule())

                    Spacer()

                    Text(DateHelper.formatISODate(report.createdAt))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }

                Text(report.content)
                    .font(.body)
                    .textSelection(.enabled)
            }
            .padding()
        }
        .navigationTitle("报告详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var typeLabel: String {
        switch report.type {
        case "assessment": return "测评报告"
        case "mockInterview": return "面试报告"
        default: return report.type
        }
    }

    private var typeIcon: String {
        switch report.type {
        case "assessment": return "checkmark.shield"
        case "mockInterview": return "person.wave.2"
        default: return "doc.text"
        }
    }

    private var typeColor: Color {
        switch report.type {
        case "assessment": return .blue
        case "mockInterview": return .purple
        default: return .gray
        }
    }

}

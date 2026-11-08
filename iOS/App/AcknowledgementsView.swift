import SwiftUI

/// Credits for the code and data Vikey is built from (Settings › Ghi nhận).
struct AcknowledgementsView: View {
    var body: some View {
        List {
            section("Mã nguồn", Acknowledgements.code.filter { $0.name != "Sparkle" })   // Sparkle is the Mac updater
            section("Dữ liệu từ ngữ", Acknowledgements.wordData,
                    note: "Vikey tự chuyển Anh/Việt nhờ thống kê từ ngữ đếm từ các nguồn này. Trong ứng dụng chỉ có số liệu thống kê, không có văn bản nào.")
            section("Dữ liệu kiểm thử", Acknowledgements.testData,
                    note: "Chỉ dùng để đo và cải thiện độ chính xác, không có trong ứng dụng.")
        }
        .navigationTitle("Ghi nhận")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func section(_ title: LocalizedStringKey, _ items: [Credit], note: LocalizedStringKey? = nil) -> some View {
        Section {
            ForEach(items) { c in
                Link(destination: URL(string: c.url)!) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(c.name).foregroundStyle(Color(.label))
                            Text("\(c.by) · \(c.license)").font(.caption).foregroundStyle(Color(.secondaryLabel))
                        }
                        Spacer()
                        Image(systemName: "arrow.up.right").font(.caption).foregroundStyle(Color(.tertiaryLabel))
                    }
                }
            }
        } header: { Text(title) } footer: { if let note { Text(note) } }
    }
}

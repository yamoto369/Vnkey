import Foundation

/// Debug log of what each key press costs (Vikey app › Gỡ lỗi). No typed text is ever written: only
/// timings, counts and key kinds (letter, space, delete…). Lives in the shared app group so the app
/// can send it; the keyboard can only write there with Full Access.
enum KBLog {
    static var url: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: VKSettings.group)?.appendingPathComponent("keyboard-log.txt")
    }
    private static var buffer: [String] = []
    private static let start = Date()

    static func add(_ line: String) {
        let t = String(format: "%8.3f", Date().timeIntervalSince(start))
        buffer.append("\(t) \(line)")
        if buffer.count >= 20 { flush() }
    }

    private static let queue = DispatchQueue(label: "vikey.kblog", qos: .utility)

    /// Writes on a background queue: file work on the main thread would itself cause lag.
    static func flush() {
        guard !buffer.isEmpty, let url else { buffer.removeAll(); return }
        let text = buffer.joined(separator: "\n") + "\n"
        buffer.removeAll()
        queue.async {
            if let h = try? FileHandle(forWritingTo: url) {
                h.seekToEndOfFile(); h.write(Data(text.utf8)); try? h.close()
            } else {
                try? Data(text.utf8).write(to: url)
            }
            // keep the last ~200 KB
            if let size = (try? FileManager.default.attributesOfItem(atPath: url.path)[.size] as? Int) ?? nil, size > 400_000,
               let d = try? Data(contentsOf: url) {
                try? d.suffix(200_000).write(to: url)
            }
        }
    }

    static func read() -> String { url.flatMap { try? String(contentsOf: $0, encoding: .utf8) } ?? "" }
    static func clear() { if let url { try? FileManager.default.removeItem(at: url) } }
}

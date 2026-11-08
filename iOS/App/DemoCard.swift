import SwiftUI
import UIKit

/// The website's demo: a normal input method vs Vikey, typed key by key. The frames come from
/// the real engine (Tools: gen_demo), so what it shows is what the keyboard does.
struct DemoCard: View {
    struct Scene: Decodable { let start: Int; let frames: [[Frame]] }
    enum Frame: Decodable {
        case s(String), i(Int)
        init(from d: Decoder) throws {
            let c = try d.singleValueContainer()
            if let v = try? c.decode(Int.self) { self = .i(v) } else { self = .s(try c.decode(String.self)) }
        }
        var string: String { if case .s(let v) = self { return v }; return "" }
        var int: Int { if case .i(let v) = self { return v }; return 0 }
    }

    @State private var scenes: [Scene] = []
    @State private var plain = ""
    @State private var vikey = ""
    @State private var vietnamese = true

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Bộ gõ thông thường").font(.caption.weight(.semibold)).foregroundStyle(.secondary).textCase(.uppercase)
                Spacer()
                Text(vietnamese ? "VI" : "EN")
                    .font(.caption.weight(.bold)).foregroundStyle(.white)
                    .padding(.horizontal, 7).padding(.vertical, 3)
                    .background(vietnamese ? Color.brand : Color.gray, in: RoundedRectangle(cornerRadius: 5))
            }
            Text(plain.isEmpty ? AttributedString(" ") : Self.marked(plain, against: vikey, good: false)).font(.title3).foregroundStyle(.secondary).lineLimit(1)
            HStack(spacing: 5) {   // the wordmark, like the website's demo
                Image("Logo").resizable().frame(width: 15, height: 15).clipShape(RoundedRectangle(cornerRadius: 3.5, style: .continuous))
                Wordmark(size: 13)
            }
            Text(vikey.isEmpty ? AttributedString(" ") : Self.marked(vikey, against: plain, good: true)).font(.title2.weight(.semibold)).lineLimit(1)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.background, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: Color.brand.opacity(0.18), radius: 20, y: 8)
        .task { await play() }
    }

    /// Like the website: words where a normal keyboard and Vikey disagree are marked,
    /// red with a dotted underline on the normal side, brand-tinted on the Vikey side.
    static func marked(_ a: String, against b: String, good: Bool) -> AttributedString {
        let wa = a.split(separator: " ", omittingEmptySubsequences: false), wb = b.split(separator: " ", omittingEmptySubsequences: false)
        var out = AttributedString()
        for (i, w) in wa.enumerated() {
            if i > 0 { out += AttributedString(" ") }
            var part = AttributedString(String(w))
            if !w.isEmpty, i >= wb.count || wb[i] != w {
                if good {
                    part.foregroundColor = .brand
                    part.backgroundColor = Color.brand.opacity(0.14)
                } else {
                    part.foregroundColor = Color(red: 0.82, green: 0.26, blue: 0.26)
                    part.underlineStyle = Text.LineStyle(pattern: .dot, color: Color(red: 0.82, green: 0.26, blue: 0.26))
                }
            }
            out += part
        }
        return out
    }

    private func play() async {
        guard let url = Bundle.main.url(forResource: "demo", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let s = try? JSONDecoder().decode([Scene].self, from: data) else { return }
        scenes = s.shuffled()
        while !Task.isCancelled {
            for scene in scenes {
                plain = ""; vikey = ""; vietnamese = scene.start == 1
                try? await Task.sleep(nanoseconds: 500_000_000)
                for f in scene.frames where f.count >= 4 {
                    vikey = f[1].string; plain = f[3].string; vietnamese = f[2].int == 1
                    try? await Task.sleep(nanoseconds: UInt64((f[0].string == " " ? 0.16 : 0.07) * 1e9))
                }
                try? await Task.sleep(nanoseconds: 1_800_000_000)
            }
        }
    }
}


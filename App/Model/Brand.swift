import SwiftUI
#if canImport(UIKit)
import UIKit
#else
import AppKit
#endif

/// The brand (design/BRAND.md): one accent color and the wordmark, shared by the Mac and iOS apps.
extension Color {
    /// Brand indigo: #4F46E5, #8C87FF in dark mode.
    static let brand: Color = {
        #if canImport(UIKit)
        return Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 0.549, green: 0.529, blue: 1, alpha: 1) : UIColor(red: 0.310, green: 0.275, blue: 0.898, alpha: 1) })
        #else
        return Color(NSColor(name: nil) { $0.bestMatch(from: [.darkAqua, .vibrantDark]) != nil ? NSColor(red: 0.549, green: 0.529, blue: 1, alpha: 1) : NSColor(red: 0.310, green: 0.275, blue: 0.898, alpha: 1) })
        #endif
    }()
}

/// "Vi" in the text color, "key" in brand indigo, bold.
struct Wordmark: View {
    var size: CGFloat = 17
    var body: some View {
        (Text("Vi").foregroundColor(.primary) + Text("key").foregroundColor(.brand))
            .font(.system(size: size, weight: .bold))
            .accessibilityLabel("Vikey")
    }
}

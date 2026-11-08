import UIKit

/// The proofread preview, over the whole keyboard (Tony): the text with each change shown (what goes struck
/// through, what comes in the brand colour), Cancel and Apply where the bottom row of keys was.
/// Nothing changes in the app until Apply (Tony: preview before replacing).
final class ProofreadPanel: UIView {
    var onApply: (() -> Void)?
    var onCancel: (() -> Void)?
    var dark = false { didSet { restyle() } }
    private let title = UILabel()
    private let text = UITextView()
    private let cancel = UIButton(type: .system)
    private let apply = UIButton(type: .system)
    private var original = "", corrected = ""

    override init(frame: CGRect) {
        super.init(frame: frame)
        title.font = .systemFont(ofSize: 13, weight: .semibold)
        title.text = "✨ " + String(localized: "Bản đã sửa").uppercased()
        text.isEditable = false; text.isSelectable = false
        text.backgroundColor = .clear
        text.textContainerInset = .zero; text.textContainer.lineFragmentPadding = 0
        text.showsVerticalScrollIndicator = true
        cancel.setTitle(String(localized: "Huỷ"), for: .normal)
        apply.setTitle(String(localized: "Áp dụng"), for: .normal)
        for b in [cancel, apply] { b.titleLabel?.font = .systemFont(ofSize: 17, weight: .semibold); b.layer.cornerRadius = 8.5; b.layer.cornerCurve = .continuous }
        cancel.accessibilityIdentifier = "proofread.cancel"; apply.accessibilityIdentifier = "proofread.apply"
        cancel.addTarget(self, action: #selector(tapCancel), for: .touchUpInside)
        apply.addTarget(self, action: #selector(tapApply), for: .touchUpInside)
        [title, text, cancel, apply].forEach(addSubview)
        restyle()
    }
    required init?(coder: NSCoder) { fatalError() }

    func show(original: String, corrected: String) {
        self.original = original; self.corrected = corrected
        restyle()
        text.setContentOffset(.zero, animated: false)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        // the buttons sit where the bottom row of keys is: the same side margin, gap, height and bottom inset
        let W = bounds.width, side: CGFloat = 6.67, gap: CGFloat = 6, M: CGFloat = 16
        let bh: CGFloat = KeyboardView.tallKeys ? 45 : 43, bottom: CGFloat = KeyboardView.tallKeys ? 5.67 : 3
        let by = bounds.height - bh - bottom
        title.frame = CGRect(x: M, y: 14, width: W - 2 * M, height: 18)
        text.frame = CGRect(x: M, y: 40, width: W - 2 * M, height: max(0, by - 52))
        let bw = (W - 2 * side - gap) / 2
        cancel.frame = CGRect(x: side, y: by, width: bw, height: bh)
        apply.frame = CGRect(x: side + bw + gap, y: by, width: bw, height: bh)
    }

    private func restyle() {
        let fg: UIColor = dark ? .white : .black
        let brand = dark ? UIColor(red: 0.62, green: 0.60, blue: 1, alpha: 1) : UIColor(red: 0.31, green: 0.27, blue: 0.90, alpha: 1)
        title.textColor = dark ? UIColor(white: 1, alpha: 0.6) : UIColor(white: 0, alpha: 0.5)
        cancel.backgroundColor = dark ? UIColor(white: 1, alpha: 0.165) : UIColor(red: 0.992, green: 1, blue: 1, alpha: 0.99)
        cancel.setTitleColor(fg, for: .normal)
        apply.backgroundColor = brand
        apply.setTitleColor(.white, for: .normal)
        text.attributedText = Self.diff(corrected, against: original, fg: fg, gone: fg.withAlphaComponent(0.45), mark: brand)
    }

    /// Word-level diff: unchanged words plain, removed ones struck through, new ones in the brand colour.
    static func diff(_ new: String, against old: String, fg: UIColor, gone: UIColor, mark: UIColor) -> NSAttributedString {
        let a = tokens(old), b = tokens(new)
        let font = UIFont.systemFont(ofSize: 18), bold = UIFont.systemFont(ofSize: 18, weight: .semibold)
        let out = NSMutableAttributedString()
        func add(_ w: String, _ attrs: [NSAttributedString.Key: Any]) {
            if w == "\n" { out.append(NSAttributedString(string: "\n", attributes: [.font: font])); return }
            if out.length > 0, !out.string.hasSuffix("\n") { out.append(NSAttributedString(string: " ", attributes: [.font: font, .foregroundColor: fg])) }
            out.append(NSAttributedString(string: w, attributes: attrs))
        }
        guard a.count * b.count < 90_000 else { add(new, [.font: font, .foregroundColor: fg]); return out }
        var dp = Array(repeating: Array(repeating: 0, count: b.count + 1), count: a.count + 1)
        for i in stride(from: a.count - 1, through: 0, by: -1) {
            for j in stride(from: b.count - 1, through: 0, by: -1) {
                dp[i][j] = a[i] == b[j] ? dp[i + 1][j + 1] + 1 : max(dp[i + 1][j], dp[i][j + 1])
            }
        }
        var i = 0, j = 0
        while i < a.count || j < b.count {
            if i < a.count, j < b.count, a[i] == b[j] { add(b[j], [.font: font, .foregroundColor: fg]); i += 1; j += 1 }
            else if i < a.count, j >= b.count || dp[i + 1][j] >= dp[i][j + 1] {
                if a[i] != "\n" { add(a[i], [.font: font, .foregroundColor: gone, .strikethroughStyle: NSUnderlineStyle.single.rawValue, .strikethroughColor: gone]) }
                i += 1
            } else {
                add(b[j], [.font: bold, .foregroundColor: mark])
                j += 1
            }
        }
        return out
    }

    /// Words, with each line break as its own token (so the preview keeps the lines).
    static func tokens(_ s: String) -> [String] {
        var out: [String] = []
        for (n, line) in s.components(separatedBy: "\n").enumerated() {
            if n > 0 { out.append("\n") }
            out += line.split(separator: " ").map(String.init)
        }
        return out
    }

    @objc private func tapCancel() { onCancel?() }
    @objc private func tapApply() { onApply?() }
}

import XCTest
import UIKit

/// The emoji keyboard's flow (open, search, insert, back to letters) and pictures of it at the
/// widths of the iPhones Vikey runs on, for a visual check.
final class EmojiKeyboardTests: XCTestCase {
    private let sender = KeyboardView()

    private func make(width: CGFloat) -> (KeyboardViewController, CursorProxy) {
        let vc = KeyboardViewController()
        let p = CursorProxy()
        vc.proxy = p
        vc.loadViewIfNeeded()
        vc.view.frame = CGRect(x: 0, y: 0, width: width, height: KeyboardView.keyboardHeight(compactHeight: false))
        vc.view.layoutIfNeeded()
        vc.viewWillAppear(false)
        vc.setLanguageForTests(1)
        return (vc, p)
    }
    private var emoji: (KeyboardViewController) -> EmojiKeyboardView? = { vc in vc.view.subviews.compactMap { $0 as? EmojiKeyboardView }.first }
    private var keys: (KeyboardViewController) -> KeyboardView = { vc in vc.view.subviews.compactMap { $0 as? KeyboardView }.first! }

    private func render(_ vc: KeyboardViewController, _ name: String) {
        var h = KeyboardView.keyboardHeight(compactHeight: false)
        if let ev = emoji(vc), !ev.isHidden, !ev.searching { h += EmojiKeyboardView.extraHeight }
        vc.view.frame.size.height = h
        vc.view.setNeedsLayout(); vc.view.layoutIfNeeded()
        for v in vc.view.subviews { v.setNeedsLayout(); v.layoutIfNeeded() }
        if let ev = emoji(vc), !ev.isHidden, !ev.searching { ev.frame = vc.view.bounds; ev.layoutIfNeeded() }
        RunLoop.main.run(until: Date().addingTimeInterval(0.1))
        let img = UIGraphicsImageRenderer(bounds: vc.view.bounds).image { ctx in
            UIColor(red: 0.82, green: 0.83, blue: 0.86, alpha: 1).setFill(); ctx.fill(vc.view.bounds)
            vc.view.layer.render(in: ctx.cgContext)
        }
        try? FileManager.default.createDirectory(atPath: "/tmp/claude-501/emoji", withIntermediateDirectories: true)
        try? img.pngData()?.write(to: URL(fileURLWithPath: "/tmp/claude-501/emoji/\(name).png"))
    }

    func testOpenInsertAndBack() {
        let (vc, p) = make(width: 440)
        XCTAssertFalse(EmojiIndex.shared.all.isEmpty)
        vc.keyboard(sender, didTap: .emoji)
        let ev = try! XCTUnwrap(emoji(vc))
        XCTAssertFalse(ev.isHidden); XCTAssertTrue(keys(vc).isHidden)
        render(vc, "browse_440")
        vc.emojiKeyboard(ev, insert: "🐎")
        XCTAssertEqual(p.text, "🐎")
        vc.emojiKeyboardDelete(ev)
        XCTAssertEqual(p.text, "")
        vc.emojiKeyboardClose(ev)
        XCTAssertTrue(ev.isHidden); XCTAssertFalse(keys(vc).isHidden)
        for ch in "tooi" { vc.keyboard(sender, didTap: .char(String(ch))) }
        XCTAssertEqual(p.text.lowercased(), "tôi")
    }

    func testSearchTypesIntoTheQueryNotTheApp() {
        let (vc, p) = make(width: 440)
        vc.keyboard(sender, didTap: .emoji)
        let ev = try! XCTUnwrap(emoji(vc))
        vc.emojiKeyboardStartSearch(ev)
        XCTAssertTrue(ev.searching); XCTAssertFalse(keys(vc).isHidden)
        for ch in "horse" { vc.keyboard(sender, didTap: .char(String(ch))) }
        XCTAssertEqual(p.text, "")                       // nothing reached the app
        render(vc, "search_440")
        vc.emojiKeyboard(ev, insert: "🐎")
        XCTAssertEqual(p.text, "🐎")
        vc.keyboard(sender, didTap: .backspace)          // edits the query
        XCTAssertEqual(p.text, "🐎")
        vc.keyboard(sender, didTap: .returnKey)          // back to the emoji grid
        XCTAssertFalse(ev.searching); XCTAssertTrue(keys(vc).isHidden)
        XCTAssertNil(KeyboardView.searchTop)
    }

    func testReopeningTheKeyboardStartsOnTheLetters() {
        let (vc, _) = make(width: 402)
        vc.keyboard(sender, didTap: .emoji)
        let ev = try! XCTUnwrap(emoji(vc))
        vc.emojiKeyboardStartSearch(ev)
        vc.viewWillDisappear(false)
        vc.viewWillAppear(false)                        // another text field
        XCTAssertTrue(ev.isHidden); XCTAssertFalse(keys(vc).isHidden)
        XCTAssertNil(KeyboardView.searchTop, "letter keys would stay pushed down")
    }

    func testANewKeyboardAfterAnEmojiSearchHasNoGap() {
        let (vc, _) = make(width: 440)
        vc.keyboard(sender, didTap: .emoji)
        vc.emojiKeyboardStartSearch(emoji(vc)!)
        // iOS throws the keyboard away and makes a new one in the same process (static state survives)
        let (vc2, _) = make(width: 440)
        _ = vc
        XCTAssertNil(KeyboardView.searchTop)
        XCTAssertEqual(vc2.view.frame.height, KeyboardView.keyboardHeight(compactHeight: false))
    }

    func testSearchInAnEmailFieldUsesPlainKeysAndADoneKey() {
        let (vc, p) = make(width: 402)
        p.keyboardType = .emailAddress
        vc.textDidChange(nil)
        XCTAssertEqual(keys(vc).fieldType, .emailAddress)
        vc.keyboard(sender, didTap: .emoji)
        vc.emojiKeyboardStartSearch(emoji(vc)!)
        XCTAssertEqual(keys(vc).fieldType, .default)
        XCTAssertTrue(keys(vc).returnIsDone)
        vc.emojiKeyboard(emoji(vc)!, insert: "🐎"); vc.textDidChange(nil)   // inserting must not reset the search keys
        XCTAssertEqual(keys(vc).fieldType, .default)
        render(vc, "search_email_field")
        vc.keyboard(sender, didTap: .returnKey)            // done: back to the grid
        XCTAssertFalse(keys(vc).returnIsDone)
        XCTAssertEqual(keys(vc).fieldType, .emailAddress)
    }

    func testEmptySearchShowsFrequentlyUsed() {
        let (vc, _) = make(width: 402)
        vc.keyboard(sender, didTap: .emoji)
        let ev = emoji(vc)!
        vc.emojiKeyboardStartSearch(ev)
        XCTAssertFalse(ev.resultsForTests.isEmpty, "an empty query shows the frequently used emoji")
        render(vc, "search_empty")
    }

    func testRenderFillOptions() {
        for f in [0.74, 0.84, 0.92] as [CGFloat] {
            EmojiKeyboardView.fill = f
            let (vc, _) = make(width: 440)
            vc.keyboard(sender, didTap: .emoji)
            render(vc, "fill_\(Int(f * 100))")
        }
        EmojiKeyboardView.fill = 0.84
    }

    func testRenderWidths() {
        for w in [375, 393, 402, 430, 440] as [CGFloat] {
            let (vc, _) = make(width: w)
            vc.keyboard(sender, didTap: .emoji)
            render(vc, "browse_\(Int(w))")
            vc.emojiKeyboardClose(emoji(vc)!)
            render(vc, "letters_\(Int(w))")
        }
    }
}

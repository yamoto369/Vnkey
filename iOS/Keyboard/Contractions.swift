import Foundation

/// English contractions typed without the apostrophe ("dont" → "don't", "Ill" → "I'll"), fixed at the
/// end of the word like the system keyboard. Only words that are not English words themselves
/// ("were", "well", "its" stay; "lets" only with a capital), and the caller skips valid Vietnamese ("im").
enum Contractions {
    static let table: [String: String] = [
        "dont": "don't", "cant": "can't", "isnt": "isn't", "arent": "aren't", "wasnt": "wasn't", "werent": "weren't",
        "doesnt": "doesn't", "didnt": "didn't", "hasnt": "hasn't", "havent": "haven't", "hadnt": "hadn't",
        "couldnt": "couldn't", "wouldnt": "wouldn't", "shouldnt": "shouldn't", "mustnt": "mustn't", "neednt": "needn't",
        "aint": "ain't", "wouldve": "would've", "couldve": "could've", "shouldve": "should've", "mightve": "might've",
        "im": "I'm", "ive": "I've", "youre": "you're", "youve": "you've", "youll": "you'll", "youd": "you'd",
        "theyre": "they're", "theyve": "they've", "theyll": "they'll", "theyd": "they'd",
        "weve": "we've", "itll": "it'll", "thatll": "that'll", "shes": "she's", "thats": "that's", "whats": "what's",
        "wheres": "where's", "theres": "there's", "heres": "here's", "hows": "how's", "whos": "who's", "whove": "who've",
        "lets": "let's", "yall": "y'all", "i": "I", "wont": "won't",
    ]
    /// Real English words that only become contractions with a capital I ("Ill" → "I'll", "ill" stays).
    /// "Lets" (the start of a sentence) is "Let's" like the system keyboard; "she lets" stays (Tony).
    static let capitalOnly: [String: String] = ["ill": "I'll", "id": "I'd", "lets": "Let's"]
    static let notFixed: Set<String> = ["lets"]   // "lets" is a word too: leave it

    /// The fixed word, or nil. `typed` is the word as it shows; keeps its capitals (Dont → Don't, DONT → DON'T).
    static func fix(_ typed: String) -> String? {
        let low = typed.lowercased()
        var out: String
        if let c = capitalOnly[low] {
            guard typed.first?.isUppercase == true else { return nil }
            out = c
        } else if let c = table[low], !notFixed.contains(low) {
            out = c
        } else { return nil }
        if typed.count > 1 && typed == typed.uppercased() { return out.uppercased() }
        if typed.first?.isUppercase == true, let f = out.first { out = f.uppercased() + out.dropFirst() }
        return out == typed ? nil : out
    }
}

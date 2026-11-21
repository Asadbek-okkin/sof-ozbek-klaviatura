import Foundation

/// Kirill va eski lotin yozuvini yangi alifboga o'girish (Android'dagi Ogir.java bilan bir xil qoidalar).
enum Ogir {
    static let tutuq: Character = "\u{02BC}"
    private static let tutuqlar: Set<Character> = ["'", "\u{02BB}", "\u{02BC}", "\u{2018}", "\u{2019}", "`", "\u{00B4}"]

    static func yangiga(_ matn: String) -> String {
        var s = matn
        if s.unicodeScalars.contains(where: { $0.value >= 0x0400 && $0.value <= 0x04FF }) { s = kirilldan(s) }
        return eskiLotindan(s)
    }

    static func eskiLotindan(_ s: String) -> String {
        let a = Array(s)
        var b = ""
        var i = 0
        while i < a.count {
            let c = a[i]
            let k: Character? = i + 1 < a.count ? a[i + 1] : nil
            let k2: Character? = i + 2 < a.count ? a[i + 2] : nil
            if let k = k, tutuqlar.contains(k) {
                if (c == "o" || c == "O"), let k2 = k2, k2.isLetter {
                    b.append(c == "o" ? "ö" : "Ö"); i += 2; continue
                }
                if c == "g" || c == "G" { b.append(c == "g" ? "ğ" : "Ğ"); i += 2; continue }
            }
            if (c == "s" || c == "S"), k == "h" || k == "H" { b.append(c == "s" ? "ş" : "Ş"); i += 2; continue }
            if (c == "c" || c == "C"), k == "h" || k == "H" { b.append(c == "c" ? "ç" : "Ç"); i += 2; continue }
            if tutuqlar.contains(c) {
                let oldHarf = i > 0 && a[i - 1].isLetter
                let keyinHarf = k?.isLetter ?? false
                b.append(oldHarf && keyinHarf ? tutuq : c); i += 1; continue
            }
            b.append(c)
            i += 1
        }
        return b
    }

    private static let jadval: [Character: String] = [
        "а": "a", "б": "b", "в": "v", "г": "g", "ғ": "ğ", "д": "d", "ё": "yo", "ж": "j", "з": "z",
        "и": "i", "й": "y", "к": "k", "қ": "q", "л": "l", "м": "m", "н": "n", "о": "o", "ў": "ö",
        "п": "p", "р": "r", "с": "s", "т": "t", "у": "u", "ф": "f", "х": "x", "ҳ": "h", "ч": "ç",
        "ш": "ş", "щ": "şç", "ь": "", "э": "e", "ю": "yu", "я": "ya", "ы": "i",
    ]
    private static let unli: Set<Character> = Set("аеёиоуўэюяАЕЁИОУЎЭЮЯ")

    static func kirilldan(_ s: String) -> String {
        let a = Array(s)
        var b = ""
        for (i, ch) in a.enumerated() {
            let kichik = Character(ch.lowercased())
            var katta = ch.isUppercase
            let old: Character = i > 0 ? a[i - 1] : " "
            let sozBoshi = !old.isLetter
            var r: String
            switch kichik {
            case "е": r = (sozBoshi || unli.contains(old) || "ъьЪЬ".contains(old)) ? "ye" : "e"
            case "ц": r = sozBoshi ? "s" : "ts"
            case "ъ": r = String(tutuq); katta = false
            default:
                if let x = jadval[kichik] { r = x } else { r = String(ch); katta = false }
            }
            if katta && !r.isEmpty {
                let keyingiKatta = i + 1 < a.count && a[i + 1].isUppercase
                r = (r.count == 1 || keyingiKatta) ? r.uppercased() : r.prefix(1).uppercased() + r.dropFirst()
            }
            b += r
        }
        return b
    }
}

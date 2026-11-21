import UIKit

/// Tugma turi va vazifasi.
enum Vazifa: Equatable {
    case matn(String)
    case shift, ochir, belgi, harf, belgi2, til, enter, boshliq
}

enum TugmaTuri { case harf, funk, enter, boshliq }

final class Tugma {
    let label: String
    let vazifa: Vazifa
    let en: CGFloat
    let tur: TugmaTuri
    let alt: [String]
    var ramka: CGRect = .zero

    init(_ label: String, _ vazifa: Vazifa, en: CGFloat = 1, tur: TugmaTuri = .harf, alt: [String] = []) {
        self.label = label; self.vazifa = vazifa; self.en = en; self.tur = tur; self.alt = alt
    }
}

struct Qator {
    let bal: CGFloat
    let tugmalar: [Tugma]
    var chet: CGFloat = 0
}

/// Joylashuv Android versiyasi bilan aynan bir xil:
///   q w e r t y u i o ö p / a s d f g h j k l ş / ⇧ z x c ç v b n m ğ ⌫
enum Tartib {
    private static func h(_ s: String, en: CGFloat = 1, alt: [String] = []) -> Tugma { Tugma(s, .matn(s), en: en, alt: alt) }
    private static func f(_ s: String, _ v: Vazifa, en: CGFloat) -> Tugma { Tugma(s, v, en: en, tur: .funk) }

    private static func pastki(globus: Bool, belgi: String, belgiV: Vazifa) -> Qator {
        var t: [Tugma] = [f(belgi, belgiV, en: 1.5)]
        if globus { t.append(f("🌐", .til, en: 1)) }
        t.append(Tugma("Özbekça", .boshliq, en: globus ? 4.5 : 5.5, tur: .boshliq))
        t.append(h(".", alt: [",", "?", "!", "ʼ", "'", "\"", ":", ";", "-", "…"]))
        t.append(Tugma("↵", .enter, en: 1.5, tur: .enter))
        return Qator(bal: 54, tugmalar: t)
    }

    static func harflar(globus: Bool) -> [Qator] {
        [
            Qator(bal: 46, tugmalar: "1 2 3 4 5 6 7 8 9 0".split(separator: " ").map { h(String($0)) }),
            Qator(bal: 56, tugmalar: "q w e r t y u i o ö p".split(separator: " ").map { h(String($0)) }),
            Qator(bal: 56, tugmalar: "a s d f g h j k l ş".split(separator: " ").map { h(String($0)) }, chet: 0.5),
            Qator(bal: 56, tugmalar: [f("⇧", .shift, en: 1.2)]
                  + "z x c ç v b n m ğ".split(separator: " ").map { h(String($0)) }
                  + [f("⌫", .ochir, en: 1.8)]),
            pastki(globus: globus, belgi: "123", belgiV: .belgi),
        ]
    }

    static func belgi1(globus: Bool) -> [Qator] {
        let r: CGFloat = 0.9375
        return [
            Qator(bal: 67, tugmalar: "1 2 3 4 5 6 7 8 9 0".split(separator: " ").map { h(String($0)) }),
            Qator(bal: 67, tugmalar: ["@", "#", "$", "_", "&", "-", "+", "(", ")", "/"].map { h($0, alt: $0 == "$" ? ["so'm", "€", "£", "₽"] : []) }),
            Qator(bal: 67, tugmalar: [f("=\\<", .belgi2, en: 1.25)]
                  + ["*", "\"", "ʼ", "'", ":", ";", "!", "?"].map { h($0, en: r) }
                  + [f("⌫", .ochir, en: 1.25)]),
            pastki(globus: globus, belgi: "ABC", belgiV: .harf),
        ]
    }

    static func belgi2(globus: Bool) -> [Qator] {
        let r: CGFloat = 0.9375
        return [
            Qator(bal: 67, tugmalar: ["~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"].map { h($0) }),
            Qator(bal: 67, tugmalar: ["£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"].map { h($0) }),
            Qator(bal: 67, tugmalar: [f("123", .belgi, en: 1.25)]
                  + ["%", "©", "®", "™", "✓", "[", "]", "№"].map { h($0, en: r) }
                  + [f("⌫", .ochir, en: 1.25)]),
            pastki(globus: globus, belgi: "ABC", belgiV: .harf),
        ]
    }
}

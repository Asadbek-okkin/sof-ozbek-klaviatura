import Foundation

/// Yangi alifbodagi lug'at (250 000 so'z). Klaviatura kengaytmasida xotira cheklangan (~50 MB),
/// shuning uchun so'zlar alohida String emas, bitta bayt massivida saqlanadi.
final class Lugat {
    static let bitta = Lugat()

    private var bayt: [UInt8] = []
    private var bosh: [Int32] = []
    private var uzun: [UInt8] = []
    private var chastota: [Int32] = []
    private(set) var tayyor = false
    private var organgan: [String: Int] = UserDefaults.standard.dictionary(forKey: "organgan") as? [String: Int] ?? [:]
    private let harflar: [Character] = Array("abdefghijklmnopqrstuvxyzöğşçcw\u{02BC}")

    func yukla() {
        if tayyor { return }
        DispatchQueue.global(qos: .userInitiated).async {
            guard let url = Bundle(for: Lugat.self).url(forResource: "lugat", withExtension: "txt"),
                  let d = try? Data(contentsOf: url, options: .mappedIfSafe) else { return }
            var bayt = [UInt8](d)
            var bosh: [Int32] = []; bosh.reserveCapacity(260_000)
            var uzun: [UInt8] = []; uzun.reserveCapacity(260_000)
            var chast: [Int32] = []; chast.reserveCapacity(260_000)
            var i = 0
            let n = bayt.count
            while i < n {
                let s = i
                while i < n && bayt[i] != 9 && bayt[i] != 10 { i += 1 }
                let tab = i
                var f: Int32 = 0
                if i < n && bayt[i] == 9 {
                    i += 1
                    while i < n && bayt[i] != 10 { f = f &* 10 &+ Int32(bayt[i] &- 48); i += 1 }
                }
                i += 1
                if tab > s && tab - s < 255 {
                    bosh.append(Int32(s)); uzun.append(UInt8(tab - s)); chast.append(f)
                }
            }
            bayt.append(0)
            DispatchQueue.main.async {
                self.bayt = bayt; self.bosh = bosh; self.uzun = uzun; self.chastota = chast
                self.tayyor = true
            }
        }
    }

    // MARK: - qidiruv

    private func taqqos(_ i: Int, _ q: [UInt8]) -> Int {
        let s = Int(bosh[i]), l = Int(uzun[i])
        let m = min(l, q.count)
        for j in 0..<m {
            let a = bayt[s + j], b = q[j]
            if a != b { return a < b ? -1 : 1 }
        }
        return l == q.count ? 0 : (l < q.count ? -1 : 1)
    }

    private func boshlanadimi(_ i: Int, _ q: [UInt8]) -> Bool {
        let s = Int(bosh[i]), l = Int(uzun[i])
        if l < q.count { return false }
        for j in 0..<q.count where bayt[s + j] != q[j] { return false }
        return true
    }

    /// q dan katta yoki teng birinchi so'z o'rni
    private func pastki(_ q: [UInt8]) -> Int {
        var lo = 0, hi = bosh.count
        while lo < hi {
            let mid = (lo + hi) / 2
            if taqqos(mid, q) < 0 { lo = mid + 1 } else { hi = mid }
        }
        return lo
    }

    private func soz(_ i: Int) -> String {
        let s = Int(bosh[i])
        return String(decoding: bayt[s..<(s + Int(uzun[i]))], as: UTF8.self)
    }

    private func chastotasi(_ w: String) -> Int? {
        guard tayyor else { return nil }
        let q = Array(w.utf8)
        let i = pastki(q)
        return i < bosh.count && taqqos(i, q) == 0 ? Int(chastota[i]) : nil
    }

    private func ogirlik(_ w: String, _ f: Int) -> Int { f + (organgan[w] ?? 0) * 5000 }

    func bormi(_ w: String) -> Bool {
        let k = w.lowercased()
        if (organgan[k] ?? 0) >= 2 { return true }
        return chastotasi(k) != nil
    }

    func takliflar(_ boshi: String, soni: Int) -> [String] {
        let p = boshi.lowercased()
        guard tayyor, !p.isEmpty else { return [] }
        let q = Array(p.utf8)
        var eng: [(String, Int)] = []
        var i = pastki(q), sanoq = 0
        while i < bosh.count && sanoq < 60000 && boshlanadimi(i, q) {
            let o = ogirlik("", Int(chastota[i]))
            if eng.count < soni || o > eng.last!.1 {
                let w = soz(i)
                eng.append((w, ogirlik(w, Int(chastota[i]))))
                eng.sort { $0.1 > $1.1 }
                if eng.count > soni { eng.removeLast() }
            }
            i += 1; sanoq += 1
        }
        for (w, n) in organgan where n >= 2 && w.hasPrefix(p) && !eng.contains(where: { $0.0 == w }) {
            eng.append((w, ogirlik(w, chastotasi(w) ?? 0)))
        }
        eng.sort { $0.1 > $1.1 }
        return eng.prefix(soni).map { $0.0 }
    }

    /// Xato so'z uchun 1 ta tahrir uzoqlikdagi eng ko'p ishlatiladigan to'g'ri so'z.
    func tuzat(_ soz: String) -> String? {
        let w = soz.lowercased()
        guard tayyor, w.count >= 2, !bormi(w) else { return nil }
        let a = Array(w)
        var nomzod = Set<String>()
        for i in 0...a.count {
            let chap = String(a[0..<i]), ong = Array(a[i...])
            if !ong.isEmpty { nomzod.insert(chap + String(ong.dropFirst())) }
            if ong.count > 1 { nomzod.insert(chap + String(ong[1]) + String(ong[0]) + String(ong.dropFirst(2))) }
            for h in harflar {
                if !ong.isEmpty { nomzod.insert(chap + String(h) + String(ong.dropFirst())) }
                nomzod.insert(chap + String(h) + String(ong))
            }
        }
        var eng: String? = nil
        var engO = 0
        for n in nomzod where n != w {
            guard let f = chastotasi(n) else { continue }
            var o = ogirlik(n, f)
            if yaqin(w, n) { o *= 4 }
            if o > engO { engO = o; eng = n }
        }
        return engO >= 20 ? eng : nil
    }

    private func yaqin(_ a: String, _ b: String) -> Bool {
        let x = Array(a), y = Array(b)
        guard x.count == y.count else { return false }
        for (p, q) in zip(x, y) where p != q {
            let juft = String(p) + String(q)
            return ["oö", "sş", "cç", "gğ", "öo", "şs"].contains(juft)
        }
        return false
    }

    func orgat(_ soz: String) {
        let w = soz.lowercased()
        guard w.count >= 2, w.count <= 24, w.allSatisfy({ $0.isLetter || $0 == Ogir.tutuq }) else { return }
        organgan[w] = min((organgan[w] ?? 0) + 1, 1000)
        UserDefaults.standard.set(organgan, forKey: "organgan")
    }
}

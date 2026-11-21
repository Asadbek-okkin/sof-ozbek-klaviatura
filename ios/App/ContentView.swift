import SwiftUI
import UIKit

private let fonRang = Color(red: 0.08, green: 0.09, blue: 0.11)
private let kartaRang = Color(red: 0.14, green: 0.15, blue: 0.17)
private let kok = Color(red: 0.54, green: 0.71, blue: 0.97)
private let yashil = Color(red: 0.20, green: 0.66, blue: 0.33)
private let server = "https://inok.uz/claude-pc/klaviatura"

/// O'rnatish yordamchisi: kirish animatsiyasi, 2 qadam, sinov, fikr va adminga yozish.
struct ContentView: View {
    @Environment(\.scenePhase) private var faza
    @State private var parda = true
    @State private var logoKatta = false
    @State private var korinsin = false
    @State private var yoqilgan = false
    @State private var sinov = ""
    @State private var fikr = ""
    @State private var fikrHolat = ""
    @State private var puls = false

    var body: some View {
        ZStack {
            fonRang.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    sarlavha.kirish(korinsin, 0)
                    Text("Ö Ş Ç Ğ harflari alohida tugmada, sōz takliflari va avtomatik tuzatiş, eski yozuvni yangisiga ōgiriş (o→ö tugmasi). Yozganlaringiz hech qayerga yuborilmaydi.")
                        .font(.subheadline).foregroundColor(.gray).kirish(korinsin, 1)
                    qadam1.kirish(korinsin, 2)
                    qadam2.kirish(korinsin, 3)
                    sinovQismi.kirish(korinsin, 4)
                    fikrQismi.kirish(korinsin, 5)
                    maslahatlar.kirish(korinsin, 6)
                }
                .padding(20)
            }
            if parda { kirishPardasi }
        }
        .onAppear {
            tekshir()
            salom()
            withAnimation(.spring(response: 0.7, dampingFraction: 0.5).delay(0.1)) { logoKatta = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                withAnimation(.easeInOut(duration: 0.5)) { parda = false }
                withAnimation(.easeOut(duration: 0.6).delay(0.2)) { korinsin = true }
            }
            withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true).delay(2.5)) { puls = true }
        }
        .onChange(of: faza) { f in if f == .active { tekshir() } }
    }

    // MARK: - qismlar

    private var kirishPardasi: some View {
        ZStack {
            fonRang.ignoresSafeArea()
            VStack(spacing: 14) {
                logo(120)
                    .scaleEffect(logoKatta ? 1 : 0.15)
                    .rotationEffect(.degrees(logoKatta ? 0 : -200))
                Text("Sof Özbek").font(.system(size: 32, weight: .bold)).foregroundColor(.white)
                    .opacity(logoKatta ? 1 : 0)
                Text("Ö  Ş  Ç  Ğ  —  yangi alifboda yozing").foregroundColor(kok)
                    .opacity(logoKatta ? 1 : 0)
            }
        }
        .transition(.opacity.combined(with: .move(edge: .top)))
    }

    private func logo(_ o: CGFloat) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: o * 0.22)
                .fill(LinearGradient(colors: [Color(red: 0.12, green: 0.53, blue: 0.9), Color(red: 0.08, green: 0.4, blue: 0.75)],
                                     startPoint: .top, endPoint: .bottom))
            Text("Öz").font(.system(size: o * 0.42, weight: .bold)).foregroundColor(.white)
        }
        .frame(width: o, height: o)
    }

    private var sarlavha: some View {
        HStack(spacing: 14) {
            logo(56)
            VStack(alignment: .leading) {
                Text("Sof Özbek klaviatura").font(.title2.bold()).foregroundColor(.white)
                Text("Yangi lotin alifbosi uçun").font(.subheadline).foregroundColor(.gray)
            }
        }
    }

    private var qadam1: some View {
        Button(action: sozlamalar) {
            karta {
                Text("1. Klaviaturani qōşiş").font(.headline).foregroundColor(.white)
                Text(yoqilgan ? "✅ Qōşilgan"
                     : "Bosing → «Klaviaturalar» → «Sof Özbek» ni yoqing.\nYoki: Sozlamalar → Umumiy → Klaviatura → Klaviaturalar → Yangi klaviatura qōşiş")
                    .font(.subheadline).foregroundColor(yoqilgan ? yashil : .gray)
            }
        }
        .scaleEffect(!yoqilgan && puls ? 1.03 : 1)
    }

    private var qadam2: some View {
        karta {
            Text("2. Klaviaturani tanlaş").font(.headline).foregroundColor(.white)
            Text("Istalgan yozuv joyida 🌐 tugmasini bosib turing va «Sof Özbek» ni tanlang.")
                .font(.subheadline).foregroundColor(.gray)
        }
        .scaleEffect(yoqilgan && puls ? 1.03 : 1)
    }

    private var sinovQismi: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("3. Sinab kōring").font(.headline).foregroundColor(.white)
            TextField("Bu yerga yozing...", text: $sinov)
                .padding(14).background(kartaRang).cornerRadius(14).foregroundColor(.white)
        }
    }

    private var fikrQismi: some View {
        karta {
            Text("💬 Fikr yoki taklif").font(.headline).foregroundColor(.white)
            TextField("Fikringiz...", text: $fikr)
                .padding(10).background(fonRang).cornerRadius(10).foregroundColor(.white)
            HStack {
                Button("Yuboriş") { fikrYubor() }.foregroundColor(kok)
                Spacer()
                Button("✈️ Adminga Telegramda yoziş") { admingaYoz() }.foregroundColor(kok)
            }
            if !fikrHolat.isEmpty { Text(fikrHolat).font(.footnote).foregroundColor(yashil) }
        }
    }

    private var maslahatlar: some View {
        Text("""
        Maslahatlar
        • Tepadagi o→ö — eski yozuvdagi (sh, ch, o', g' yoki kirill) matnni yangi alifboga ōgiradi
        • Yozayotganda tepada 3 ta sōz taklifi çiqadi
        • Xato yozsangiz bōşliqda ō'zi tuzatadi; ⌫ bossangiz — yozganingiz qaytadi
        • Nuqtani bosib turing: , ? ! ʼ ' " : ;
        • Bōşliqni çapga-ōngga surib — kursor yuradi
        • Ikki marta bōşliq — nuqta; ⇧ ni ikki marta tez — doimiy katta harf
        """)
        .font(.footnote).foregroundColor(.gray).padding(.top, 8)
    }

    private func karta<V: View>(@ViewBuilder _ ichi: () -> V) -> some View {
        VStack(alignment: .leading, spacing: 6, content: ichi)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16).background(kartaRang).cornerRadius(16)
    }

    // MARK: - amallar

    private func sozlamalar() {
        if let u = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(u) }
    }

    private func tekshir() {
        let royxat = UserDefaults.standard.object(forKey: "AppleKeyboards") as? [String] ?? []
        yoqilgan = royxat.contains { $0.hasPrefix("uz.okkin.sofozbek") }
    }

    private static var qurilma: String {
        if let q = UserDefaults.standard.string(forKey: "q") { return q }
        let q = UUID().uuidString.lowercased()
        UserDefaults.standard.set(q, forKey: "q")
        return q
    }

    private static func asos() -> [String: Any] {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        return ["q": qurilma, "v": "ios-" + v, "a": "iOS " + UIDevice.current.systemVersion + " " + UIDevice.current.model]
    }

    private func post(_ yol: String, _ tana: [String: Any], _ tayyor: @escaping (Bool) -> Void) {
        guard let url = URL(string: server + yol), let d = try? JSONSerialization.data(withJSONObject: tana) else { return }
        var r = URLRequest(url: url)
        r.httpMethod = "POST"
        r.setValue("application/json", forHTTPHeaderField: "Content-Type")
        r.httpBody = d
        URLSession.shared.dataTask(with: r) { _, javob, _ in
            let ok = (javob as? HTTPURLResponse)?.statusCode == 200
            DispatchQueue.main.async { tayyor(ok) }
        }.resume()
    }

    /// Kuniga bir marta: admin statistikasi uchun (anonim).
    private func salom() {
        let oxirgi = UserDefaults.standard.double(forKey: "salom")
        guard Date().timeIntervalSince1970 - oxirgi > 12 * 3600 else { return }
        post("/salom", Self.asos()) { ok in
            if ok { UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: "salom") }
        }
    }

    private func fikrYubor() {
        let m = fikr.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !m.isEmpty else { return }
        var b = Self.asos()
        b["tur"] = "fikr"
        b["matn"] = m
        fikrHolat = "Yuborilmoqda..."
        post("/fikr", b) { ok in
            fikrHolat = ok ? "Rahmat! Fikringiz yuborildi 🙏" : "Yuborib bōlmadi, internetni tekşiring"
            if ok { fikr = "" }
        }
    }

    private func admingaYoz() {
        guard let url = URL(string: server + "/aloqa") else { return }
        URLSession.shared.dataTask(with: url) { d, _, _ in
            var u = "claude_asadbek_bot"
            if let d = d, let o = try? JSONSerialization.jsonObject(with: d) as? [String: Any],
               let t = o["telegram"] as? String, !t.isEmpty { u = t }
            DispatchQueue.main.async {
                if let l = URL(string: "https://t.me/" + u) { UIApplication.shared.open(l) }
            }
        }.resume()
    }
}

private extension View {
    /// Kartalar pastdan ketma-ket suzib keladi.
    func kirish(_ korinsin: Bool, _ i: Int) -> some View {
        self.opacity(korinsin ? 1 : 0)
            .offset(y: korinsin ? 0 : 60)
            .animation(.easeOut(duration: 0.6).delay(Double(i) * 0.08), value: korinsin)
    }
}

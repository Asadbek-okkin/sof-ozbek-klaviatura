import UIKit

/// Sof Özbek klaviaturasi (iPhone). Joylashuv, takliflar, tuzatish va o'girish - Android versiyasi bilan bir xil.
final class KeyboardViewController: UIInputViewController, KlaviaturaTinglovchi {

    private let kv = KlaviaturaView()
    private let satr = UIStackView()
    private var takliflar: [UIButton] = []
    private let ogirTugma = UIButton(type: .system)
    private var balandlik: NSLayoutConstraint?
    private let lugat = Lugat.bitta

    private enum Sahifa { case harf, belgi1, belgi2 }
    private var sahifa = Sahifa.harf
    private var oxirgiShift = Date.distantPast
    private var oxirgiBoshliq = Date.distantPast
    // avtomatik tuzatishni bekor qilish uchun
    private var tuzAsl: String?, tuzYangi = "", tuzAjrat = ""
    private var tuzatilmasin = Set<String>()
    private var tuzatishTaklifi: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        lugat.yukla()
        let ildiz = IldizView(frame: .zero, inputViewStyle: .keyboard)
        ildiz.allowsSelfSizing = true
        inputView = ildiz

        // tepadagi satr: o→ö o'girish + 3 ta taklif
        satr.axis = .horizontal
        satr.distribution = .fill
        satr.translatesAutoresizingMaskIntoConstraints = false
        ogirTugma.setTitle("o→ö", for: .normal)
        ogirTugma.titleLabel?.font = .boldSystemFont(ofSize: 15)
        ogirTugma.addTarget(self, action: #selector(ogir), for: .touchUpInside)
        ogirTugma.widthAnchor.constraint(equalToConstant: 64).isActive = true
        satr.addArrangedSubview(ogirTugma)
        let uchlik = UIStackView()
        uchlik.distribution = .fillEqually
        for i in 0..<3 {
            let b = UIButton(type: .system)
            b.titleLabel?.font = .systemFont(ofSize: 17)
            b.titleLabel?.lineBreakMode = .byTruncatingTail
            b.tag = i
            b.addTarget(self, action: #selector(taklifBosildi(_:)), for: .touchUpInside)
            takliflar.append(b)
            uchlik.addArrangedSubview(b)
        }
        satr.addArrangedSubview(uchlik)

        kv.t = self
        kv.translatesAutoresizingMaskIntoConstraints = false
        ildiz.addSubview(satr)
        ildiz.addSubview(kv)
        NSLayoutConstraint.activate([
            satr.topAnchor.constraint(equalTo: ildiz.topAnchor),
            satr.leadingAnchor.constraint(equalTo: ildiz.leadingAnchor, constant: 4),
            satr.trailingAnchor.constraint(equalTo: ildiz.trailingAnchor, constant: -4),
            satr.heightAnchor.constraint(equalToConstant: 42),
            kv.topAnchor.constraint(equalTo: satr.bottomAnchor),
            kv.leadingAnchor.constraint(equalTo: ildiz.leadingAnchor),
            kv.trailingAnchor.constraint(equalTo: ildiz.trailingAnchor),
            kv.bottomAnchor.constraint(equalTo: ildiz.bottomAnchor),
        ])
        kv.korinishniOrnat(ildiz)
        sahifaQoy(.harf)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        mavzu()
        sahifaQoy(sahifa)
        avtoKatta()
        takliflarniYangila()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        mavzu()
        avtoKatta()
        takliflarniYangila()
    }

    private func mavzu() {
        let qorongi = textDocumentProxy.keyboardAppearance == .dark || traitCollection.userInterfaceStyle == .dark
        kv.tun = qorongi
        let fon = qorongi ? UIColor(rgb: 0x1F2023) : UIColor(rgb: 0xE8EAED)
        inputView?.backgroundColor = fon
        satr.backgroundColor = fon
        let m = qorongi ? UIColor(rgb: 0xE8EAED) : UIColor(rgb: 0x202124)
        for b in takliflar { b.setTitleColor(m, for: .normal) }
        ogirTugma.setTitleColor(m, for: .normal)
    }

    private func sahifaQoy(_ s: Sahifa) {
        sahifa = s
        let g = needsInputModeSwitchKey
        switch s {
        case .harf: kv.sahifa = Tartib.harflar(globus: g)
        case .belgi1: kv.sahifa = Tartib.belgi1(globus: g)
        case .belgi2: kv.sahifa = Tartib.belgi2(globus: g)
        }
        let h = kv.jamiBalandlik + 42
        if let b = balandlik { b.constant = h } else if let ildiz = inputView {
            balandlik = ildiz.heightAnchor.constraint(equalToConstant: h)
            balandlik?.priority = .defaultHigh
            balandlik?.isActive = true
        }
    }

    // MARK: - KlaviaturaTinglovchi

    func tegdi() {
        UIDevice.current.playInputClick()
    }

    func kursor(_ qadam: Int) {
        textDocumentProxy.adjustTextPosition(byCharacterOffset: qadam)
    }

    func matnYoz(_ s: String) {
        let p = textDocumentProxy
        let hozir = Date()
        if s == " " {
            // ikki marta bo'shliq -> ". "
            if hozir.timeIntervalSince(oxirgiBoshliq) < 0.7, let b = p.documentContextBeforeInput, b.count >= 2 {
                let oxiri = Array(b.suffix(2))
                if oxiri[1] == " " && (oxiri[0].isLetter || oxiri[0].isNumber) {
                    p.deleteBackward()
                    p.insertText(". ")
                    oxirgiBoshliq = .distantPast
                    tuzAsl = nil
                    avtoKatta(); takliflarniYangila()
                    return
                }
            }
            oxirgiBoshliq = hozir
        } else {
            oxirgiBoshliq = .distantPast
        }
        tuzAsl = nil
        if s.count == 1, " .,!?;:".contains(s) { sozTugadi(s) }
        p.insertText(s)
        if kv.katta == 1 { kv.katta = 0 }
        avtoKatta()
        takliflarniYangila()
    }

    func vazifa(_ v: Vazifa) {
        if v != .ochir && v != .shift { tuzAsl = nil }
        switch v {
        case .shift:
            let hozir = Date()
            if kv.katta == 0 { kv.katta = 1 }
            else if kv.katta == 1 && hozir.timeIntervalSince(oxirgiShift) < 0.35 { kv.katta = 2 }
            else { kv.katta = 0 }
            oxirgiShift = hozir
        case .ochir: ochir()
        case .belgi: sahifaQoy(.belgi1); takliflarniYangila()
        case .belgi2: sahifaQoy(.belgi2)
        case .harf: sahifaQoy(.harf); avtoKatta(); takliflarniYangila()
        case .til: advanceToNextInputMode()
        case .enter:
            sozTugadi("\n")
            textDocumentProxy.insertText("\n")
            avtoKatta()
        default: break
        }
    }

    private func ochir() {
        let p = textDocumentProxy
        // avtomatik tuzatishni bekor qilish
        if let asl = tuzAsl {
            let kutilgan = tuzYangi + tuzAjrat
            if let b = p.documentContextBeforeInput, b.hasSuffix(kutilgan) {
                for _ in 0..<kutilgan.count { p.deleteBackward() }
                p.insertText(asl)
                tuzatilmasin.insert(asl.lowercased())
                lugat.orgat(asl)
                tuzAsl = nil
                takliflarniYangila()
                return
            }
            tuzAsl = nil
        }
        p.deleteBackward()
        avtoKatta()
        takliflarniYangila()
    }

    // MARK: - katta harf

    private func avtoKatta() {
        guard sahifa == .harf, kv.katta != 2 else { return }
        let p = textDocumentProxy
        if p.autocapitalizationType == UITextAutocapitalizationType.none { kv.katta = 0; return }
        let b = p.documentContextBeforeInput ?? ""
        let qisqa = b.trimmingCharacters(in: .whitespaces)
        let kerak = b.isEmpty || b.hasSuffix("\n") || (b.hasSuffix(" ") && (qisqa.hasSuffix(".") || qisqa.hasSuffix("!") || qisqa.hasSuffix("?")))
        kv.katta = kerak ? 1 : 0
    }

    // MARK: - takliflar va tuzatish

    private func joriySoz() -> String {
        let b = textDocumentProxy.documentContextBeforeInput ?? ""
        var w = ""
        for ch in b.reversed() {
            if ch.isLetter || ch == Ogir.tutuq { w.insert(ch, at: w.startIndex) } else { break }
        }
        return w
    }

    private func maxfiy() -> Bool {
        let p = textDocumentProxy
        if p.isSecureTextEntry == true { return true }
        switch p.keyboardType ?? .default {
        case .emailAddress, .URL, .webSearch, .numberPad, .phonePad, .decimalPad, .asciiCapableNumberPad: return true
        default: return p.autocorrectionType == .no
        }
    }

    private static func holatMos(_ asl: String, _ s: String) -> String {
        guard let b = asl.first else { return s }
        if asl.count > 1 && asl == asl.uppercased() && asl != asl.lowercased() { return s.uppercased() }
        if b.isUppercase { return s.prefix(1).uppercased() + s.dropFirst() }
        return s
    }

    private func takliflarniYangila() {
        tuzatishTaklifi = nil
        let w = joriySoz()
        var chiq: [String] = []
        var asosiy = -1
        if sahifa == .harf, !maxfiy(), !w.isEmpty, lugat.tayyor {
            let kichik = w.lowercased()
            let tuz = lugat.tuzat(w)
            let boshqa = lugat.takliflar(w, soni: 4).filter { $0 != kichik && $0 != tuz }
            if let tuz = tuz {
                chiq = [w, Self.holatMos(w, tuz)] + boshqa.prefix(1).map { Self.holatMos(w, $0) }
                asosiy = 1
                tuzatishTaklifi = tuz
            } else {
                chiq = [w] + boshqa.prefix(2).map { Self.holatMos(w, $0) }
                if chiq.count >= 2 { chiq.swapAt(0, 1) }
            }
        }
        for (i, b) in takliflar.enumerated() {
            let s = i < chiq.count ? chiq[i] : ""
            b.setTitle(s, for: .normal)
            b.titleLabel?.font = i == asosiy ? .boldSystemFont(ofSize: 17) : .systemFont(ofSize: 17)
            b.isEnabled = !s.isEmpty
        }
    }

    @objc private func taklifBosildi(_ b: UIButton) {
        guard let s = b.title(for: .normal), !s.isEmpty else { return }
        let w = joriySoz()
        for _ in 0..<w.count { textDocumentProxy.deleteBackward() }
        textDocumentProxy.insertText(s + " ")
        lugat.orgat(s)
        tuzatilmasin.insert(s.lowercased())
        tuzAsl = nil
        if kv.katta == 1 { kv.katta = 0 }
        avtoKatta()
        takliflarniYangila()
    }

    private func sozTugadi(_ ajrat: String) {
        guard !maxfiy() else { return }
        let w = joriySoz()
        guard w.count >= 2 else { return }
        if !tuzatilmasin.contains(w.lowercased()), let tuz = lugat.tuzat(w) {
            let yangi = Self.holatMos(w, tuz)
            for _ in 0..<w.count { textDocumentProxy.deleteBackward() }
            textDocumentProxy.insertText(yangi)
            tuzAsl = w; tuzYangi = yangi; tuzAjrat = ajrat
            return
        }
        lugat.orgat(w)
    }

    /// Kursordan oldingi matnni (oxirgi gapgacha) yangi alifboga o'girish.
    @objc private func ogir() {
        let p = textDocumentProxy
        if let tanlangan = p.selectedText, !tanlangan.isEmpty {
            p.insertText(Ogir.yangiga(tanlangan))
            return
        }
        guard let oldin = p.documentContextBeforeInput, !oldin.isEmpty else { return }
        let yangi = Ogir.yangiga(oldin)
        guard yangi != oldin else { return }
        for _ in 0..<oldin.count { p.deleteBackward() }
        p.insertText(yangi)
        takliflarniYangila()
    }
}

/// Tugma bosilganda tizim "chiq" tovushi (sozlamalarda Keyboard Clicks yoqilgan bo'lsa).
final class IldizView: UIInputView, UIInputViewAudioFeedback {
    var enableInputClicksWhenVisible: Bool { true }
}

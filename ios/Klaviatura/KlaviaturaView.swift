import UIKit

protocol KlaviaturaTinglovchi: AnyObject {
    func matnYoz(_ s: String)
    func vazifa(_ v: Vazifa)
    func kursor(_ qadam: Int)
    func tegdi()
}

/// Gboard uslubidagi, o'zi chizadigan klaviatura (Android'dagi KlaviaturaView bilan bir xil).
final class KlaviaturaView: UIView {
    weak var t: KlaviaturaTinglovchi?
    var sahifa: [Qator] = [] { didSet { joylash(); setNeedsDisplay() } }
    var katta = 0 { didSet { if katta != oldValue { setNeedsDisplay() } } } // 0 kichik, 1 bitta, 2 doimiy
    var tun = true { didSet { setNeedsDisplay() } }

    private var bosilgan: Tugma?
    private var boshX: CGFloat = 0, oxirgiX: CGFloat = 0
    private var surildi = false, bajarildi = false
    private var uzunTaymer: Timer?, takrorTaymer: Timer?
    private var altTanlov = -1
    private var altRamka: CGRect = .zero
    private let korinish = UILabel()

    // ranglar
    private var fon: UIColor { tun ? UIColor(rgb: 0x1F2023) : UIColor(rgb: 0xE8EAED) }
    private var harfRang: UIColor { tun ? UIColor(rgb: 0x3C3D41) : .white }
    private var funkRang: UIColor { tun ? UIColor(rgb: 0x4A5563) : UIColor(rgb: 0xC9D2DE) }
    private var bosilganRang: UIColor { tun ? UIColor(rgb: 0x666A72) : UIColor(rgb: 0xB8BEC6) }
    private var matnRang: UIColor { tun ? UIColor(rgb: 0xE8EAED) : UIColor(rgb: 0x202124) }
    private var aksent: UIColor { tun ? UIColor(rgb: 0x8AB4F8) : UIColor(rgb: 0x1A73E8) }

    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = false
        contentMode = .redraw
        korinish.textAlignment = .center
        korinish.font = .systemFont(ofSize: 30)
        korinish.layer.cornerRadius = 10
        korinish.clipsToBounds = true
        korinish.isHidden = true
    }

    required init?(coder: NSCoder) { fatalError() }

    var jamiBalandlik: CGFloat { sahifa.reduce(0) { $0 + $1.bal } + 8 }

    override func layoutSubviews() {
        super.layoutSubviews()
        joylash()
    }

    private func joylash() {
        let w = bounds.width
        guard w > 0 else { return }
        var y: CGFloat = 4
        let pad: CGFloat = 1.5
        for q in sahifa {
            let jami = q.tugmalar.reduce(0) { $0 + $1.en } + 2 * q.chet
            let birlik = (w - 2 * pad) / jami
            var x = pad + q.chet * birlik
            for k in q.tugmalar {
                k.ramka = CGRect(x: x, y: y, width: k.en * birlik, height: q.bal)
                x += k.en * birlik
            }
            y += q.bal
        }
    }

    // MARK: - chizish

    private func yorliq(_ k: Tugma) -> String {
        if case .shift = k.vazifa { return katta == 2 ? "⇪" : "⇧" }
        if k.tur == .harf, katta > 0 { return k.label.uppercased() }
        return k.label
    }

    override func draw(_ rect: CGRect) {
        fon.setFill()
        UIRectFill(bounds)
        for q in sahifa {
            for k in q.tugmalar {
                var rang: UIColor
                switch k.tur {
                case .harf, .boshliq: rang = harfRang
                case .funk, .enter: rang = funkRang
                }
                if k === bosilgan { rang = bosilganRang }
                let r = k.ramka.insetBy(dx: 2.5, dy: 4)
                let rad = (k.tur == .enter || k.label == "123" || k.label == "ABC") ? r.height / 2 : 8
                rang.setFill()
                UIBezierPath(roundedRect: r, cornerRadius: rad).fill()
                if !tun {
                    UIColor(white: 0, alpha: 0.12).setFill()
                    UIBezierPath(roundedRect: CGRect(x: r.minX, y: r.maxY - 1, width: r.width, height: 1), cornerRadius: 0.5).fill()
                }
                let s = yorliq(k)
                var shrift: UIFont
                var rangM = matnRang
                switch k.tur {
                case .boshliq: shrift = .systemFont(ofSize: 14); rangM = matnRang.withAlphaComponent(0.6)
                case .harf: shrift = .systemFont(ofSize: q.bal < 50 ? 20 : 24)
                default: shrift = s.count > 2 ? .boldSystemFont(ofSize: 15) : .systemFont(ofSize: 22)
                }
                if case .shift = k.vazifa, katta == 2 { rangM = aksent }
                if case .ochir = k.vazifa { shrift = .systemFont(ofSize: 26) }
                let attr: [NSAttributedString.Key: Any] = [.font: shrift, .foregroundColor: rangM]
                let o = (s as NSString).size(withAttributes: attr)
                (s as NSString).draw(at: CGPoint(x: r.midX - o.width / 2, y: r.midY - o.height / 2), withAttributes: attr)
                if case .shift = k.vazifa, katta == 1 {
                    aksent.setFill()
                    UIBezierPath(ovalIn: CGRect(x: r.midX - 2.5, y: r.maxY - 9, width: 5, height: 5)).fill()
                }
            }
        }
        // bosib turilganda qo'shimcha belgilar
        if altTanlov >= 0, let k = bosilgan {
            (tun ? UIColor(rgb: 0x4A4D53) : UIColor(rgb: 0xF8F9FA)).setFill()
            UIBezierPath(roundedRect: altRamka, cornerRadius: 10).fill()
            let cw = altRamka.width / CGFloat(k.alt.count)
            for (i, s) in k.alt.enumerated() {
                let r = CGRect(x: altRamka.minX + CGFloat(i) * cw, y: altRamka.minY, width: cw, height: altRamka.height).insetBy(dx: 2, dy: 4)
                if i == altTanlov { aksent.setFill(); UIBezierPath(roundedRect: r, cornerRadius: 7).fill() }
                let yoz = katta > 0 ? s.uppercased() : s
                let attr: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: yoz.count > 2 ? 15 : 22),
                                                           .foregroundColor: i == altTanlov ? fon : matnRang]
                let o = (yoz as NSString).size(withAttributes: attr)
                (yoz as NSString).draw(at: CGPoint(x: r.midX - o.width / 2, y: r.midY - o.height / 2), withAttributes: attr)
            }
        }
    }

    // MARK: - ko'rinish (bosilgan harf ustida)

    func korinishniOrnat(_ ota: UIView) {
        if korinish.superview == nil { ota.addSubview(korinish) }
    }

    private func korinishKorsat(_ k: Tugma) {
        guard k.tur == .harf, let ota = korinish.superview else { korinish.isHidden = true; return }
        korinish.text = yorliq(k)
        korinish.backgroundColor = tun ? UIColor(rgb: 0x5F636A) : .white
        korinish.textColor = tun ? .white : UIColor(rgb: 0x202124)
        let r = convert(k.ramka, to: ota)
        let w = max(r.width - 2, 44), h = r.height * 1.05
        korinish.frame = CGRect(x: r.midX - w / 2, y: r.minY + 4 - h, width: w, height: h)
        korinish.isHidden = false
        ota.bringSubviewToFront(korinish)
    }

    // MARK: - teginish

    private func top(_ p: CGPoint) -> Tugma? {
        var eng: Tugma?
        var engM = CGFloat.greatestFiniteMagnitude
        for q in sahifa {
            for k in q.tugmalar {
                if k.ramka.contains(p) { return k }
                let dx = max(k.ramka.minX - p.x, 0, p.x - k.ramka.maxX)
                let dy = max(k.ramka.minY - p.y, 0, p.y - k.ramka.maxY)
                if dx * dx + dy * dy < engM { engM = dx * dx + dy * dy; eng = k }
            }
        }
        return eng
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let p = touches.first?.location(in: self), let k = top(p) else { return }
        if let eski = bosilgan, altTanlov < 0, !bajarildi { yakunla(eski) } // tez yozish
        tozala()
        bosilgan = k
        boshX = p.x; oxirgiX = p.x
        surildi = false; bajarildi = false
        t?.tegdi()
        if case .ochir = k.vazifa {
            t?.vazifa(.ochir)
            bajarildi = true
            takrorTaymer = Timer.scheduledTimer(withTimeInterval: 0.4, repeats: false) { [weak self] _ in
                self?.takrorTaymer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in self?.t?.vazifa(.ochir) }
            }
        } else {
            uzunTaymer = Timer.scheduledTimer(withTimeInterval: 0.35, repeats: false) { [weak self] _ in self?.uzunBosish() }
        }
        korinishKorsat(k)
        setNeedsDisplay()
    }

    private func uzunBosish() {
        guard let k = bosilgan, !bajarildi, !surildi, !k.alt.isEmpty else { return }
        korinish.isHidden = true
        let cw = max(k.ramka.width - 2, 40)
        let w = cw * CGFloat(k.alt.count)
        var x = k.ramka.midX - cw / 2
        x = min(max(x, 2), bounds.width - w - 2)
        altRamka = CGRect(x: x, y: max(k.ramka.minY - k.ramka.height + 2, 0), width: w, height: k.ramka.height)
        altTanlov = 0
        setNeedsDisplay()
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let p = touches.first?.location(in: self), let k = bosilgan else { return }
        if altTanlov >= 0 {
            let cw = altRamka.width / CGFloat(k.alt.count)
            let s = min(max(Int((p.x - altRamka.minX) / cw), 0), k.alt.count - 1)
            if s != altTanlov { altTanlov = s; setNeedsDisplay() }
            return
        }
        if case .boshliq = k.vazifa {
            if !surildi && abs(p.x - boshX) > 14 { surildi = true; uzunTaymer?.invalidate() }
            if surildi {
                let n = Int((p.x - oxirgiX) / 12)
                if n != 0 { t?.kursor(n); oxirgiX += CGFloat(n) * 12 }
            }
            return
        }
        if case .ochir = k.vazifa { return }
        if let y = top(p), y !== k, y.tur == .harf {
            uzunTaymer?.invalidate()
            bosilgan = y
            uzunTaymer = Timer.scheduledTimer(withTimeInterval: 0.35, repeats: false) { [weak self] _ in self?.uzunBosish() }
            korinishKorsat(y)
            setNeedsDisplay()
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        if let k = bosilgan { yakunla(k) }
        tozala()
        setNeedsDisplay()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        tozala()
        setNeedsDisplay()
    }

    private func yakunla(_ k: Tugma) {
        if altTanlov >= 0 {
            let s = k.alt[altTanlov]
            t?.matnYoz(katta > 0 ? s.uppercased() : s)
        } else if !bajarildi && !surildi {
            switch k.vazifa {
            case .matn(let s): t?.matnYoz(katta > 0 ? s.uppercased() : s)
            case .boshliq: t?.matnYoz(" ")
            default: t?.vazifa(k.vazifa)
            }
        }
        bajarildi = true
    }

    private func tozala() {
        uzunTaymer?.invalidate(); uzunTaymer = nil
        takrorTaymer?.invalidate(); takrorTaymer = nil
        bosilgan = nil
        altTanlov = -1
        korinish.isHidden = true
    }
}

extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}

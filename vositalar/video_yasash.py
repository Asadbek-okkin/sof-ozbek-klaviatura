"""Sof Ozbek klaviatura - qisqa o'quv videolari (720x1280, 30 kadr/s).

Ishlatish: python video_yasash.py <chiqish_papkasi>
Klaviatura ko'rinishi ilovadagi KlaviaturaView bilan bir xil o'lcham va ranglarda chiziladi.
"""
import os
import subprocess
import sys

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont

W, H, FPS = 720, 1280, 30
DP = 2.0
F = "C:/Windows/Fonts/"


def shrift(nom, o):
    return ImageFont.truetype(F + nom, int(o))


S_ODDIY = lambda o: shrift("segoeui.ttf", o)
S_QALIN = lambda o: shrift("segoeuib.ttf", o)
S_BELGI = lambda o: shrift("seguisym.ttf", o)
S_EMOJI = shrift("seguiemj.ttf", 109)  # emoji faqat 109 o'lchamda aniq chiqadi

FON = (31, 32, 35)
HARF = (60, 61, 65)
FUNK = (74, 85, 99)
BOSILGAN = (102, 106, 114)
MATN = (232, 234, 237)
HINT = (154, 160, 166)
AKSENT = (138, 180, 248)
KOK = (26, 119, 210)


# ---------------------------------------------------------------- klaviatura

def qatorlar():
    q = []
    q.append((46, [(c, 1, "h") for c in "1234567890"], 0))
    q.append((56, [(c, 1, "h") for c in "q w e r t y u i o ö p".split()], 0))
    q.append((56, [(c, 1, "h") for c in "a s d f g h j k l ş".split()], 0.5))
    q.append((56, [("⇧", 1, "f")] + [(c, 1, "h") for c in "z x c ç v b n m ğ".split()] + [("⌫", 1, "f")], 0))
    q.append((54, [("?123", 1.5, "f"), ("☺", 1, "f"), ("🌐", 1, "f"), ("Özbekça", 4, "s"), (".", 1, "h"), ("↵", 1.5, "e")], 0))
    return q


ASBOB_H = 44 * DP
KLAV_H = ASBOB_H + sum(r[0] for r in qatorlar()) * DP + 10 * DP
KLAV_Y = H - KLAV_H


def tugmalar():
    """{yorliq: (x, y, w, h, tur)}"""
    t = {}
    y = KLAV_Y + ASBOB_H + 4 * DP
    pad = 1.5 * DP
    for bal, keys, chet in qatorlar():
        jami = sum(k[1] for k in keys) + 2 * chet
        birlik = (W - 2 * pad) / jami
        x = pad + chet * birlik
        for lab, en, tur in keys:
            w = en * birlik
            t[lab] = (x, y, w, bal * DP, tur)
            x += w
        y += bal * DP
    return t


TUG = tugmalar()


def matn_markaz(d, x, y, s, f, rang):
    bb = d.textbbox((0, 0), s, font=f)
    d.text((x - (bb[2] - bb[0]) / 2 - bb[0], y - (bb[3] - bb[1]) / 2 - bb[1]), s, font=f, fill=rang)


def emoji(img, x, y, e, o):
    """Rangli emojini (x,y) markazga o'lchami o bilan qo'yadi."""
    t = Image.new("RGBA", (160, 160), (0, 0, 0, 0))
    ImageDraw.Draw(t).text((80, 80), e, font=S_EMOJI, embedded_color=True, anchor="mm")
    t = t.resize((int(o), int(o)), Image.LANCZOS)
    img.paste(t, (int(x - o / 2), int(y - o / 2)), t)


def emojimi(c):
    return ord(c) >= 0x1F000 or c in "☺⚙✅✨❤⚽☕⏳"


def aralash(img, d, x, y, matn, f, rang, markaz=False):
    """Oddiy matn + rangli emoji aralash qatorini chizadi. (x, y) - chap-yuqori (yoki markaz)."""
    o = f.size
    bo_laklar = []
    joriy = ""
    for c in matn:
        if c == "️":
            continue
        if emojimi(c):
            if joriy:
                bo_laklar.append(("m", joriy))
                joriy = ""
            bo_laklar.append(("e", c))
        else:
            joriy += c
    if joriy:
        bo_laklar.append(("m", joriy))
    en = sum(d.textlength(s, font=f) if t == "m" else o * 1.15 for t, s in bo_laklar)
    if markaz:
        x, y = x - en / 2, y - o * 0.62
    for t, s in bo_laklar:
        if t == "m":
            d.text((x, y), s, font=f, fill=rang)
            x += d.textlength(s, font=f)
        else:
            emoji(img, x + o * 0.55, y + o * 0.68, s, o * 1.05)
            x += o * 1.15


def klaviatura(img, d, h):
    """h - holat: bosilgan, katta, asboblar (list|None), panel"""
    d.rectangle([0, KLAV_Y, W, H], fill=FON)
    # asboblar satri / takliflar
    ay = KLAV_Y + ASBOB_H / 2
    if h.get("takliflar"):
        katak = (W - 52 * DP) / 3
        for i, s in enumerate(h["takliflar"][:3]):
            if i > 0:
                d.line([i * katak, ay - 14 * DP, i * katak, ay + 14 * DP], fill=(80, 82, 86), width=2)
            if h.get("taklif_bosildi") == i:
                d.rounded_rectangle([i * katak + 6, KLAV_Y + 10, (i + 1) * katak - 6, KLAV_Y + ASBOB_H - 10], 16, fill=HARF)
            f = S_QALIN(32) if i == h.get("asosiy", -1) else S_ODDIY(32)
            matn_markaz(d, i * katak + katak / 2, ay, s, f, MATN)
    elif h.get("xabar"):
        aralash(img, d, (W - 52 * DP) / 2, ay, h["xabar"], S_ODDIY(30), MATN, markaz=True)
        d.ellipse([W - 26 * DP - 32, ay - 32, W - 26 * DP + 32, ay + 32], fill=(229, 57, 53))
    else:
        belgilar = ["📋", "ö", "🎨", "⚙"]
        katak = (W - 52 * DP) / 4
        for i, b in enumerate(belgilar):
            cx = i * katak + katak / 2
            if h.get("asbob_bosildi") == i:
                d.ellipse([cx - 38, ay - 38, cx + 38, ay + 38], fill=HARF)
            if b == "ö":
                matn_markaz(d, cx, ay - 4, "o→ö", S_QALIN(26), MATN)
            else:
                emoji(img, cx, ay, b, 40)
    mx = W - 26 * DP
    if h.get("mik_bosildi"):
        d.ellipse([mx - 38, ay - 38, mx + 38, ay + 38], fill=HARF)
    emoji(img, mx, ay, "🎤", 40)

    if h.get("panel") == "emoji":
        emojilar = "😀 😂 🥰 😍 😎 🤔 😢 😡 👍 👏 🙏 💪 ❤️ 🔥 🎉 ✨ 🌹 🌙 ⚽ 🍉 🚗 ☕ 📚 💯".split()
        y0 = KLAV_Y + ASBOB_H + 70
        for i, e in enumerate(emojilar):
            emoji(img, 60 + (i % 6) * 120, y0 + (i // 6) * 105, e, 64)
        d.rounded_rectangle([10, H - 110, 180, H - 20], 16, fill=FUNK)
        matn_markaz(d, 95, H - 65, "ABC", S_QALIN(32), MATN)
        return
    if h.get("panel") == "bufer":
        matn_markaz(d, W / 2 - 80, KLAV_Y + ASBOB_H + 40, "Bufer — nusxalanganlar", S_QALIN(30), MATN)
        y = KLAV_Y + ASBOB_H + 90
        for s in h.get("bufer", []):
            d.rounded_rectangle([14, y, W - 14, y + 90], 16, fill=HARF)
            d.text((40, y + 25), s, font=S_ODDIY(30), fill=MATN)
            y += 104
        return

    for lab, (x, y, w, hh, tur) in TUG.items():
        rang = {"h": HARF, "s": HARF, "f": FUNK, "e": FUNK}[tur]
        if h.get("bosilgan") == lab:
            rang = BOSILGAN
        r = [x + 2.25 * DP, y + 4 * DP, x + w - 2.25 * DP, y + hh - 4 * DP]
        rad = (r[3] - r[1]) / 2 if tur == "e" or lab == "?123" else 8 * DP
        d.rounded_rectangle(r, rad, fill=rang)
        cx, cy = (r[0] + r[2]) / 2, (r[1] + r[3]) / 2
        katta = h.get("katta") and tur == "h"
        s = lab.upper() if katta else lab
        if lab == "🌐":
            emoji(img, cx, cy, "🌐", 40)
        elif lab == "☺":
            matn_markaz(d, cx, cy - 6, "☺", S_BELGI(40), MATN)
            matn_markaz(d, cx, cy + 26, ",", S_QALIN(26), MATN)
        elif tur == "s":
            matn_markaz(d, cx, cy, s, S_ODDIY(26), HINT)
        elif tur in ("f", "e"):
            f = S_QALIN(28) if len(lab) > 2 else S_BELGI(40)
            matn_markaz(d, cx, cy, s, f, AKSENT if lab == "⇧" and h.get("katta") else MATN)
        else:
            matn_markaz(d, cx, cy, s, S_ODDIY(38 if hh > 100 else 34), MATN)

    # ko'rinish (bosilgan harf ustida katta harf)
    b = h.get("bosilgan")
    if b and b in TUG and TUG[b][4] == "h":
        x, y, w, hh, _ = TUG[b]
        pw, ph = max(w - 4, 88), hh * 1.1
        px, py = x + w / 2 - pw / 2, y + 8 - ph
        d.rounded_rectangle([px, py, px + pw, py + ph], 20, fill=(95, 99, 106))
        s = b.upper() if h.get("katta") else b
        matn_markaz(d, px + pw / 2, py + ph / 2, s, S_ODDIY(56), (255, 255, 255))
    # bosib turilganda qo'shimcha belgilar
    if h.get("alt"):
        lab, var, tan = h["alt"]
        x, y, w, hh, _ = TUG[lab]
        cw = 84
        px = min(max(x + w / 2 - cw / 2, 8), W - cw * len(var) - 16)
        py = y - hh + 4
        d.rounded_rectangle([px, py, px + cw * len(var) + 8, py + hh * 0.95], 20, fill=(74, 77, 83))
        for i, s in enumerate(var):
            r = [px + 4 + i * cw + 4, py + 8, px + 4 + (i + 1) * cw - 4, py + hh * 0.95 - 8]
            if i == tan:
                d.rounded_rectangle(r, 14, fill=AKSENT)
            matn_markaz(d, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2, s, S_ODDIY(44), FON if i == tan else MATN)


# ---------------------------------------------------------------- sahna

def sahna(h):
    img = Image.new("RGB", (W, H), (21, 23, 27))
    d = ImageDraw.Draw(img)
    # sarlavha
    d.rectangle([0, 0, W, 210], fill=KOK)
    d.text((40, 34), h.get("sarlavha", ""), font=S_QALIN(46), fill=(255, 255, 255))
    y = 108
    for qator in h.get("izoh", "").split("\n"):
        aralash(img, d, 40, y, qator, S_ODDIY(31), (225, 236, 252))
        y += 42
    if h.get("ekran"):
        h["ekran"](img, d, h)
    else:
        # matn maydoni
        d.rounded_rectangle([30, 250, W - 30, KLAV_Y - 40], 26, fill=(35, 38, 44))
        d.text((60, 270), h.get("maydon_nomi", "Xabar"), font=S_ODDIY(26), fill=HINT)
        matn = h.get("matn", "")
        qatorlar_ = []
        joriy = ""
        for soz in matn.split(" "):
            sinov = (joriy + " " + soz).strip() if joriy else soz
            if d.textlength(sinov, font=S_ODDIY(40)) > W - 140:
                qatorlar_.append(joriy)
                joriy = soz
            else:
                joriy = sinov
        qatorlar_.append(joriy)
        ty = 320
        for i, q in enumerate(qatorlar_):
            d.text((60, ty), q, font=S_ODDIY(40), fill=MATN)
            if i == len(qatorlar_) - 1 and h.get("kursor", True):
                kx = 60 + d.textlength(q, font=S_ODDIY(40)) + 4
                d.rectangle([kx, ty + 6, kx + 3, ty + 52], fill=AKSENT)
            ty += 56
        klaviatura(img, d, h)
    if h.get("barmoq"):
        x, y = h["barmoq"]
        d.ellipse([x - 34, y - 34, x + 34, y + 34], outline=(255, 255, 255), width=5)
        d.ellipse([x - 14, y - 14, x + 14, y + 14], fill=(255, 255, 255))
    return img


class Video:
    def __init__(self, yol):
        exe = imageio_ffmpeg.get_ffmpeg_exe()
        self.p = subprocess.Popen([exe, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24",
                                   "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-c:v", "libx264",
                                   "-pix_fmt", "yuv420p", "-crf", "23", "-preset", "medium",
                                   "-movflags", "+faststart", yol], stdin=subprocess.PIPE)
        self.h = {}

    def kadr(self, n=1):
        b = sahna(self.h).tobytes()
        for _ in range(n):
            self.p.stdin.write(b)

    def kut(self, s):
        self.kadr(int(s * FPS))

    def qoy(self, **k):
        self.h.update(k)

    def tugma_markazi(self, lab):
        x, y, w, hh, _ = TUG[lab]
        return x + w / 2, y + hh / 2

    def bos(self, lab, yoz=None, tez=False):
        self.h["barmoq"] = self.tugma_markazi(lab)
        self.h["bosilgan"] = lab
        self.kadr(4 if tez else 7)
        if yoz is not None:
            self.h["matn"] = self.h.get("matn", "") + yoz
        self.h["bosilgan"] = None
        self.kadr(2 if tez else 5)

    def yoz(self, matn, tez=True):
        for ch in matn:
            lab = "Özbekça" if ch == " " else ch.lower()
            if lab not in TUG:
                self.h["matn"] = self.h.get("matn", "") + ch
                self.kadr(2)
                continue
            if ch.isupper():
                self.h["katta"] = True
            self.bos(lab, ch, tez)
            self.h["katta"] = False

    def tugat(self):
        self.p.stdin.close()
        self.p.wait()


# ---------------------------------------------------------------- videolar

def v1_ornatish(yol):
    v = Video(yol)

    def ekran_fn(qadam):
        def chiz(img, d, h):
            d.rectangle([0, 210, W, H], fill=(21, 23, 27))
            d.text((50, 260), "Sof Özbek klaviatura", font=S_QALIN(44), fill=MATN)
            d.text((50, 320), "Yangi lotin alifbosi uçun", font=S_ODDIY(30), fill=HINT)
            kartalar = [("1. Klaviaturani yoqiş", "✅ Yoqilgan" if qadam >= 2 else "Bosing va «Sof Özbek» ni yoqing"),
                        ("2. Asosiy klaviatura qiliş", "✅ Tanlangan — tayyor!" if qadam >= 4 else "Bosing va «Sof Özbek» ni tanlang")]
            y = 420
            for i, (a, b) in enumerate(kartalar):
                bosildi = (qadam == 1 and i == 0) or (qadam == 3 and i == 1)
                d.rounded_rectangle([40, y, W - 40, y + 140], 28, fill=(50, 56, 66) if bosildi else (35, 38, 44))
                d.text((76, y + 26), a, font=S_QALIN(36), fill=MATN)
                d.text((76, y + 80), b, font=S_ODDIY(28), fill=(52, 168, 83) if "✅" in b else HINT)
                y += 170
            if qadam in (1, 3):
                v.h["barmoq"] = (W / 2, 490 if qadam == 1 else 660)
            else:
                v.h["barmoq"] = None
        return chiz

    v.qoy(sarlavha="1. Ōrnatiş", izoh="Telegramdan kelgan APK faylni bosing\nva «Ōrnatiş» ni tanlang")
    v.qoy(ekran=lambda img, d, h: (d.rounded_rectangle([60, 400, W - 60, 620], 30, fill=(35, 38, 44)),
                                   d.text((100, 440), "📦  SofOzbekKlaviatura.apk", font=S_QALIN(34), fill=MATN),
                                   d.text((100, 500), "640 KB  •  Bosing → «Ōrnatiş»", font=S_ODDIY(30), fill=HINT)))
    v.kut(3)
    for q, izoh, s in [(0, "Ilovani oçing — 2 ta qadam bor", 2.5), (1, "1-qadam: «Klaviaturani yoqiş» ni bosing\nva ro'yxatda «Sof Özbek» ni yoqing", 3.5),
                       (2, "Yoqildi ✅", 1.5), (3, "2-qadam: «Asosiy klaviatura qiliş»\nva «Sof Özbek (yangi lotin)» ni tanlang", 3.5),
                       (4, "Tayyor! Endi istalgan joyda\nyangi alifboda yozing 🎉", 3)]:
        v.qoy(izoh=izoh, ekran=ekran_fn(q))
        v.kut(s)
    v.qoy(ekran=None, barmoq=None, sarlavha="1. Ōrnatiş", izoh="Tayyor! Endi istalgan joyda\nyangi alifboda yozing 🎉", matn="Salom! ")
    v.yoz("Özbekça yozamiz", tez=False)
    v.kut(2)
    v.tugat()


def v2_yozish(yol):
    v = Video(yol)
    v.qoy(sarlavha="2. Yangi harflar", izoh="Ö Ş Ç Ğ — alohida tugmada,\nbitta bosiş bilan", matn="")
    v.kut(2)
    v.qoy(izoh="Ö — o ning yonida")
    v.bos("ö", "ö")
    v.qoy(izoh="Ş — ikkinçi qator oxirida")
    v.bos("ş", "ş")
    v.qoy(izoh="Ç — c ning yonida, Ğ — m dan keyin")
    v.bos("ç", "ç")
    v.bos("ğ", "ğ")
    v.kut(1)
    v.qoy(matn="", izoh="Gap boşi o'zi katta harf bilan boşlanadi")
    v.qoy(katta=True)
    v.kadr(10)
    v.yoz("Şahrimiz çiroyli. ", tez=False)
    v.qoy(izoh="Tutuq belgisi ʼ — nuqtani bosib turing")
    v.yoz("Maʼ"[:2], tez=False)
    x, y = v.tugma_markazi(".")
    v.qoy(barmoq=(x, y), bosilgan=".")
    v.kut(0.6)
    var = [",", "?", "!", "ʼ", "'"]
    for i in range(4):
        v.qoy(alt=(".", var, i), barmoq=(x - 250 + i * 84 + 40, y - 40))
        v.kadr(6)
    v.qoy(alt=None, bosilgan=None, matn=v.h["matn"] + "ʼ", barmoq=None)
    v.kadr(6)
    v.yoz("no", tez=False)
    v.qoy(izoh="⇧ ni ikki marta tez bossangiz —\ndoimiy KATTA harf")
    v.kut(2.5)
    v.qoy(izoh="Raqamlar — yuqori qatorda doim tayyor")
    v.yoz(" 2026", tez=False)
    v.kut(2)
    v.tugat()


def v3_takliflar(yol):
    v = Video(yol)
    v.qoy(sarlavha="3. Sōz takliflari", izoh="Yozayotganda tepada 3 ta sōz çiqadi —\nbosing, sōz tōliq yoziladi", matn="Bugun ")
    v.kut(2)
    v.yoz("şa", tez=False)
    v.qoy(takliflar=["şahar", "şaharda", "şahri"], asosiy=-1)
    v.kadr(20)
    v.yoz("h", tez=False)
    v.kut(1)
    v.qoy(taklif_bosildi=1, barmoq=((W - 104) / 3 * 1.5, KLAV_Y + ASBOB_H / 2))
    v.kadr(10)
    v.qoy(taklif_bosildi=None, barmoq=None, takliflar=None, matn="Bugun şaharda ")
    v.kut(1.2)
    v.qoy(izoh="Xato yozsangiz — bōşliq bosilganda\nō'zi tuzatadi: sahar → şahar")
    v.yoz("sahar", tez=False)
    v.qoy(takliflar=["sahar", "şahar", "sahro"], asosiy=1)
    v.kadr(30)
    v.bos("Özbekça")
    v.qoy(matn="Bugun şaharda şahar ", takliflar=None)
    v.kut(2)
    v.qoy(izoh="Tuzatiş kerak bōlmasa — ⌫ ni bosing,\nyozganingiz qaytadi")
    v.bos("⌫")
    v.qoy(matn="Bugun şaharda sahar")
    v.kut(2)
    v.qoy(izoh="Klaviatura siz kōp yozgan sōzlarni\neslab qoladi va keyin taklif qiladi")
    v.kut(3)
    v.tugat()


def v4_ovoz_ogirish(yol):
    v = Video(yol)
    v.qoy(sarlavha="4. Ovoz va ōgiriş", izoh="🎤 ni bosing va gapiring —\nmatn yangi alifboda yoziladi", matn="")
    v.kut(2)
    v.qoy(mik_bosildi=True, barmoq=(W - 26 * DP, KLAV_Y + ASBOB_H / 2))
    v.kadr(8)
    v.qoy(mik_bosildi=False, barmoq=None, xabar="🎤 Gapiring...")
    v.kut(1.5)
    for s in ["Assalomu", "Assalomu alaykum", "Assalomu alaykum, çoy içamizmi?"]:
        v.qoy(xabar=s)
        v.kut(0.8)
    v.qoy(xabar=None, matn="Assalomu alaykum, çoy içamizmi?")
    v.kut(2)
    v.qoy(izoh="Eski yozuvdagi matnni bir bosişda\nyangi alifboga ōgiriş: o→ö",
          matn="O'zbekiston - go'zal o'lka, shahar va qishloqlari chiroyli")
    v.kut(3)
    v.qoy(asbob_bosildi=1, barmoq=((W - 104) / 4 * 1.5, KLAV_Y + ASBOB_H / 2))
    v.kadr(10)
    v.qoy(asbob_bosildi=None, barmoq=None, xabar="✅ Matn yangi alifboga ōgirildi",
          matn="Özbekiston - gözal ölka, şahar va qişloqlari çiroyli")
    v.kut(3)
    v.qoy(xabar=None, izoh="Kirill ham ōgiriladi:\nЎзбекистон → Özbekiston", matn="Ўзбекистон шаҳри чиройли")
    v.kut(2)
    v.qoy(asbob_bosildi=1, barmoq=((W - 104) / 4 * 1.5, KLAV_Y + ASBOB_H / 2))
    v.kadr(10)
    v.qoy(asbob_bosildi=None, barmoq=None, matn="Özbekiston şahri çiroyli")
    v.kut(3)
    v.tugat()


def v5_fishkalar(yol):
    v = Video(yol)
    v.qoy(sarlavha="5. Foydali fişkalar", izoh="☺ — smayliklar tugmasi", matn="Tabriklayman ")
    v.kut(1.5)
    v.bos("☺")
    v.qoy(panel="emoji")
    v.kut(1.5)
    for e in ["🎉", "🔥", "🇺🇿"]:
        v.qoy(barmoq=(W / 2, KLAV_Y + 300))
        v.kadr(6)
        v.qoy(matn=v.h["matn"] + e, barmoq=None)
        v.kadr(10)
    v.qoy(panel=None, izoh="📋 Bufer — oxirgi nusxalangan\nmatnlar bir bosişda qōyiladi", matn="")
    v.kut(1)
    v.qoy(asbob_bosildi=0, barmoq=((W - 104) / 4 * 0.5, KLAV_Y + ASBOB_H / 2))
    v.kadr(8)
    v.qoy(asbob_bosildi=None, barmoq=None, panel="bufer", bufer=["+998 90 123 45 67", "Toşkent, Çilonzor 9-kvartal", "Rahmat, kōrişguncha!"])
    v.kut(1.5)
    v.qoy(barmoq=(W / 2, KLAV_Y + ASBOB_H + 135))
    v.kadr(8)
    v.qoy(barmoq=None, matn="+998 90 123 45 67", panel=None)
    v.kut(1.5)
    v.qoy(izoh="🌐 — English va Русский klaviatura\nBōşliqni surib — kursor yuradi", matn="Hello! Привет! ")
    v.kut(3)
    v.qoy(izoh="🎨 — mavzu: qorong'i, yorug', kōk, qora\n⚙ — sozlamalar: tugma balandligi, tovuş...")
    v.kut(3.5)
    v.qoy(izoh="Yangi versiya va yangi sōzlar\nō'zi keladi — hech narsa qilmang 🎉")
    v.kut(3)
    v.tugat()


if __name__ == "__main__":
    papka = sys.argv[1]
    os.makedirs(papka, exist_ok=True)
    for nom, fn in [("1-ornatish", v1_ornatish), ("2-yangi-harflar", v2_yozish), ("3-soz-takliflari", v3_takliflar),
                    ("4-ovoz-va-ogirish", v4_ovoz_ogirish), ("5-fishkalar", v5_fishkalar)]:
        fn(os.path.join(papka, nom + ".mp4"))
        print("tayyor:", nom)
    # hammasini bitta to'liq qo'llanmaga birlashtirish
    royxat = os.path.join(papka, "royxat.txt")
    with open(royxat, "w", encoding="utf-8") as f:
        for nom in ["1-ornatish", "2-yangi-harflar", "3-soz-takliflari", "4-ovoz-va-ogirish", "5-fishkalar"]:
            f.write(f"file '{nom}.mp4'\n")
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-loglevel", "error", "-y", "-f", "concat", "-safe", "0",
                    "-i", royxat, "-c", "copy", "-movflags", "+faststart",
                    os.path.join(papka, "0-toliq-qollanma.mp4")], check=True)
    os.remove(royxat)
    print("tayyor: 0-toliq-qollanma")

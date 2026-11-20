"""Leipzig korpuslaridan yangi alifbodagi o'zbekcha lug'at yasaydi.

Ishlatish: python lugat_yasash.py <korpuslar_papkasi> <chiqish.txt> [soni]
Kirill va eski lotin (o', g', sh, ch) so'zlar yangi alifboga o'giriladi:
o' -> ö, g' -> ğ, sh -> ş, ch -> ç, tutuq -> ʼ.
Chiqish: har qatorda "so'z<TAB>chastota", so'z bo'yicha tartiblangan.
"""
import glob
import os
import re
import sys
from collections import Counter

TUTUQLAR = "'ʻʼ‘’`´"
TUTUQ = "ʼ"

KIRILL = {
    "а": "a", "б": "b", "в": "v", "г": "g", "ғ": "ğ", "д": "d", "ё": "yo", "ж": "j",
    "з": "z", "и": "i", "й": "y", "к": "k", "қ": "q", "л": "l", "м": "m", "н": "n",
    "о": "o", "ў": "ö", "п": "p", "р": "r", "с": "s", "т": "t", "у": "u", "ф": "f",
    "х": "x", "ҳ": "h", "ч": "ç", "ш": "ş", "щ": "şç", "ъ": TUTUQ, "ь": "", "э": "e",
    "ю": "yu", "я": "ya", "ы": "i",
}
UNLILAR = set("аеёиоуўэюяaeiouö")


def kirilldan(s):
    natija = []
    for i, ch in enumerate(s):
        kichik = ch.lower()
        katta = ch != kichik
        if kichik == "е":
            oldingi = s[i - 1].lower() if i > 0 else ""
            r = "ye" if (i == 0 or oldingi in UNLILAR or oldingi in "ъь") else "e"
        elif kichik == "ц":
            r = "s" if i == 0 else "ts"
        elif kichik in KIRILL:
            r = KIRILL[kichik]
        else:
            r = ch
            katta = False
        if katta and r:
            keyingi_katta = i + 1 < len(s) and s[i + 1].isupper()
            r = r.upper() if keyingi_katta or len(r) == 1 else r[0].upper() + r[1:]
        natija.append(r)
    return "".join(natija)


def eski_lotindan(s):
    t = "[" + re.escape(TUTUQLAR) + "]"
    s = re.sub("o" + t, "ö", s)
    s = re.sub("O" + t, "Ö", s)
    s = re.sub("g" + t, "ğ", s)
    s = re.sub("G" + t, "Ğ", s)
    s = s.replace("sh", "ş").replace("Sh", "Ş").replace("SH", "Ş").replace("sH", "ş")
    s = s.replace("ch", "ç").replace("Ch", "Ç").replace("CH", "Ç").replace("cH", "ç")
    return re.sub(t, TUTUQ, s)


def ogir(s):
    if re.search("[Ѐ-ӿ]", s):
        s = kirilldan(s)
    return eski_lotindan(s)


RUXSAT = re.compile("^[a-zöğşç" + TUTUQ + "]+$")


def main():
    papka, chiqish = sys.argv[1], sys.argv[2]
    soni = int(sys.argv[3]) if len(sys.argv) > 3 else 120000
    jami = Counter()
    for f in glob.glob(os.path.join(papka, "**", "*-words.txt"), recursive=True):
        korpus = Counter()
        with open(f, encoding="utf-8", errors="ignore") as fh:
            for qator in fh:
                q = qator.rstrip("\n").split("\t")
                if len(q) < 3:
                    continue
                soz, chast = q[1], q[-1]
                if " " in soz or not chast.isdigit():
                    continue
                y = ogir(soz).lower()
                y = y.strip(TUTUQ)
                if len(y) < 1 or len(y) > 24 or not RUXSAT.match(y):
                    continue
                if not re.search("[aeiouö]", y) and len(y) > 1:
                    continue
                korpus[y] += int(chast)
        # har korpusni bir xil og'irlikka keltiramiz (eng kattasi ustun bo'lib ketmasin)
        umumiy = sum(korpus.values()) or 1
        for k, v in korpus.items():
            jami[k] += v * 10_000_000 / umumiy
        print(os.path.basename(f), len(korpus), file=sys.stderr)
    eng = [(k, v) for k, v in jami.most_common(soni) if v >= 0.5]
    eng.sort(key=lambda x: x[0])
    with open(chiqish, "w", encoding="utf-8", newline="\n") as fh:
        for k, v in eng:
            fh.write(f"{k}\t{max(1, int(round(v)))}\n")
    print("yozildi:", len(eng), file=sys.stderr)


if __name__ == "__main__":
    main()

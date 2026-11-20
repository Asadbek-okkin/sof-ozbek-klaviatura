"""Serverdagi tasdiqlangan yangi so'zlarni (qoshimcha.json) ilova lug'atiga qo'shadi.

Ishlatish: python lugat_qosh.py qoshimcha.json assets/lugat.txt
Chiqish (stdout): qo'shilgan so'zlar soni.
"""
import json
import re
import sys

TOZA = re.compile("^[a-zöğşçcwʼ]{2,24}$")


def main():
    q = json.load(open(sys.argv[1], encoding="utf-8"))
    yol = sys.argv[2]
    sozlar = {}
    with open(yol, encoding="utf-8") as f:
        for qator in f:
            w, _, n = qator.rstrip("\n").partition("\t")
            if w:
                sozlar[w] = int(n or 1)
    yangi = 0
    for w in q.get("sozlar", []):
        w = str(w).lower()
        if TOZA.match(w) and w not in sozlar:
            sozlar[w] = 300
            yangi += 1
    with open(yol, "w", encoding="utf-8", newline="\n") as f:
        for w in sorted(sozlar):
            f.write(f"{w}\t{sozlar[w]}\n")
    print(yangi)


if __name__ == "__main__":
    main()

"""Tugma tovushlari (qisqa WAV, 22050 Hz, mono) - res/raw/ ga yoziladi.

Ishlatish: python tovush_yasash.py <res/raw papkasi>
"""
import math
import os
import random
import struct
import sys
import wave

SR = 22050
random.seed(7)


def yoz(yol, namunalar):
    with wave.open(yol, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32000)) for s in namunalar))


def n(ms):
    return int(SR * ms / 1000)


def yumshoq():  # past ovozli yumshoq "tik"
    out, prev = [], 0.0
    for i in range(n(18)):
        x = random.uniform(-1, 1)
        prev = prev * 0.8 + x * 0.2  # past chastota filtri
        out.append(prev * 1.6 * math.exp(-i / n(4)))
    return out


def mexanik():  # mexanik klaviatura: ikki zarba
    out = []
    for i in range(n(45)):
        t = i / SR
        z1 = random.uniform(-1, 1) * math.exp(-i / n(2.5))
        z2 = random.uniform(-1, 1) * math.exp(-max(0, i - n(22)) / n(3)) * (1 if i >= n(22) else 0) * 0.6
        ton = math.sin(2 * math.pi * 2400 * t) * math.exp(-i / n(6)) * 0.3
        out.append((z1 + z2) * 0.7 + ton)
    return out


def yozuv_mashinkasi():  # keskin "chiq"
    out = []
    for i in range(n(60)):
        t = i / SR
        z = random.uniform(-1, 1) * math.exp(-i / n(5))
        metall = (math.sin(2 * math.pi * 3100 * t) + 0.6 * math.sin(2 * math.pi * 4700 * t)) * math.exp(-i / n(18)) * 0.25
        out.append(z * 0.8 + metall)
    return out


def pufak():  # "pop"
    out, faza = [], 0.0
    for i in range(n(55)):
        f = 900 * math.exp(-i / n(18)) + 180
        faza += 2 * math.pi * f / SR
        out.append(math.sin(faza) * math.exp(-i / n(16)) * 0.8)
    return out


def tomchi():  # suv tomchisi
    out, faza = [], 0.0
    for i in range(n(80)):
        f = 600 + 1400 * (i / n(80)) ** 0.5
        faza += 2 * math.pi * f / SR
        out.append(math.sin(faza) * math.exp(-i / n(22)) * 0.7)
    return out


def nota():  # yumshoq qo'ng'iroq
    out = []
    for i in range(n(110)):
        t = i / SR
        s = math.sin(2 * math.pi * 1046 * t) + 0.35 * math.sin(2 * math.pi * 2093 * t)
        out.append(s * math.exp(-i / n(30)) * 0.45 * min(1, i / n(2)))
    return out


if __name__ == "__main__":
    papka = sys.argv[1]
    os.makedirs(papka, exist_ok=True)
    for nom, fn in [("tovush_yumshoq", yumshoq), ("tovush_mexanik", mexanik), ("tovush_mashinka", yozuv_mashinkasi),
                    ("tovush_pufak", pufak), ("tovush_tomchi", tomchi), ("tovush_nota", nota)]:
        yoz(os.path.join(papka, nom + ".wav"), fn())
        print(nom)

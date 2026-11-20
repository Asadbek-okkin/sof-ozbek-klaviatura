package uz.okkin.klaviatura;

import java.util.ArrayList;
import java.util.List;

/**
 * Klaviatura sahifalari. Harflar joylashuvi inglizcha QWERTY bilan bir xil,
 * faqat o'zbek alifbosida yo'q harflar o'rniga yangilari:
 *   W -> Ö,  C -> Ç,  qatorlar oxirida Ş va Ğ.
 * 2026-yilgi alifbo: 28 harf (C va W yo'q; Ş Ç Ğ Ö bor) + tutuq belgisi ʼ.
 */
final class Tartib {
    private static final float R = 0.9375f; // 3-qatordagi 8 ta belgi eni
    static final int SHIFT = -1, BELGI = -2, HARF = -3, ENTER = -4, OCHIR = -5,
            BELGI2 = -6, EMOJI = -7, TIL = -101;
    static final int T_HARF = 0, T_FUNK = 1, T_ENTER = 2, T_BOSHLIQ = 3;

    static final class Tugma {
        final String label;
        final String chiqish;   // yoziladigan matn (maxsus tugmada null)
        final int kod;
        final float en;
        final int tur;
        final String[] alt;     // bosib turganda chiqadiganlar
        float x, y, w, h;

        Tugma(String label, String chiqish, int kod, float en, int tur, String alt) {
            this.label = label;
            this.chiqish = chiqish;
            this.kod = kod;
            this.en = en;
            this.tur = tur;
            this.alt = (alt == null || alt.isEmpty()) ? null : alt.split(" ");
        }
    }

    static final class Qator {
        final List<Tugma> t = new ArrayList<>();
        final float bal; // dp
        float chet;      // chap va o'ngdan bo'sh joy (tugma eni birligida)

        Qator(float bal, Tugma... tugmalar) {
            this.bal = bal;
            for (Tugma k : tugmalar) t.add(k);
        }

        Qator chet(float c) { chet = c; return this; }
    }

    private static Tugma h(String s, String alt) {
        return new Tugma(s, s, s.codePointAt(0), 1f, T_HARF, alt);
    }

    private static Tugma h(String s, float en, String alt) {
        return new Tugma(s, s, s.codePointAt(0), en, T_HARF, alt);
    }

    private static Tugma f(String label, int kod, float en) {
        return new Tugma(label, null, kod, en, T_FUNK, null);
    }

    private static Tugma boshliq(String label) {
        return new Tugma(label, " ", 32, 4f, T_BOSHLIQ, null);
    }

    private static Tugma enter() {
        return new Tugma("↵", null, ENTER, 1.5f, T_ENTER, null);
    }


    static final int UZ = 0, EN = 1, RU = 2;
    static final String[] TIL_NOMI = {"Özbekça", "English", "Русский"};

    /**
     * Harflar sahifasi. Barcha harf tugmalari bir xil enli.
     * O'zbekchada harflarni bosib turish yo'q - chalg'itmasin (foydalanuvchi talabi).
     */
    static List<Qator> harflar(int til, boolean raqamlar) {
        String q1, q2, q3;
        float chet2, funkEn;
        switch (til) {
            case EN:
                q1 = "q w e r t y u i o p"; q2 = "a s d f g h j k l"; q3 = "z x c v b n m";
                chet2 = 0.5f; funkEn = 1.5f;
                break;
            case RU:
                q1 = "й ц у к е н г ш щ з х"; q2 = "ф ы в а п р о л д ж э"; q3 = "я ч с м и т ь б ю";
                chet2 = 0f; funkEn = 1f;
                break;
            default:
                q1 = "q w e r t y u i o ö p"; q2 = "a s d f g h j k l ş"; q3 = "z x c ç v b n m ğ";
                chet2 = 0.5f; funkEn = 1f;
                break;
        }
        float bal = raqamlar ? 56 : 60;
        List<Qator> s = new ArrayList<>();
        if (raqamlar) {
            s.add(new Qator(46,
                    h("1", "¹ ½ ⅓ ¼"), h("2", "² ⅔"), h("3", "³ ¾"), h("4", null), h("5", null),
                    h("6", null), h("7", null), h("8", null), h("9", null), h("0", "° ∅")));
        }
        Qator r1 = new Qator(bal);
        String[] h1 = q1.split(" ");
        for (int i = 0; i < h1.length; i++) {
            String alt = raqamlar || i >= 10 ? null : String.valueOf((i + 1) % 10);
            if (til == RU && h1[i].equals("е")) alt = alt == null ? "ё" : alt + " ё";
            r1.t.add(h(h1[i], alt));
        }
        s.add(r1);
        Qator r2 = new Qator(bal);
        for (String x : q2.split(" ")) r2.t.add(h(x, null));
        s.add(r2.chet(chet2));
        Qator r3 = new Qator(bal);
        r3.t.add(f("⇧", SHIFT, funkEn == 1f ? 1.2f : funkEn));
        for (String x : q3.split(" ")) r3.t.add(h(x, til == RU && x.equals("ь") ? "ъ" : null));
        r3.t.add(f("⌫", OCHIR, funkEn == 1f ? 1.8f : funkEn)); // ⌫ kattaroq (foydalanuvchi talabi)
        s.add(r3);
        s.add(new Qator(54,
                f("?123", BELGI, 1.5f),
                new Tugma("☺", null, EMOJI, 1f, T_FUNK, til == UZ ? ", ʼ" : ","),
                f("🌐", TIL, 1f),
                boshliq(TIL_NOMI[til]),
                new Tugma(".", ".", '.', 1f, T_HARF, ", ? ! ʼ ' \" : ; - …"),
                enter()));
        return s;
    }


    static List<Qator> belgi1(float B) {
        List<Qator> s = new ArrayList<>();
        s.add(new Qator(B,
                h("1", null), h("2", null), h("3", null), h("4", null), h("5", null),
                h("6", null), h("7", null), h("8", null), h("9", null), h("0", null)));
        s.add(new Qator(B,
                h("@", null), h("#", "№"), h("$", "so'm € £ ¥ ₽ ¢"), h("_", null), h("&", null),
                h("-", "– — ·"), h("+", "±"), h("(", "[ { <"), h(")", "] } >"), h("/", "\\ |")));
        s.add(new Qator(B,
                f("=\\<", BELGI2, 1.25f),
                h("*", R, "★ †"), h("\"", R, "« » „ “ ”"), h("ʼ", R, "' ‘ ’"), h("'", R, "‘ ’ ʼ"),
                h(":", R, null), h(";", R, null), h("!", R, "¡"), h("?", R, "¿"),
                f("⌫", OCHIR, 1.25f)));
        s.add(new Qator(B,
                f("ABC", HARF, 1.5f),
                h(",", null),
                f("☺", EMOJI, 1f),
                boshliq(""),
                h(".", "…"),
                enter()));
        return s;
    }

    static List<Qator> belgi2(float B) {
        List<Qator> s = new ArrayList<>();
        s.add(new Qator(B,
                h("~", null), h("`", null), h("|", null), h("•", null), h("√", null),
                h("π", null), h("÷", null), h("×", null), h("¶", null), h("∆", null)));
        s.add(new Qator(B,
                h("£", null), h("¢", null), h("€", null), h("¥", null), h("^", null),
                h("°", null), h("=", "≠ ≈"), h("{", null), h("}", null), h("\\", null)));
        s.add(new Qator(B,
                f("?123", BELGI, 1.25f),
                h("%", R, "‰"), h("©", R, null), h("®", R, null), h("™", R, null),
                h("✓", R, "✔ ✗"), h("[", R, null), h("]", R, null), h("№", R, null),
                f("⌫", OCHIR, 1.25f)));
        s.add(new Qator(B,
                f("ABC", HARF, 1.5f),
                h("<", "≤ «"),
                h(">", "≥ »"),
                boshliq(""),
                h(".", "…"),
                enter()));
        return s;
    }
}

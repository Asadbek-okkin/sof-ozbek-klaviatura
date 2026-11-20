package uz.okkin.klaviatura;

/**
 * Kirill va eski lotin yozuvini yangi alifboga o'girish.
 *   o' -> ö, g' -> ğ, sh -> ş, ch -> ç, qolgan tutuqlar -> ʼ
 *   ў -> ö, ғ -> ğ, ш -> ş, ч -> ç, қ -> q, ҳ -> h ...
 */
final class Ogir {
    static final char TUTUQ = 'ʼ';
    private static final String TUTUQLAR = "'ʻʼ‘’`´";

    private Ogir() { }

    static String yangiga(CharSequence matn) {
        String s = matn.toString();
        if (kirillBor(s)) s = kirilldan(s);
        return eskiLotindan(s);
    }

    static boolean kirillBor(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'Ѐ' && c <= 'ӿ') return true;
        }
        return false;
    }

    private static boolean tutuqmi(char c) {
        return TUTUQLAR.indexOf(c) >= 0;
    }

    static String eskiLotindan(String s) {
        StringBuilder b = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char k = i + 1 < n ? s.charAt(i + 1) : 0;
            char k2 = i + 2 < n ? s.charAt(i + 2) : 0;
            boolean oShart = (c == 'o' || c == 'O') && Character.isLetter(k2);   // o' faqat so'z ichida
            boolean gShart = c == 'g' || c == 'G';                                  // tog', bog' - oxirida ham
            if ((oShart || gShart) && k != 0 && tutuqmi(k)) {
                b.append(c == 'o' ? 'ö' : c == 'O' ? 'Ö' : c == 'g' ? 'ğ' : 'Ğ');
                i++;
                continue;
            }
            if ((c == 's' || c == 'S') && (k == 'h' || k == 'H')) {
                b.append(c == 's' ? 'ş' : 'Ş');
                i++;
                continue;
            }
            if ((c == 'c' || c == 'C') && (k == 'h' || k == 'H')) {
                b.append(c == 'c' ? 'ç' : 'Ç');
                i++;
                continue;
            }
            if (tutuqmi(c)) {
                // so'z ichidagi tutuq -> ʼ ; qo'shtirnoq vazifasidagilar o'z holicha
                boolean oldHarf = i > 0 && Character.isLetter(s.charAt(i - 1));
                boolean keyinHarf = k != 0 && Character.isLetter(k);
                b.append(oldHarf && keyinHarf ? TUTUQ : c);
                continue;
            }
            b.append(c);
        }
        return b.toString();
    }

    private static final String UNLI = "аеёиоуўэюяАЕЁИОУЎЭЮЯ";

    static String kirilldan(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            char kichik = Character.toLowerCase(ch);
            boolean katta = ch != kichik;
            char old = i > 0 ? s.charAt(i - 1) : ' ';
            boolean sozBoshi = !Character.isLetter(old);
            String r;
            switch (kichik) {
                case 'а': r = "a"; break;
                case 'б': r = "b"; break;
                case 'в': r = "v"; break;
                case 'г': r = "g"; break;
                case 'ғ': r = "ğ"; break;
                case 'д': r = "d"; break;
                case 'е': r = (sozBoshi || UNLI.indexOf(old) >= 0 || old == 'ъ' || old == 'ь'
                        || old == 'Ъ' || old == 'Ь') ? "ye" : "e"; break;
                case 'ё': r = "yo"; break;
                case 'ж': r = "j"; break;
                case 'з': r = "z"; break;
                case 'и': r = "i"; break;
                case 'й': r = "y"; break;
                case 'к': r = "k"; break;
                case 'қ': r = "q"; break;
                case 'л': r = "l"; break;
                case 'м': r = "m"; break;
                case 'н': r = "n"; break;
                case 'о': r = "o"; break;
                case 'ў': r = "ö"; break;
                case 'п': r = "p"; break;
                case 'р': r = "r"; break;
                case 'с': r = "s"; break;
                case 'т': r = "t"; break;
                case 'у': r = "u"; break;
                case 'ф': r = "f"; break;
                case 'х': r = "x"; break;
                case 'ҳ': r = "h"; break;
                case 'ц': r = sozBoshi ? "s" : "ts"; break;
                case 'ч': r = "ç"; break;
                case 'ш': r = "ş"; break;
                case 'щ': r = "şç"; break;
                case 'ъ': r = String.valueOf(TUTUQ); katta = false; break;
                case 'ь': r = ""; break;
                case 'э': r = "e"; break;
                case 'ю': r = "yu"; break;
                case 'я': r = "ya"; break;
                case 'ы': r = "i"; break;
                default: r = String.valueOf(ch); katta = false; break;
            }
            if (katta && !r.isEmpty()) {
                char keyingi = i + 1 < n ? s.charAt(i + 1) : ' ';
                boolean hammasiKatta = Character.isUpperCase(keyingi);
                if (r.length() == 1 || hammasiKatta) r = r.toUpperCase();
                else r = Character.toUpperCase(r.charAt(0)) + r.substring(1);
            }
            b.append(r);
        }
        return b.toString();
    }
}

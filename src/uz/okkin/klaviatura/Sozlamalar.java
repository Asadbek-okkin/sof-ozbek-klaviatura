package uz.okkin.klaviatura;

import android.content.Context;
import android.content.SharedPreferences;

/** Foydalanuvchi sozlamalari (bitta joyda). */
final class Sozlamalar {
    static final String MAVZU = "mavzu", BALANDLIK = "balandlik", RAQAMLAR = "raqamlar",
            TOVUSH = "tovush", TEBRANISH = "tebranish", TAKLIFLAR = "takliflar",
            AVTOTUZATISH = "avtotuzatish", AVTO_KATTA = "avto_katta", IKKI_BOSHLIQ = "ikki_boshliq",
            INGLIZCHA = "inglizcha", RUSCHA = "ruscha", OGIRISH_YOZUV = "ogirish_yozuv",
            HISSA = "hissa"; // lug'atni boyitishga yordam (yangi so'zlarni anonim yuborish)

    static final String[] MAVZULAR = {"avto", "qorongi", "yorug", "kok", "qora"};
    static final String[] MAVZU_NOMLARI = {"Telefonga moslaşsin", "Qorong'i", "Yorug'", "Tungi kōk", "Qop-qora (AMOLED)"};

    private Sozlamalar() { }

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("sozlamalar", Context.MODE_PRIVATE);
    }

    static boolean b(Context c, String k) {
        boolean def = !(TOVUSH.equals(k));
        return sp(c).getBoolean(k, def);
    }

    static void b(Context c, String k, boolean v) {
        sp(c).edit().putBoolean(k, v).putLong("ozgardi", System.currentTimeMillis()).apply();
    }

    /** 0 kichik, 1 oddiy, 2 katta */
    static int balandlik(Context c) { return sp(c).getInt(BALANDLIK, 1); }

    /** 0 yo'q, 1 yengil, 2 kuchli */
    static int tebranish(Context c) { return sp(c).getInt(TEBRANISH, 1); }

    static String mavzu(Context c) { return sp(c).getString(MAVZU, "avto"); }

    static void i(Context c, String k, int v) {
        sp(c).edit().putInt(k, v).putLong("ozgardi", System.currentTimeMillis()).apply();
    }

    static void s(Context c, String k, String v) {
        sp(c).edit().putString(k, v).putLong("ozgardi", System.currentTimeMillis()).apply();
    }

    static long ozgardi(Context c) { return sp(c).getLong("ozgardi", 0); }
}

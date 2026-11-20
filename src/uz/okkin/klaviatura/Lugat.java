package uz.okkin.klaviatura;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Yangi alifbodagi o'zbekcha lug'at: so'z takliflari va xatoni tuzatish. */
final class Lugat {
    private static final String HARFLAR = "abdefghijklmnopqrstuvxyzöğşçcwʼ";

    private static volatile Lugat bitta;
    private volatile boolean tayyor;
    private String[] sozlar = new String[0];
    private int[] chastota = new int[0];
    private final Map<String, Integer> organgan = new HashMap<>();
    private final SharedPreferences sp;
    /** Serverdan kelgan qo'shimcha so'zlar (boshqa foydalanuvchilar yozgan, tasdiqlangan). */
    private volatile java.util.TreeSet<String> qoshimcha = new java.util.TreeSet<>();
    private static final int QOSHIMCHA_CHASTOTA = 300;

    /** Qo'shimcha so'zlarni o'rnatish (Sinxron chaqiradi). */
    void qoshimchaniQoy(java.util.Collection<String> s) {
        qoshimcha = new java.util.TreeSet<>(s);
    }

    /** Asosiy (ilova ichidagi) lug'atda bormi - faqat shu. */
    boolean bazadaBormi(String w) {
        String k = w.toLowerCase();
        return (tayyor && indeks(k) >= 0) || qoshimcha.contains(k);
    }

    /** O'rganilgan, lekin lug'atda yo'q so'zlar (kamida n marta yozilgan) - lug'atni boyitish uchun. */
    List<String> yangiSozlar(int kamida) {
        List<String> r = new ArrayList<>();
        if (!tayyor) return r;
        for (Map.Entry<String, Integer> e : organgan.entrySet()) {
            if (e.getValue() >= kamida && !bazadaBormi(e.getKey())) r.add(e.getKey());
        }
        return r;
    }

    static Lugat ol(Context c) {
        if (bitta == null) {
            synchronized (Lugat.class) {
                if (bitta == null) bitta = new Lugat(c.getApplicationContext());
            }
        }
        return bitta;
    }

    private Lugat(final Context c) {
        sp = c.getSharedPreferences("organgan", Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            if (e.getValue() instanceof Integer) organgan.put(e.getKey(), (Integer) e.getValue());
        }
        new Thread(new Runnable() {
            @Override public void run() { yukla(c); }
        }, "lugat").start();
    }

    private void yukla(Context c) {
        try {
            ArrayList<String> s = new ArrayList<>(130000);
            int[] f = new int[130000];
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open("lugat.txt"), "UTF-8"), 65536);
            String q;
            int n = 0;
            while ((q = r.readLine()) != null) {
                int t = q.indexOf('\t');
                if (t <= 0) continue;
                if (n == f.length) f = Arrays.copyOf(f, n * 2);
                s.add(q.substring(0, t));
                f[n++] = Integer.parseInt(q.substring(t + 1));
            }
            r.close();
            String[] arr = s.toArray(new String[0]);
            // fayl allaqachon tartiblangan; ishonch uchun tekshiramiz
            boolean tartibli = true;
            for (int i = 1; i < arr.length && tartibli; i++) if (arr[i - 1].compareTo(arr[i]) > 0) tartibli = false;
            if (!tartibli) {
                Integer[] idx = new Integer[arr.length];
                for (int i = 0; i < idx.length; i++) idx[i] = i;
                final String[] a = arr;
                Arrays.sort(idx, new java.util.Comparator<Integer>() {
                    @Override public int compare(Integer x, Integer y) { return a[x].compareTo(a[y]); }
                });
                String[] s2 = new String[arr.length];
                int[] f2 = new int[arr.length];
                for (int i = 0; i < idx.length; i++) { s2[i] = arr[idx[i]]; f2[i] = f[idx[i]]; }
                arr = s2;
                f = f2;
            }
            sozlar = arr;
            chastota = Arrays.copyOf(f, arr.length);
            tayyor = true;
        } catch (Exception e) {
            tayyor = false;
        }
    }

    boolean tayyormi() { return tayyor; }

    private int indeks(String w) {
        return Arrays.binarySearch(sozlar, w);
    }

    /** So'z og'irligi: lug'at chastotasi + o'rganilgan so'zlarga katta qo'shimcha. */
    private long ogirlik(String w, int lugatChastota) {
        Integer o = organgan.get(w);
        return lugatChastota + (o == null ? 0 : (long) o * 5000L);
    }

    boolean bormi(String w) {
        String k = w.toLowerCase();
        if (organgan.containsKey(k) && organgan.get(k) >= 2) return true;
        return (tayyor && indeks(k) >= 0) || qoshimcha.contains(k);
    }

    /** Shu bilan boshlanadigan eng ko'p ishlatiladigan so'zlar. */
    List<String> takliflar(String boshi, int soni) {
        String p = boshi.toLowerCase();
        List<String> natija = new ArrayList<>();
        if (p.isEmpty()) return natija;
        String[] eng = new String[soni];
        long[] engO = new long[soni];
        Set<String> korilgan = new HashSet<>();
        if (tayyor) {
            int i = indeks(p);
            if (i < 0) i = -i - 1;
            int chegara = 0;
            for (; i < sozlar.length && sozlar[i].startsWith(p) && chegara < 60000; i++, chegara++) {
                joyla(eng, engO, sozlar[i], ogirlik(sozlar[i], chastota[i]));
                korilgan.add(sozlar[i]);
            }
        }
        for (String w : qoshimcha.subSet(p, p + '￿')) {
            if (!korilgan.contains(w)) { joyla(eng, engO, w, ogirlik(w, QOSHIMCHA_CHASTOTA)); korilgan.add(w); }
        }
        for (Map.Entry<String, Integer> e : organgan.entrySet()) {
            String w = e.getKey();
            if (e.getValue() >= 2 && w.startsWith(p) && !korilgan.contains(w)) {
                joyla(eng, engO, w, ogirlik(w, 0));
            }
        }
        for (String w : eng) if (w != null) natija.add(w);
        return natija;
    }

    private static void joyla(String[] eng, long[] engO, String w, long o) {
        for (int j = 0; j < eng.length; j++) {
            if (eng[j] == null || o > engO[j]) {
                for (int k = eng.length - 1; k > j; k--) { eng[k] = eng[k - 1]; engO[k] = engO[k - 1]; }
                eng[j] = w;
                engO[j] = o;
                return;
            }
        }
    }

    /** Xato yozilgan so'z uchun eng yaqin (1 ta tahrir) to'g'ri so'z, bo'lmasa null. */
    String tuzat(String soz) {
        if (!tayyor) return null;
        String w = soz.toLowerCase();
        if (w.length() < 2 || bormi(w)) return null;
        String eng = null;
        long engO = 0;
        Set<String> nomzodlar = new HashSet<>();
        int n = w.length();
        for (int i = 0; i <= n; i++) {
            String a = w.substring(0, i), b = w.substring(i);
            if (!b.isEmpty()) nomzodlar.add(a + b.substring(1));                       // o'chirish
            if (b.length() > 1) nomzodlar.add(a + b.charAt(1) + b.charAt(0) + b.substring(2)); // almashtirish
            for (int h = 0; h < HARFLAR.length(); h++) {
                char c = HARFLAR.charAt(h);
                if (!b.isEmpty()) nomzodlar.add(a + c + b.substring(1));                // o'rniga
                nomzodlar.add(a + c + b);                                               // qo'shish
            }
        }
        for (String nz : nomzodlar) {
            if (nz.equals(w)) continue;
            int i = indeks(nz);
            long o;
            if (i >= 0) o = ogirlik(nz, chastota[i]);
            else if (qoshimcha.contains(nz)) o = ogirlik(nz, QOSHIMCHA_CHASTOTA);
            else continue;
            // eski yozuvdan kelgan odatiy xatolarga ustunlik (o->ö, s->ş, c->ç, g->ğ)
            if (yaqinHarf(w, nz)) o *= 4;
            if (o > engO) { engO = o; eng = nz; }
        }
        // juda kam uchraydigan so'zga tuzatmaymiz
        if (eng != null && engO < 20) return null;
        return eng;
    }

    private static boolean yaqinHarf(String w, String nz) {
        if (w.length() != nz.length()) return false;
        for (int i = 0; i < w.length(); i++) {
            char a = w.charAt(i), b = nz.charAt(i);
            if (a == b) continue;
            return (a == 'o' && b == 'ö') || (a == 's' && b == 'ş') || (a == 'c' && b == 'ç')
                    || (a == 'g' && b == 'ğ') || (a == 'ö' && b == 'o') || (a == 'ş' && b == 's');
        }
        return false;
    }

    /** Foydalanuvchi yozgan so'zni eslab qolish. */
    void orgat(String soz) {
        String w = soz.toLowerCase();
        if (w.length() < 2 || w.length() > 24) return;
        for (int i = 0; i < w.length(); i++) if (!Character.isLetter(w.charAt(i)) && w.charAt(i) != Ogir.TUTUQ) return;
        Integer v = organgan.get(w);
        int yangi = v == null ? 1 : Math.min(v + 1, 1000);
        organgan.put(w, yangi);
        sp.edit().putInt(w, yangi).apply();
    }

    void unut(String soz) {
        String w = soz.toLowerCase();
        organgan.remove(w);
        sp.edit().remove(w).apply();
    }

    void hammasiniUnut() {
        organgan.clear();
        sp.edit().clear().apply();
    }
}

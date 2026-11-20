package uz.okkin.klaviatura;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

/** Tugma tovushlari: "standart" - telefonning o'z ovozi, qolganlari ilova ichida. */
final class Tovushlar {
    static final String[] NOMLAR = {"standart", "yumshoq", "mexanik", "mashinka", "pufak", "tomchi", "nota"};
    static final String[] KORINISH = {"Standart (telefon)", "Yumşoq", "Mexanik klaviatura", "Yozuv maşinkasi",
            "Pufak", "Suv tomçisi", "Qōng'iroq"};
    private static final int[] FAYL = {0, R.raw.tovush_yumshoq, R.raw.tovush_mexanik, R.raw.tovush_mashinka,
            R.raw.tovush_pufak, R.raw.tovush_tomchi, R.raw.tovush_nota};

    private final AudioManager am;
    private SoundPool pool;
    private int yuklangan = -1, idSound;
    private final Context c;

    Tovushlar(Context c) {
        this.c = c.getApplicationContext();
        am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    }

    static int indeks(String nom) {
        for (int i = 0; i < NOMLAR.length; i++) if (NOMLAR[i].equals(nom)) return i;
        return 0;
    }

    private void tayyorla(int i) {
        if (i == yuklangan || i <= 0) return;
        if (pool == null) {
            if (Build.VERSION.SDK_INT >= 21) {
                pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            }
        }
        if (pool == null) return;
        idSound = pool.load(c, FAYL[i], 1);
        yuklangan = i;
    }

    /** kod: tugma kodi (o'chirish/bo'shliq/enter uchun standartda boshqacha ovoz). */
    void chal(String nom, int kod) {
        int i = indeks(nom);
        if (i == 0 || pool == null && Build.VERSION.SDK_INT < 21) {
            if (am == null) return;
            int fx;
            if (kod == Tartib.OCHIR) fx = AudioManager.FX_KEYPRESS_DELETE;
            else if (kod == ' ') fx = AudioManager.FX_KEYPRESS_SPACEBAR;
            else if (kod == Tartib.ENTER) fx = AudioManager.FX_KEYPRESS_RETURN;
            else fx = AudioManager.FX_KEYPRESS_STANDARD;
            am.playSoundEffect(fx, 0.5f);
            return;
        }
        tayyorla(i);
        if (pool != null) {
            float balandlik = kod == Tartib.OCHIR || kod == ' ' ? 0.55f : 0.7f;
            pool.play(idSound, balandlik, balandlik, 1, 0, kod == Tartib.OCHIR ? 0.85f : 1f);
        }
    }

    /** Sozlamalarda tanlanganda oldindan eshittirish (birinchi yuklashda biroz kutadi). */
    void namuna(final String nom) {
        int i = indeks(nom);
        if (i == 0) { chal(nom, 'a'); return; }
        tayyorla(i);
        if (pool == null) return;
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override public void onLoadComplete(SoundPool p, int id, int st) { if (st == 0) p.play(id, 0.8f, 0.8f, 1, 0, 1f); }
        });
        pool.play(idSound, 0.8f, 0.8f, 1, 0, 1f);
    }

    void yop() {
        if (pool != null) { pool.release(); pool = null; yuklangan = -1; }
    }
}

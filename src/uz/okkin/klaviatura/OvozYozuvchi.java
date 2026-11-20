package uz.okkin.klaviatura;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Sof o'zbekcha ovozli yozish: ovoz yozib olinadi va serverda (Gemini) o'zbek tiliga moslab matnga
 * aylantiriladi. Gapirib bo'lgach 2 soniya jim tursangiz o'zi to'xtaydi (yoki 🟥 ni bosing).
 * Server ishlamasa - telefonning o'z ovoz tanish xizmatiga o'tiladi (zaxira).
 */
final class OvozYozuvchi {

    private static final int OVOZ_CHEGARA = 2200;   // shundan baland - gapiryapti
    private static final long JIMLIK_MS = 1800, MAKS_MS = 60000, BOSHLASH_KUT_MS = 7000;

    private final Context c;
    private final Ovoz.Tinglovchi t;
    private final Handler h = new Handler(Looper.getMainLooper());
    private MediaRecorder mr;
    private File fayl;
    private boolean ishlayapti, gapirdi;
    private long boshlandi, oxirgiOvoz;
    private String til = "uz";
    private Runnable zaxira;

    OvozYozuvchi(Context c, Ovoz.Tinglovchi t) {
        this.c = c.getApplicationContext();
        this.t = t;
    }

    boolean ishlayaptimi() { return ishlayapti; }

    /** @return false bo'lsa mikrofonni ochib bo'lmadi */
    boolean boshla(String til, Runnable zaxira) {
        this.til = til;
        this.zaxira = zaxira;
        bekor();
        try {
            fayl = new File(c.getCacheDir(), "ovoz.aac");
            mr = new MediaRecorder();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mr.setAudioChannels(1);
            mr.setAudioSamplingRate(16000);
            mr.setAudioEncodingBitRate(32000);
            mr.setOutputFile(fayl.getAbsolutePath());
            mr.prepare();
            mr.start();
        } catch (Exception e) {
            bekor();
            return false;
        }
        ishlayapti = true;
        gapirdi = false;
        boshlandi = oxirgiOvoz = System.currentTimeMillis();
        h.post(kuzat);
        return true;
    }

    private final Runnable kuzat = new Runnable() {
        @Override public void run() {
            if (!ishlayapti || mr == null) return;
            long hozir = System.currentTimeMillis();
            int amp = 0;
            try { amp = mr.getMaxAmplitude(); } catch (Exception ignored) { }
            if (amp > OVOZ_CHEGARA) { gapirdi = true; oxirgiOvoz = hozir; }
            long s = (hozir - boshlandi) / 1000;
            t.ovozHolat("🎤 " + (gapirdi ? "Eşitilmoqda" : "Gapiring") + "...  " + s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60);
            if ((gapirdi && hozir - oxirgiOvoz > JIMLIK_MS) || hozir - boshlandi > MAKS_MS) { toxtat(); return; }
            if (!gapirdi && hozir - boshlandi > BOSHLASH_KUT_MS) { bekor(); t.ovozXato("Hech narsa eşitilmadi"); return; }
            h.postDelayed(this, 120);
        }
    };

    /** Yozishni tugatib, matnga aylantirishga yuboradi. */
    void toxtat() {
        if (!ishlayapti) return;
        ishlayapti = false;
        h.removeCallbacks(kuzat);
        try { mr.stop(); } catch (Exception ignored) { }
        try { mr.release(); } catch (Exception ignored) { }
        mr = null;
        if (!gapirdi) { t.ovozXato("Hech narsa eşitilmadi"); return; }
        t.ovozHolat("⏳ Yozilmoqda...");
        final File f = fayl;
        new Thread(new Runnable() {
            @Override public void run() {
                String matn = null;
                String xato = null;
                try {
                    matn = yubor(f);
                } catch (Exception e) {
                    xato = e.getMessage();
                }
                try { f.delete(); } catch (Exception ignored) { }
                final String m = matn, x = xato;
                h.post(new Runnable() {
                    @Override public void run() {
                        if (m != null && !m.trim().isEmpty()) t.ovozNatija(m.trim());
                        else if (m != null) t.ovozXato("Hech narsa eşitilmadi");
                        else if (zaxira != null) zaxira.run();   // server ishlamadi - telefonning o'z xizmati
                        else t.ovozXato(x == null ? "Ovozni yozib bōlmadi" : x);
                    }
                });
            }
        }, "ovoz-yubor").start();
    }

    void bekor() {
        ishlayapti = false;
        h.removeCallbacks(kuzat);
        if (mr != null) {
            try { mr.stop(); } catch (Exception ignored) { }
            try { mr.release(); } catch (Exception ignored) { }
            mr = null;
        }
    }

    private String yubor(File f) throws Exception {
        byte[] d = oqi(new FileInputStream(f));
        HttpURLConnection u = (HttpURLConnection) new URL(Sinxron.MANZIL + "/ovoz").openConnection();
        u.setConnectTimeout(10000);
        u.setReadTimeout(40000);
        u.setDoOutput(true);
        u.setRequestMethod("POST");
        u.setRequestProperty("Content-Type", "audio/aac");
        u.setRequestProperty("X-Q", Sinxron.qurilma(c));
        u.setRequestProperty("X-Til", til);
        u.setRequestProperty("X-V", Sinxron.versiya(c));
        u.setFixedLengthStreamingMode(d.length);
        OutputStream o = u.getOutputStream();
        o.write(d);
        o.close();
        int kod = u.getResponseCode();
        if (kod != 200) {
            u.disconnect();
            throw new Exception("server " + kod);
        }
        String s = new String(oqi(u.getInputStream()), "UTF-8");
        u.disconnect();
        return new JSONObject(s).optString("matn", "");
    }

    private static byte[] oqi(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toByteArray();
        } finally {
            in.close();
        }
    }
}

package uz.okkin.klaviatura;

import android.view.Choreographer;
import android.view.animation.Interpolator;

/**
 * O'zimizning animatsiya dvigateli (ekran kadrlari bilan). Android'ning "animatsiyalarni o'chirish"
 * sozlamasi (ko'p Xiaomi/Realme telefonlarda o'chiq turadi) bunga ta'sir qilmaydi - harakat har doim ko'rinadi.
 */
final class Harakat implements Choreographer.FrameCallback {

    interface Qadam { void qadam(float t); }

    private final long davom, kechik;
    private final Interpolator ip;
    private final Qadam q;
    private final Runnable oxiri;
    private final boolean takror;
    private long boshi = -1;
    private boolean toxtadi;

    private Harakat(long davom, long kechik, Interpolator ip, boolean takror, Qadam q, Runnable oxiri) {
        this.davom = Math.max(1, davom);
        this.kechik = kechik;
        this.ip = ip;
        this.takror = takror;
        this.q = q;
        this.oxiri = oxiri;
    }

    /** t: 0 dan 1 gacha (interpolator bilan). */
    static Harakat boshla(long davom, long kechik, Interpolator ip, Qadam q, Runnable oxiri) {
        Harakat h = new Harakat(davom, kechik, ip, false, q, oxiri);
        q.qadam(ip == null ? 0 : ip.getInterpolation(0));
        Choreographer.getInstance().postFrameCallback(h);
        return h;
    }

    /** To'xtatilmaguncha 0->1->0 takrorlanadi (puls). */
    static Harakat takrorla(long davom, long kechik, Qadam q) {
        Harakat h = new Harakat(davom, kechik, null, true, q, null);
        Choreographer.getInstance().postFrameCallback(h);
        return h;
    }

    void toxtat() { toxtadi = true; }

    @Override
    public void doFrame(long nano) {
        if (toxtadi) return;
        long ms = nano / 1000000L;
        if (boshi < 0) boshi = ms + kechik;
        long o = ms - boshi;
        if (o < 0) { Choreographer.getInstance().postFrameCallback(this); return; }
        if (takror) {
            float f = (o % (2 * davom)) / (float) davom;
            float t = f <= 1 ? f : 2 - f;
            q.qadam((float) (0.5 - 0.5 * Math.cos(Math.PI * t)));
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }
        float t = Math.min(1f, o / (float) davom);
        q.qadam(ip == null ? t : ip.getInterpolation(t));
        if (t < 1f) Choreographer.getInstance().postFrameCallback(this);
        else if (oxiri != null) oxiri.run();
    }
}

package uz.okkin.klaviatura;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/**
 * Klaviatura tepasidagi satr (Gboard'dagidek):
 *  - bo'sh paytda asboblar: bufer, tahrir, o'girish, mavzu, sozlamalar, mikrofon;
 *  - yozayotganda 3 ta so'z taklifi + mikrofon;
 *  - qisqa xabar (masalan "Gapiring...").
 */
final class Asboblar extends View {

    interface Tinglovchi {
        void asbob(int qaysi);
        void taklifTanlandi(String soz);
        void ovozToxtat();
    }

    static final int BUFER = 0, TAHRIR = 1, OGIR = 2, MAVZU = 3, SOZLAMA = 4, OVOZ = 5;
    private static final int[] TARTIB = {BUFER, TAHRIR, OGIR, MAVZU, SOZLAMA};

    private static final int R_ASBOB = 0, R_TAKLIF = 1, R_XABAR = 2;

    private final Tinglovchi t;
    private final float d, sp;
    private int rejim = R_ASBOB;
    private final String[] taklif = new String[3];
    private int asosiy = -1;
    private String xabar = "";
    private boolean ovozXabar;
    private int bosilgan = -1; // bosilgan katak

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path yol = new Path();
    private final RectF r = new RectF();

    Asboblar(Context c, Tinglovchi t) {
        super(c);
        this.t = t;
        d = c.getResources().getDisplayMetrics().density;
        sp = c.getResources().getDisplayMetrics().scaledDensity;
        tp.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        setMeasuredDimension(MeasureSpec.getSize(ws), (int) (44 * d));
    }

    void asboblar() {
        rejim = R_ASBOB;
        invalidate();
    }

    void takliflar(List<String> s, int asosiyIndeks) {
        for (int i = 0; i < 3; i++) taklif[i] = i < s.size() ? s.get(i) : null;
        asosiy = asosiyIndeks;
        rejim = R_TAKLIF;
        invalidate();
    }

    void xabar(String s, boolean ovoz) {
        xabar = s;
        ovozXabar = ovoz;
        rejim = R_XABAR;
        invalidate();
    }

    boolean xabarmi() { return rejim == R_XABAR; }

    // ---------- chizish ----------

    private float mikEn() { return 52 * d; }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(KlaviaturaView.FON);
        float w = getWidth(), h = getHeight();
        if (rejim == R_XABAR) {
            tp.setColor(KlaviaturaView.MATN);
            tp.setTextSize(15 * sp);
            tp.setTypeface(Typeface.DEFAULT);
            float maxW = w - (ovozXabar ? mikEn() : 0) - 24 * d;
            String s = TextUtils.ellipsize(xabar, tp, maxW, TextUtils.TruncateAt.START).toString();
            c.drawText(s, (w - (ovozXabar ? mikEn() : 0)) / 2, h / 2 - (tp.descent() + tp.ascent()) / 2, tp);
            if (ovozXabar) {
                float cx = w - mikEn() / 2, cy = h / 2;
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFE53935);
                c.drawCircle(cx, cy, 16 * d, p);
                stroke(0xFFFFFFFF);
                float q = 5 * d;
                p.setStyle(Paint.Style.FILL);
                r.set(cx - q, cy - q, cx + q, cy + q);
                c.drawRoundRect(r, 1.5f * d, 1.5f * d, p);
            }
            return;
        }
        // mikrofon - har doim o'ngda
        float mx = w - mikEn() / 2;
        if (bosilgan == 99) yumaloq(c, mx, h / 2, 19 * d);
        mikrofon(c, mx, h / 2, 20 * d);

        float ishEn = w - mikEn();
        if (rejim == R_TAKLIF) {
            float katak = ishEn / 3;
            for (int i = 0; i < 3; i++) {
                if (bosilgan == i) {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(KlaviaturaView.HARF);
                    r.set(i * katak + 3 * d, 5 * d, (i + 1) * katak - 3 * d, h - 5 * d);
                    c.drawRoundRect(r, 8 * d, 8 * d, p);
                }
                if (i > 0) {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(KlaviaturaView.HINT & 0x55FFFFFF);
                    c.drawRect(i * katak - 0.5f * d, 12 * d, i * katak + 0.5f * d, h - 12 * d, p);
                }
                if (taklif[i] == null) continue;
                tp.setColor(KlaviaturaView.MATN);
                tp.setTextSize(16 * sp);
                tp.setTypeface(i == asosiy ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                String s = TextUtils.ellipsize(taklif[i], tp, katak - 12 * d, TextUtils.TruncateAt.END).toString();
                c.drawText(s, i * katak + katak / 2, h / 2 - (tp.descent() + tp.ascent()) / 2, tp);
            }
            return;
        }
        float katak = ishEn / TARTIB.length;
        for (int i = 0; i < TARTIB.length; i++) {
            float cx = i * katak + katak / 2, cy = h / 2;
            if (bosilgan == i) yumaloq(c, cx, cy, 19 * d);
            ikonka(c, TARTIB[i], cx, cy, 20 * d);
        }
    }

    private void yumaloq(Canvas c, float cx, float cy, float rad) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(KlaviaturaView.HARF);
        c.drawCircle(cx, cy, rad, p);
    }

    private void stroke(int rang) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(rang);
        p.setStrokeWidth(1.8f * d);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    private void mikrofon(Canvas c, float cx, float cy, float s) {
        stroke(KlaviaturaView.MATN);
        r.set(cx - s * 0.17f, cy - s * 0.45f, cx + s * 0.17f, cy + s * 0.12f);
        c.drawRoundRect(r, s * 0.17f, s * 0.17f, p);
        r.set(cx - s * 0.32f, cy - s * 0.25f, cx + s * 0.32f, cy + s * 0.28f);
        c.drawArc(r, 10, 160, false, p);
        c.drawLine(cx, cy + s * 0.28f, cx, cy + s * 0.45f, p);
    }

    private void ikonka(Canvas c, int qaysi, float cx, float cy, float s) {
        stroke(KlaviaturaView.MATN);
        yol.reset();
        switch (qaysi) {
            case BUFER: {
                r.set(cx - s * 0.33f, cy - s * 0.38f, cx + s * 0.33f, cy + s * 0.45f);
                c.drawRoundRect(r, s * 0.08f, s * 0.08f, p);
                p.setStyle(Paint.Style.FILL);
                r.set(cx - s * 0.16f, cy - s * 0.48f, cx + s * 0.16f, cy - s * 0.3f);
                c.drawRoundRect(r, s * 0.05f, s * 0.05f, p);
                stroke(KlaviaturaView.MATN);
                c.drawLine(cx - s * 0.17f, cy - s * 0.05f, cx + s * 0.17f, cy - s * 0.05f, p);
                c.drawLine(cx - s * 0.17f, cy + s * 0.15f, cx + s * 0.17f, cy + s * 0.15f, p);
                break;
            }
            case TAHRIR: { // matn kursori va o'qlar
                c.drawLine(cx, cy - s * 0.42f, cx, cy + s * 0.42f, p);
                c.drawLine(cx - s * 0.12f, cy - s * 0.42f, cx + s * 0.12f, cy - s * 0.42f, p);
                c.drawLine(cx - s * 0.12f, cy + s * 0.42f, cx + s * 0.12f, cy + s * 0.42f, p);
                float a = s * 0.48f, b = s * 0.25f, q = s * 0.12f;
                c.drawLine(cx - a, cy, cx - b, cy, p);
                c.drawLine(cx - a, cy, cx - a + q, cy - q, p);
                c.drawLine(cx - a, cy, cx - a + q, cy + q, p);
                c.drawLine(cx + a, cy, cx + b, cy, p);
                c.drawLine(cx + a, cy, cx + a - q, cy - q, p);
                c.drawLine(cx + a, cy, cx + a - q, cy + q, p);
                break;
            }
            case OGIR: { // "Ö" va ikki o'q
                tp.setColor(KlaviaturaView.MATN);
                tp.setTextSize(s * 0.62f);
                tp.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText("ö", cx + s * 0.18f, cy + s * 0.18f, tp);
                c.drawText("o", cx - s * 0.28f, cy - s * 0.02f, tp);
                float y = cy + s * 0.36f, a = s * 0.45f, q = s * 0.1f;
                c.drawLine(cx - a, y, cx + a, y, p);
                c.drawLine(cx + a, y, cx + a - q, y - q, p);
                c.drawLine(cx + a, y, cx + a - q, y + q, p);
                break;
            }
            case MAVZU: { // palitra
                c.drawCircle(cx, cy, s * 0.42f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - s * 0.17f, cy - s * 0.13f, s * 0.07f, p);
                c.drawCircle(cx + s * 0.05f, cy - s * 0.22f, s * 0.07f, p);
                c.drawCircle(cx + s * 0.2f, cy - s * 0.02f, s * 0.07f, p);
                c.drawCircle(cx - s * 0.08f, cy + s * 0.18f, s * 0.09f, p);
                break;
            }
            case SOZLAMA: { // tishli g'ildirak
                c.drawCircle(cx, cy, s * 0.28f, p);
                c.drawCircle(cx, cy, s * 0.1f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x1 = cx + (float) Math.cos(a) * s * 0.28f, y1 = cy + (float) Math.sin(a) * s * 0.28f;
                    float x2 = cx + (float) Math.cos(a) * s * 0.44f, y2 = cy + (float) Math.sin(a) * s * 0.44f;
                    p.setStrokeWidth(3.2f * d);
                    c.drawLine(x1, y1, x2, y2, p);
                }
                p.setStrokeWidth(1.8f * d);
                break;
            }
            default:
                break;
        }
    }

    // ---------- teginish ----------

    private int katak(float x) {
        float w = getWidth();
        if (rejim == R_XABAR) return ovozXabar && x > w - mikEn() ? 98 : -1;
        if (x > w - mikEn()) return 99;
        float ishEn = w - mikEn();
        if (rejim == R_TAKLIF) return (int) Math.min(2, x / (ishEn / 3));
        return (int) Math.min(TARTIB.length - 1, x / (ishEn / TARTIB.length));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                bosilgan = katak(e.getX());
                invalidate();
                return true;
            case MotionEvent.ACTION_UP: {
                int k = katak(e.getX());
                int b = bosilgan;
                bosilgan = -1;
                invalidate();
                if (k != b) return true;
                if (k == 98) { t.ovozToxtat(); return true; }
                if (k == 99) { t.asbob(OVOZ); return true; }
                if (rejim == R_TAKLIF) {
                    if (k >= 0 && taklif[k] != null) t.taklifTanlandi(taklif[k]);
                } else if (rejim == R_ASBOB && k >= 0) {
                    t.asbob(TARTIB[k]);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                bosilgan = -1;
                invalidate();
                return true;
            default:
                return true;
        }
    }
}

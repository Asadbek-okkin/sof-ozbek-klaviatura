package uz.okkin.klaviatura;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Gboard uslubidagi, o'zi chizadigan klaviatura ko'rinishi. */
final class KlaviaturaView extends View {

    interface Tinglovchi {
        void matn(String s);
        void maxsus(int kod);
        void kursor(int qadam);
        void tilTanlash();
        void tegdi(int kod); // tovush va tebranish
    }

    // Ranglar telefon mavzusiga qarab (qorong'i / yorug') - mavzu() o'rnatadi.
    static int FON, HARF, FUNK, ENTER, BOSILGAN, MATN, HINT, AKSENT,
            KORINISH_FON, KORINISH_MATN, ALT_FON, ALT_TANLOV_MATN;
    static boolean tun = true;

    /** @return true agar mavzu o'zgargan bo'lsa */
    static float koef = 1f; // tugma balandligi (sozlamadan)
    private static String joriyMavzu = "";

    static boolean mavzu(Context c) {
        String tanlov = Sozlamalar.mavzu(c);
        int bk = Sozlamalar.balandlik(c);
        koef = bk == 0 ? 0.88f : bk == 2 ? 1.12f : 1f;
        String nom = tanlov;
        if ("avto".equals(tanlov)) {
            int m = c.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            nom = m == android.content.res.Configuration.UI_MODE_NIGHT_NO ? "yorug" : "qorongi";
        }
        boolean ozgardi = !nom.equals(joriyMavzu) || FON == 0;
        joriyMavzu = nom;
        tun = !"yorug".equals(nom);
        if ("kok".equals(nom)) {
            FON = 0xFF0E1A2B; HARF = 0xFF1D2F48; FUNK = 0xFF2A4467; ENTER = 0xFF2F6FD0;
            BOSILGAN = 0xFF3B5A85; MATN = 0xFFEAF2FF; HINT = 0xFF8FA6C4; AKSENT = 0xFF64B5F6;
            KORINISH_FON = 0xFF3B5A85; KORINISH_MATN = 0xFFFFFFFF; ALT_FON = 0xFF24395A;
            ALT_TANLOV_MATN = 0xFF0E1A2B;
        } else if ("qora".equals(nom)) {
            FON = 0xFF000000; HARF = 0xFF1B1B1D; FUNK = 0xFF2B2C30; ENTER = 0xFF2B2C30;
            BOSILGAN = 0xFF46484D; MATN = 0xFFF1F1F1; HINT = 0xFF8A8A8A; AKSENT = 0xFF8AB4F8;
            KORINISH_FON = 0xFF46484D; KORINISH_MATN = 0xFFFFFFFF; ALT_FON = 0xFF2B2C30;
            ALT_TANLOV_MATN = 0xFF000000;
        } else if (tun) {
            FON = 0xFF1F2023; HARF = 0xFF3C3D41; FUNK = 0xFF4A5563; ENTER = 0xFF4A5563;
            BOSILGAN = 0xFF666A72; MATN = 0xFFE8EAED; HINT = 0xFF9AA0A6; AKSENT = 0xFF8AB4F8;
            KORINISH_FON = 0xFF5F636A; KORINISH_MATN = 0xFFFFFFFF; ALT_FON = 0xFF4A4D53;
            ALT_TANLOV_MATN = 0xFF1F2023;
        } else {
            FON = 0xFFE8EAED; HARF = 0xFFFFFFFF; FUNK = 0xFFC9D2DE; ENTER = 0xFFC9D2DE;
            BOSILGAN = 0xFFB8BEC6; MATN = 0xFF202124; HINT = 0xFF5F6368; AKSENT = 0xFF1A73E8;
            KORINISH_FON = 0xFFFFFFFF; KORINISH_MATN = 0xFF202124; ALT_FON = 0xFFF8F9FA;
            ALT_TANLOV_MATN = 0xFFFFFFFF;
        }
        return ozgardi;
    }

    private final Paint ikonP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path yol = new android.graphics.Path();

    private final float d, sp;
    private final Tinglovchi t;
    private List<Tartib.Qator> sahifa;
    private int katta; // 0 kichik, 1 bitta katta, 2 doimiy katta
    private String enterBelgi = "↵";

    private final Paint fonP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint matnP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Handler h = new Handler(Looper.getMainLooper());
    private final SparseArray<Bosish> faol = new SparseArray<>();

    private final PopupWindow korinish;
    private final TextView korinishMatn;
    private final PopupWindow altOyna;
    private final AltView altView;
    private float altChap, altCw;

    KlaviaturaView(Context c, Tinglovchi t) {
        super(c);
        this.t = t;
        d = c.getResources().getDisplayMetrics().density;
        sp = c.getResources().getDisplayMetrics().scaledDensity;
        setBackgroundColor(FON);
        matnP.setColor(MATN);
        matnP.setTextAlign(Paint.Align.CENTER);
        hintP.setColor(HINT);
        hintP.setTextAlign(Paint.Align.CENTER);
        hintP.setTextSize(10 * sp);

        korinishMatn = new TextView(c);
        korinishMatn.setGravity(Gravity.CENTER);
        korinishMatn.setTextColor(KORINISH_MATN);
        korinishMatn.setTextSize(28);
        GradientDrawable kf = new GradientDrawable();
        kf.setColor(KORINISH_FON);
        kf.setCornerRadius(10 * d);
        if (!tun) kf.setStroke((int) Math.max(1, d), 0xFFD0D3D8);
        korinishMatn.setBackground(kf);
        korinish = popup(korinishMatn);

        altView = new AltView(c);
        altOyna = popup(altView);
    }

    private static PopupWindow popup(View v) {
        PopupWindow p = new PopupWindow(v);
        p.setTouchable(false);
        p.setClippingEnabled(false);
        p.setAnimationStyle(0);
        p.setBackgroundDrawable(null);
        return p;
    }

    // ---------- holat ----------

    void setSahifa(List<Tartib.Qator> s) {
        faolTozala();
        sahifa = s;
        joylash(getWidth());
        requestLayout();
        invalidate();
    }

    int katta() { return katta; }

    void setKatta(int k) {
        if (k != katta) { katta = k; invalidate(); }
    }

    void setEnter(String belgi) {
        enterBelgi = belgi;
        invalidate();
    }

    // ---------- o'lcham ----------

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        float jami = 0;
        if (sahifa != null) for (Tartib.Qator q : sahifa) jami += q.bal;
        setMeasuredDimension(w, (int) (jami * d * koef + 10 * d));
    }

    @Override
    protected void onSizeChanged(int w, int hh, int ow, int oh) {
        joylash(w);
    }

    private void joylash(int w) {
        if (sahifa == null || w == 0) return;
        float pad = 1.5f * d, y = 4 * d;
        for (Tartib.Qator q : sahifa) {
            float jamiEn = 2 * q.chet;
            for (Tartib.Tugma k : q.t) jamiEn += k.en;
            float birlik = (w - 2 * pad) / jamiEn, x = pad + q.chet * birlik, qh = q.bal * d * koef;
            for (Tartib.Tugma k : q.t) {
                k.x = x; k.y = y; k.w = k.en * birlik; k.h = qh;
                x += k.w;
            }
            y += qh;
        }
    }

    private Tartib.Tugma top(float x, float y) {
        if (sahifa == null) return null;
        Tartib.Tugma eng = null;
        float engMasofa = Float.MAX_VALUE;
        for (Tartib.Qator q : sahifa) {
            for (Tartib.Tugma k : q.t) {
                if (x >= k.x && x < k.x + k.w && y >= k.y && y < k.y + k.h) return k;
                float dx = Math.max(Math.max(k.x - x, 0), x - (k.x + k.w));
                float dy = Math.max(Math.max(k.y - y, 0), y - (k.y + k.h));
                float m = dx * dx + dy * dy;
                if (m < engMasofa) { engMasofa = m; eng = k; }
            }
        }
        return eng;
    }

    // ---------- chizish ----------

    private String yorliq(Tartib.Tugma k) {
        switch (k.kod) {
            case Tartib.SHIFT: return katta == 2 ? "⇪" : "⇧";
            case Tartib.ENTER: return enterBelgi;
            default: break;
        }
        if (k.tur == Tartib.T_HARF) return holat(k.label);
        return k.label;
    }

    String holat(String s) {
        return katta > 0 ? s.toUpperCase(Locale.ROOT) : s;
    }

    private boolean bosilganmi(Tartib.Tugma k) {
        for (int i = 0; i < faol.size(); i++) if (faol.valueAt(i).k == k) return true;
        return false;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (sahifa == null) return;
        float gx = 2.25f * d, gy = 4 * d;
        for (Tartib.Qator q : sahifa) {
            for (Tartib.Tugma k : q.t) {
                int rang;
                if (k.tur == Tartib.T_ENTER) rang = ENTER;
                else if (k.tur == Tartib.T_FUNK) rang = FUNK;
                else rang = HARF;
                if (bosilganmi(k)) rang = BOSILGAN;
                r.set(k.x + gx, k.y + gy, k.x + k.w - gx, k.y + k.h - gy);
                boolean pill = k.tur == Tartib.T_ENTER || k.kod == Tartib.BELGI || k.kod == Tartib.HARF;
                float rad = pill ? r.height() / 2 : 8 * d;
                fonP.setColor(rang);
                c.drawRoundRect(r, rad, rad, fonP);

                if (ikonChiz(c, k)) continue;
                String lab = yorliq(k);
                if (lab.isEmpty()) continue;
                float size;
                if (k.tur == Tartib.T_BOSHLIQ) size = 13 * sp;
                else if (k.tur == Tartib.T_HARF) size = (q.bal < 50 ? 19 : 23) * sp;
                else size = lab.length() > 2 ? 15 * sp : 20 * sp;
                matnP.setTextSize(size);
                matnP.setTypeface(k.tur == Tartib.T_FUNK && lab.length() > 2 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                matnP.setColor(k.tur == Tartib.T_BOSHLIQ ? HINT : (k.kod == Tartib.SHIFT && katta == 2 ? AKSENT : MATN));
                float ty = r.centerY() - (matnP.descent() + matnP.ascent()) / 2;
                c.drawText(lab, r.centerX(), ty, matnP);

                if (k.alt != null && k.tur == Tartib.T_HARF && q.bal >= 50) {
                    c.drawText(holat(k.alt[0]), r.right - 8 * d, r.top + 12 * d, hintP);
                }
            }
        }
    }

    /** Gboard'dagidek chiziqli ikonkalar. r - tugma to'rtburchagi. */
    private boolean ikonChiz(Canvas c, Tartib.Tugma k) {
        int kod = k.kod;
        if (kod == Tartib.ENTER && !"↵".equals(enterBelgi)) return false;
        if (kod != Tartib.SHIFT && kod != Tartib.OCHIR && kod != Tartib.TIL
                && kod != Tartib.EMOJI && kod != Tartib.ENTER) return false;
        float s = (kod == Tartib.OCHIR ? 27 : 22) * d, cx = r.centerX(), cy = r.centerY();
        ikonP.setColor(MATN);
        ikonP.setStyle(Paint.Style.STROKE);
        ikonP.setStrokeWidth(1.9f * d);
        ikonP.setStrokeCap(Paint.Cap.ROUND);
        ikonP.setStrokeJoin(Paint.Join.ROUND);
        yol.reset();
        switch (kod) {
            case Tartib.SHIFT: {
                yol.moveTo(cx, cy - s * 0.45f);
                yol.lineTo(cx + s * 0.45f, cy + s * 0.02f);
                yol.lineTo(cx + s * 0.22f, cy + s * 0.02f);
                yol.lineTo(cx + s * 0.22f, cy + s * 0.38f);
                yol.lineTo(cx - s * 0.22f, cy + s * 0.38f);
                yol.lineTo(cx - s * 0.22f, cy + s * 0.02f);
                yol.lineTo(cx - s * 0.45f, cy + s * 0.02f);
                yol.close();
                if (katta > 0) {
                    ikonP.setStyle(Paint.Style.FILL_AND_STROKE);
                    if (katta == 2) ikonP.setColor(AKSENT);
                }
                c.drawPath(yol, ikonP);
                if (katta == 2) {
                    ikonP.setStyle(Paint.Style.STROKE);
                    c.drawLine(cx - s * 0.22f, cy + s * 0.55f, cx + s * 0.22f, cy + s * 0.55f, ikonP);
                }
                return true;
            }
            case Tartib.OCHIR: {
                float hy = s * 0.36f;
                yol.moveTo(cx - s * 0.52f, cy);
                yol.lineTo(cx - s * 0.22f, cy - hy);
                yol.lineTo(cx + s * 0.52f, cy - hy);
                yol.lineTo(cx + s * 0.52f, cy + hy);
                yol.lineTo(cx - s * 0.22f, cy + hy);
                yol.close();
                c.drawPath(yol, ikonP);
                float xx = cx + s * 0.12f, q = s * 0.14f;
                c.drawLine(xx - q, cy - q, xx + q, cy + q, ikonP);
                c.drawLine(xx - q, cy + q, xx + q, cy - q, ikonP);
                return true;
            }
            case Tartib.ENTER: {
                float o = s * 0.42f, py = cy + s * 0.12f;
                yol.moveTo(cx + o, cy - s * 0.38f);
                yol.lineTo(cx + o, py);
                yol.lineTo(cx - o, py);
                c.drawPath(yol, ikonP);
                c.drawLine(cx - o, py, cx - o + s * 0.24f, py - s * 0.24f, ikonP);
                c.drawLine(cx - o, py, cx - o + s * 0.24f, py + s * 0.24f, ikonP);
                return true;
            }
            case Tartib.TIL: {
                float rr = s * 0.45f;
                c.drawCircle(cx, cy, rr, ikonP);
                RectF e = new RectF(cx - rr * 0.45f, cy - rr, cx + rr * 0.45f, cy + rr);
                c.drawOval(e, ikonP);
                c.drawLine(cx - rr, cy, cx + rr, cy, ikonP);
                c.drawLine(cx - rr * 0.85f, cy - rr * 0.5f, cx + rr * 0.85f, cy - rr * 0.5f, ikonP);
                c.drawLine(cx - rr * 0.85f, cy + rr * 0.5f, cx + rr * 0.85f, cy + rr * 0.5f, ikonP);
                return true;
            }
            case Tartib.EMOJI: {
                boolean vergul = k.alt != null;
                float ey = vergul ? cy - s * 0.18f : cy;
                float rr = s * 0.38f;
                c.drawCircle(cx, ey, rr, ikonP);
                ikonP.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - rr * 0.38f, ey - rr * 0.25f, 1.4f * d, ikonP);
                c.drawCircle(cx + rr * 0.38f, ey - rr * 0.25f, 1.4f * d, ikonP);
                ikonP.setStyle(Paint.Style.STROKE);
                c.drawArc(new RectF(cx - rr * 0.55f, ey - rr * 0.35f, cx + rr * 0.55f, ey + rr * 0.6f),
                        25, 130, false, ikonP);
                if (vergul) {
                    matnP.setTextSize(16 * sp);
                    matnP.setColor(MATN);
                    matnP.setTypeface(Typeface.DEFAULT_BOLD);
                    c.drawText(",", cx, cy + s * 0.62f, matnP);
                }
                return true;
            }
            default:
                return false;
        }
    }

    // ---------- teginish ----------

    private final class Bosish implements Runnable {
        Tartib.Tugma k;
        float x0, oxirgiX;
        boolean uzun, surildi, bajarildi;
        int tanlov;

        final Runnable takror = new Runnable() {
            @Override public void run() {
                t.maxsus(Tartib.OCHIR);
                h.postDelayed(this, 50);
            }
        };

        @Override public void run() { // uzoq bosish
            if (bajarildi) return;
            if (k.tur == Tartib.T_BOSHLIQ || k.kod == Tartib.TIL) {
                if (!surildi) { bajarildi = true; t.tilTanlash(); }
                return;
            }
            if (k.alt != null) {
                uzun = true;
                altKorsat(this);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        switch (a) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = e.getActionIndex();
                int id = e.getPointerId(i);
                float x = e.getX(i), y = e.getY(i);
                // tez yozish: oldingi barmoq tugmasini darhol yozamiz
                for (int j = faol.size() - 1; j >= 0; j--) {
                    Bosish b = faol.valueAt(j);
                    if (!b.uzun && b.k.kod != Tartib.OCHIR && b.k.tur != Tartib.T_BOSHLIQ) {
                        faol.removeAt(j);
                        yakunla(b);
                    }
                }
                Tartib.Tugma k = top(x, y);
                if (k == null) break;
                Bosish b = new Bosish();
                b.k = k;
                b.x0 = b.oxirgiX = x;
                faol.put(id, b);
                t.tegdi(k.kod);
                if (k.kod == Tartib.OCHIR) {
                    t.maxsus(Tartib.OCHIR);
                    b.bajarildi = true;
                    h.postDelayed(b.takror, 400);
                } else {
                    h.postDelayed(b, k.tur == Tartib.T_BOSHLIQ ? 500 : 350);
                }
                korinishKorsat(k);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    Bosish b = faol.get(e.getPointerId(i));
                    if (b == null) continue;
                    float x = e.getX(i), y = e.getY(i);
                    if (b.uzun) { altTanla(b, x); continue; }
                    if (b.k.tur == Tartib.T_BOSHLIQ) {
                        // bo'shliqda surish - kursorni yurgizish
                        if (!b.surildi && Math.abs(x - b.x0) > 14 * d) {
                            b.surildi = true;
                            h.removeCallbacks(b);
                        }
                        if (b.surildi) {
                            float qadam = 12 * d;
                            int n = (int) ((x - b.oxirgiX) / qadam);
                            if (n != 0) { t.kursor(n); b.oxirgiX += n * qadam; }
                        }
                        continue;
                    }
                    if (b.k.kod == Tartib.OCHIR) continue;
                    Tartib.Tugma k = top(x, y);
                    if (k != null && k != b.k && k.tur != Tartib.T_BOSHLIQ && k.kod != Tartib.OCHIR) {
                        h.removeCallbacks(b);
                        b.k = k;
                        h.postDelayed(b, 350);
                        korinishKorsat(k);
                        invalidate();
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int id = e.getPointerId(e.getActionIndex());
                Bosish b = faol.get(id);
                if (b != null) {
                    faol.remove(id);
                    yakunla(b);
                }
                if (a == MotionEvent.ACTION_UP) faolTozala();
                invalidate();
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                faolTozala();
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    private void yakunla(Bosish b) {
        h.removeCallbacks(b);
        h.removeCallbacks(b.takror);
        if (b.uzun) {
            if (b.tanlov >= 0 && b.k.alt != null) t.matn(holat(b.k.alt[b.tanlov]));
            altOyna.dismiss();
        } else if (!b.bajarildi && !b.surildi) {
            bos(b.k);
        }
        b.bajarildi = true;
        korinish.dismiss();
    }

    private void bos(Tartib.Tugma k) {
        if (k.chiqish != null) t.matn(k.tur == Tartib.T_HARF ? holat(k.chiqish) : k.chiqish);
        else t.maxsus(k.kod);
    }

    void faolTozala() {
        for (int i = 0; i < faol.size(); i++) {
            Bosish b = faol.valueAt(i);
            h.removeCallbacks(b);
            h.removeCallbacks(b.takror);
        }
        faol.clear();
        korinish.dismiss();
        altOyna.dismiss();
    }

    @Override
    protected void onDetachedFromWindow() {
        faolTozala();
        super.onDetachedFromWindow();
    }

    // ---------- ko'rinish (tugma ustida katta harf) ----------

    private void korinishKorsat(Tartib.Tugma k) {
        if (k.tur != Tartib.T_HARF || getWindowToken() == null) { korinish.dismiss(); return; }
        korinishMatn.setText(yorliq(k));
        int w = (int) Math.max(k.w - 2 * d, 44 * d), hh = (int) (k.h * 1.1f);
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0] + (int) (k.x + k.w / 2 - w / 2f), y = loc[1] + (int) (k.y + 4 * d - hh);
        if (korinish.isShowing()) {
            korinish.update(x, y, w, hh);
        } else {
            korinish.setWidth(w);
            korinish.setHeight(hh);
            korinish.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
        }
    }

    // ---------- bosib turilganda qo'shimcha belgilar ----------

    private void altKorsat(Bosish b) {
        korinish.dismiss();
        if (getWindowToken() == null) return;
        Tartib.Tugma k = b.k;
        int n = k.alt.length;
        float cw = 0;
        for (String s : k.alt) cw = Math.max(cw, s.length() > 1 ? 30 * d * s.length() / 2f : 0);
        cw = Math.max(cw, Math.max(k.w - 2 * d, 40 * d));
        int w = (int) (cw * n + 8 * d), hh = (int) (k.h * 0.95f);
        float chap = k.x + k.w / 2 - cw / 2 - 4 * d;
        if (chap + w > getWidth()) chap = getWidth() - w;
        if (chap < 0) chap = 0;
        altChap = chap + 4 * d;
        altCw = cw;
        b.tanlov = 0;
        altView.qiymat(k.alt, cw, 0);
        int[] loc = new int[2];
        getLocationInWindow(loc);
        altOyna.setWidth(w);
        altOyna.setHeight(hh);
        altOyna.showAtLocation(this, Gravity.NO_GRAVITY, loc[0] + (int) chap, loc[1] + (int) (k.y + 2 * d - hh));
    }

    private void altTanla(Bosish b, float x) {
        int n = b.k.alt.length;
        int s = (int) Math.floor((x - altChap) / altCw);
        if (s < 0) s = 0;
        if (s >= n) s = n - 1;
        if (s != b.tanlov) {
            b.tanlov = s;
            altView.tanlov = s;
            altView.invalidate();
        }
    }

    private final class AltView extends View {
        String[] q = new String[0];
        float cw;
        int tanlov;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rr = new RectF();

        AltView(Context c) { super(c); }

        void qiymat(String[] q, float cw, int tanlov) {
            this.q = q; this.cw = cw; this.tanlov = tanlov;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            p.setColor(ALT_FON);
            rr.set(0, 0, getWidth(), getHeight());
            c.drawRoundRect(rr, 10 * d, 10 * d, p);
            p.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < q.length; i++) {
                float x0 = 4 * d + i * cw;
                rr.set(x0 + 2 * d, 4 * d, x0 + cw - 2 * d, getHeight() - 4 * d);
                if (i == tanlov) {
                    p.setColor(AKSENT);
                    c.drawRoundRect(rr, 7 * d, 7 * d, p);
                }
                String s = holat(q[i]);
                p.setTextSize((s.length() > 2 ? 16 : 24) * sp);
                p.setColor(i == tanlov ? ALT_TANLOV_MATN : MATN);
                float ty = rr.centerY() - (p.descent() + p.ascent()) / 2;
                c.drawText(s, rr.centerX(), ty, p);
            }
        }
    }
}

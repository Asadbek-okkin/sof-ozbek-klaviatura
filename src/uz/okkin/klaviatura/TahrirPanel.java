package uz.okkin.klaviatura;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Matnni tahrirlash paneli: o'qlar, belgilash, nusxa, kesish, qo'yish. */
final class TahrirPanel extends LinearLayout {

    interface Tinglovchi {
        void tahrir(int amal, boolean belgilash);
        void harflarga();
        void ochir();
    }

    static final int CHAP = 1, ONG = 2, TEPA = 3, PAST = 4, BOSH = 5, OXIR = 6,
            HAMMASI = 7, NUSXA = 8, KESISH = 9, QOYISH = 10;

    private final Tinglovchi t;
    private final float d;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean belgilash;
    private TextView belgiTugma;

    TahrirPanel(Context c, Tinglovchi t) {
        super(c);
        this.t = t;
        d = c.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setBackgroundColor(KlaviaturaView.FON);
        setPadding((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));

        LinearLayout q1 = qator();
        q1.addView(tugma("⇤ Boş", BOSH, false), lp(1));
        q1.addView(tugma("↑", TEPA, true), lp(1));
        q1.addView(tugma("Oxir ⇥", OXIR, false), lp(1));
        q1.addView(tugma("Hammasi", HAMMASI, false), lp(1));
        addView(q1, qlp());

        LinearLayout q2 = qator();
        q2.addView(tugma("←", CHAP, true), lp(1));
        belgiTugma = tugma("Belgilaş", 0, false);
        belgiTugma.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                belgilash = !belgilash;
                belgiYangila();
            }
        });
        q2.addView(belgiTugma, lp(1));
        q2.addView(tugma("→", ONG, true), lp(1));
        q2.addView(tugma("Nusxa", NUSXA, false), lp(1));
        addView(q2, qlp());

        LinearLayout q3 = qator();
        q3.addView(bosh(), lp(1));
        q3.addView(tugma("↓", PAST, true), lp(1));
        q3.addView(bosh(), lp(1));
        q3.addView(tugma("Kesiş", KESISH, false), lp(1));
        addView(q3, qlp());

        LinearLayout q4 = qator();
        TextView abc = tugma("ABC", 0, false);
        abc.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { belgilash = false; belgiYangila(); TahrirPanel.this.t.harflarga(); }
        });
        q4.addView(abc, lp(1));
        q4.addView(tugma("Qōyiş", QOYISH, false), lp(2));
        TextView ochir = tugma("⌫", 0, false);
        ochir.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { TahrirPanel.this.t.ochir(); }
        });
        q4.addView(ochir, lp(1));
        addView(q4, qlp());
        belgiYangila();
    }

    private void belgiYangila() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(belgilash ? KlaviaturaView.AKSENT : KlaviaturaView.FUNK);
        g.setCornerRadius(8 * d);
        belgiTugma.setBackground(g);
        belgiTugma.setTextColor(belgilash ? KlaviaturaView.FON : KlaviaturaView.MATN);
    }

    private View bosh() {
        View v = new View(getContext());
        v.setVisibility(INVISIBLE);
        return v;
    }

    private LinearLayout qator() {
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(HORIZONTAL);
        return l;
    }

    private LayoutParams lp(float og) {
        LayoutParams l = new LayoutParams(0, LayoutParams.MATCH_PARENT, og);
        l.setMargins((int) (3 * d), (int) (3 * d), (int) (3 * d), (int) (3 * d));
        return l;
    }

    private LayoutParams qlp() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
    }

    private TextView tugma(String s, final int amal, boolean takror) {
        TextView tv = new TextView(getContext());
        tv.setText(s);
        tv.setTextSize(s.length() <= 1 ? 22 : 15);
        tv.setTextColor(KlaviaturaView.MATN);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(takror ? KlaviaturaView.HARF : KlaviaturaView.FUNK);
        g.setCornerRadius(8 * d);
        tv.setBackground(g);
        if (amal == 0) return tv;
        if (!takror) {
            tv.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    t.tahrir(amal, belgilash);
                    if (amal == NUSXA || amal == KESISH || amal == QOYISH) { belgilash = false; belgiYangila(); }
                }
            });
            return tv;
        }
        // o'qlar bosib turilsa takrorlanadi
        tv.setOnTouchListener(new OnTouchListener() {
            final Runnable r = new Runnable() {
                @Override public void run() { t.tahrir(amal, belgilash); h.postDelayed(this, 70); }
            };
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.6f);
                        t.tahrir(amal, belgilash);
                        h.postDelayed(r, 400);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1f);
                        h.removeCallbacks(r);
                        return true;
                    default:
                        return true;
                }
            }
        });
        return tv;
    }
}

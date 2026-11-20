package uz.okkin.klaviatura;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Bufer: oxirgi nusxalangan matnlar. Bosilsa - qo'yiladi, ✕ - o'chiriladi. */
final class BuferPanel extends LinearLayout {

    interface Tinglovchi {
        void emoji(String matn); // matnni yozish
        void harflarga();
    }

    private static final int MAX = 25;

    private final Tinglovchi t;
    private final float d;
    private final SharedPreferences sp;
    private final List<String> royxat = new ArrayList<>();
    private final LinearLayout ichki;
    private final ClipboardManager cm;
    private final ClipboardManager.OnPrimaryClipChangedListener tinglovchi;

    BuferPanel(Context c, Tinglovchi t) {
        super(c);
        this.t = t;
        d = c.getResources().getDisplayMetrics().density;
        sp = c.getSharedPreferences("bufer", Context.MODE_PRIVATE);
        yukla();
        setOrientation(VERTICAL);
        setBackgroundColor(KlaviaturaView.FON);

        LinearLayout tepa = new LinearLayout(c);
        tepa.setOrientation(HORIZONTAL);
        tepa.setGravity(Gravity.CENTER_VERTICAL);
        tepa.setPadding((int) (12 * d), 0, (int) (6 * d), 0);
        TextView sarlavha = matn("Bufer — nusxalanganlar", 15, true);
        tepa.addView(sarlavha, new LayoutParams(0, (int) (40 * d), 1f));
        TextView tozala = matn("Tozalaş", 14, false);
        tozala.setTextColor(KlaviaturaView.AKSENT);
        tozala.setPadding((int) (10 * d), 0, (int) (10 * d), 0);
        tozala.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { royxat.clear(); saqla(); chiz(); }
        });
        tepa.addView(tozala, new LayoutParams(LayoutParams.WRAP_CONTENT, (int) (40 * d)));
        TextView abc = matn("ABC", 14, true);
        abc.setPadding((int) (12 * d), 0, (int) (12 * d), 0);
        abc.setBackground(fon(KlaviaturaView.FUNK));
        abc.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { BuferPanel.this.t.harflarga(); }
        });
        tepa.addView(abc, new LayoutParams(LayoutParams.WRAP_CONTENT, (int) (34 * d)));
        addView(tepa);

        ScrollView sv = new ScrollView(c);
        ichki = new LinearLayout(c);
        ichki.setOrientation(VERTICAL);
        ichki.setPadding((int) (6 * d), 0, (int) (6 * d), (int) (6 * d));
        sv.addView(ichki);
        addView(sv, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        tinglovchi = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override public void onPrimaryClipChanged() { buferdanOl(); }
        };
        if (cm != null) cm.addPrimaryClipChangedListener(tinglovchi);
    }

    void yopil() {
        if (cm != null) cm.removePrimaryClipChangedListener(tinglovchi);
    }

    private void buferdanOl() {
        try {
            if (cm == null || !cm.hasPrimaryClip()) return;
            ClipData cd = cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) return;
            CharSequence s = cd.getItemAt(0).coerceToText(getContext());
            if (s == null) return;
            String m = s.toString();
            if (m.trim().isEmpty() || m.length() > 5000) return;
            royxat.remove(m);
            royxat.add(0, m);
            while (royxat.size() > MAX) royxat.remove(royxat.size() - 1);
            saqla();
            if (getVisibility() == VISIBLE) chiz();
        } catch (Exception ignored) { }
    }

    void ochildi() {
        buferdanOl();
        chiz();
    }

    private void chiz() {
        ichki.removeAllViews();
        if (royxat.isEmpty()) {
            TextView bosh = matn("Hali hech narsa nusxalanmagan.\nMatnni nusxalasangiz, shu yerda paydo bōladi.", 14, false);
            bosh.setTextColor(KlaviaturaView.HINT);
            bosh.setGravity(Gravity.CENTER);
            bosh.setPadding(0, (int) (30 * d), 0, 0);
            ichki.addView(bosh);
            return;
        }
        for (final String m : royxat) {
            LinearLayout q = new LinearLayout(getContext());
            q.setOrientation(HORIZONTAL);
            q.setGravity(Gravity.CENTER_VERTICAL);
            q.setBackground(fon(KlaviaturaView.HARF));
            TextView tv = matn(m, 15, false);
            tv.setMaxLines(2);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setPadding((int) (12 * d), (int) (10 * d), (int) (6 * d), (int) (10 * d));
            tv.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) { t.emoji(m); }
            });
            q.addView(tv, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            TextView x = matn("✕", 16, false);
            x.setTextColor(KlaviaturaView.HINT);
            x.setGravity(Gravity.CENTER);
            x.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) { royxat.remove(m); saqla(); chiz(); }
            });
            q.addView(x, new LayoutParams((int) (44 * d), (int) (44 * d)));
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (5 * d);
            ichki.addView(q, lp);
        }
    }

    private TextView matn(String s, int size, boolean qalin) {
        TextView tv = new TextView(getContext());
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(KlaviaturaView.MATN);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if (qalin) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private GradientDrawable fon(int rang) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(rang);
        g.setCornerRadius(8 * d);
        return g;
    }

    private void yukla() {
        try {
            JSONArray a = new JSONArray(sp.getString("royxat", "[]"));
            for (int i = 0; i < a.length(); i++) royxat.add(a.getString(i));
        } catch (Exception ignored) { }
    }

    private void saqla() {
        JSONArray a = new JSONArray();
        for (String s : royxat) a.put(s);
        sp.edit().putString("royxat", a.toString()).apply();
    }
}

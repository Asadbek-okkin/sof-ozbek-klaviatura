package uz.okkin.klaviatura;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Smayliklar paneli (Gboard'dagi kabi): toifalar, oxirgilar, ABC va o'chirish. */
final class EmojiPanel extends LinearLayout {

    interface Tinglovchi {
        void emoji(String e);
        void ochir();
        void harflarga();
    }

    private static final int MAX_OXIRGI = 40;

    private final Tinglovchi t;
    private final float d;
    private final SharedPreferences sp;
    private final List<String> oxirgi = new ArrayList<>();
    private final List<String> joriy = new ArrayList<>();
    private final TextView[] tablar = new TextView[Emojilar.BELGI.length];
    private final GridView grid;
    private final Adapter adapter = new Adapter();
    private final Handler h = new Handler(Looper.getMainLooper());
    private int toifa = -1;

    EmojiPanel(Context c, Tinglovchi t) {
        super(c);
        this.t = t;
        d = c.getResources().getDisplayMetrics().density;
        sp = c.getSharedPreferences("emoji", Context.MODE_PRIVATE);
        String s = sp.getString("oxirgi", "");
        if (!s.isEmpty()) oxirgi.addAll(Arrays.asList(s.split(" ")));

        setOrientation(VERTICAL);
        setBackgroundColor(KlaviaturaView.FON);

        LinearLayout tabQator = new LinearLayout(c);
        tabQator.setOrientation(HORIZONTAL);
        for (int i = 0; i < tablar.length; i++) {
            TextView tv = new TextView(c);
            tv.setText(Emojilar.BELGI[i]);
            tv.setTextSize(17);
            tv.setGravity(Gravity.CENTER);
            final int n = i;
            tv.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) { tanla(n); }
            });
            tablar[i] = tv;
            tabQator.addView(tv, new LayoutParams(0, (int) (40 * d), 1f));
        }
        addView(tabQator, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        grid = new GridView(c);
        grid.setColumnWidth((int) (44 * d));
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(0);
        grid.setSelector(new GradientDrawable());
        grid.setAdapter(adapter);
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout past = new LinearLayout(c);
        past.setOrientation(HORIZONTAL);
        past.setPadding((int) (2 * d), (int) (2 * d), (int) (2 * d), (int) (4 * d));
        TextView abc = tugma(c, "ABC", 15, KlaviaturaView.FUNK);
        abc.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { haptic(v); EmojiPanel.this.t.harflarga(); }
        });
        past.addView(abc, new LayoutParams(0, (int) (46 * d), 1.5f));
        TextView boshliq = tugma(c, "", 14, KlaviaturaView.HARF);
        boshliq.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { haptic(v); EmojiPanel.this.t.emoji(" "); }
        });
        past.addView(boshliq, new LayoutParams(0, (int) (46 * d), 6f));
        final TextView ochir = tugma(c, "⌫", 20, KlaviaturaView.FUNK);
        ochir.setOnTouchListener(new OnTouchListener() {
            final Runnable takror = new Runnable() {
                @Override public void run() { EmojiPanel.this.t.ochir(); h.postDelayed(this, 60); }
            };
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        haptic(v);
                        v.setPressed(true);
                        EmojiPanel.this.t.ochir();
                        h.postDelayed(takror, 400);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        h.removeCallbacks(takror);
                        return true;
                    default:
                        return true;
                }
            }
        });
        past.addView(ochir, new LayoutParams(0, (int) (46 * d), 1.5f));
        addView(past, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private TextView tugma(Context c, String s, int size, int rang) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(KlaviaturaView.MATN);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(rang);
        g.setCornerRadius(7 * d);
        android.graphics.drawable.InsetDrawable fon =
                new android.graphics.drawable.InsetDrawable(g, (int) (3 * d), (int) (3 * d), (int) (3 * d), (int) (3 * d));
        tv.setBackground(fon);
        return tv;
    }

    private void haptic(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    /** Panel ochilganda: oxirgilar bo'lsa shular, bo'lmasa yuzlar. */
    void ochildi() {
        tanla(oxirgi.isEmpty() ? 1 : 0);
    }

    private void tanla(int n) {
        toifa = n;
        joriy.clear();
        if (n == 0) joriy.addAll(oxirgi);
        else joriy.addAll(Arrays.asList(Emojilar.ROYXAT[n].split(" ")));
        for (int i = 0; i < tablar.length; i++) {
            if (i == n) {
                GradientDrawable g = new GradientDrawable();
                g.setColor(KlaviaturaView.FUNK);
                g.setCornerRadius(8 * d);
                tablar[i].setBackground(g);
            } else {
                tablar[i].setBackground(null);
            }
        }
        adapter.notifyDataSetChanged();
        grid.setSelection(0);
    }

    private void bosildi(String e) {
        t.emoji(e);
        oxirgi.remove(e);
        oxirgi.add(0, e);
        while (oxirgi.size() > MAX_OXIRGI) oxirgi.remove(oxirgi.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String s : oxirgi) { if (sb.length() > 0) sb.append(' '); sb.append(s); }
        sp.edit().putString("oxirgi", sb.toString()).apply();
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return joriy.size(); }
        @Override public Object getItem(int i) { return joriy.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View eski, ViewGroup ota) {
            TextView tv = (TextView) eski;
            if (tv == null) {
                tv = new TextView(getContext());
                tv.setTextSize(27);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(KlaviaturaView.MATN);
                tv.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)));
                tv.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) {
                        haptic(v);
                        bosildi(((TextView) v).getText().toString());
                    }
                });
            }
            tv.setText(joriy.get(i));
            return tv;
        }
    }
}

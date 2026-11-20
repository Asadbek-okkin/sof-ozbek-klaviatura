package uz.okkin.klaviatura;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** O'rnatish yordamchisi: 1) yoqish, 2) tanlash, 3) sinab ko'rish. */
public class Sozlash extends Activity {

    private static final int FON = 0xFF15171B, KARTA = 0xFF23262C, MATN = 0xFFE8EAED,
            KULRANG = 0xFF9AA0A6, KOK = 0xFF1A77D2, YASHIL = 0xFF34A853;

    private TextView qadam1, qadam2;
    private TextView holat1, holat2;
    private LinearLayout yangiKarta;
    private TextView yangiMatn;
    private LinearLayout k1, k2, ildiz;
    private ImageView ikonka;
    private android.animation.ObjectAnimator puls;
    private View pulsKarta;
    private boolean pickerOchildi, tabriklandi, animatsiyaBoldi;
    // fikrga qo'shiladigan skrinshot
    private byte[] fikrRasm;
    private TextView fikrRasmHolat;
    private static final int RASM_TANLASH = 7;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Sinxron.xatoUshlagich(this);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(FON);
            getWindow().setNavigationBarColor(FON);
        }
        int p = dp(20);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(p, dp(28), p, p);
        l.setBackgroundColor(FON);

        // sarlavha
        LinearLayout bosh = new LinearLayout(this);
        bosh.setOrientation(LinearLayout.HORIZONTAL);
        bosh.setGravity(Gravity.CENTER_VERTICAL);
        ikonka = new ImageView(this);
        ikonka.setImageResource(R.mipmap.ic_launcher);
        bosh.addView(ikonka, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout nomlar = new LinearLayout(this);
        nomlar.setOrientation(LinearLayout.VERTICAL);
        nomlar.setPadding(dp(14), 0, 0, 0);
        nomlar.addView(matn("Sof Özbek klaviatura", 22, MATN, true));
        nomlar.addView(matn("Yangi lotin alifbosi uçun • v" + versiya(), 14, KULRANG, false));
        bosh.addView(nomlar);
        l.addView(bosh);

        // yangi versiya kartasi (bo'lsa ko'rinadi)
        yangiKarta = karta();
        yangiKarta.setBackground(fon(0xFF1B3A63, 14));
        yangiMatn = matn("", 16, MATN, true);
        yangiKarta.addView(yangiMatn);
        yangiKarta.addView(matn("Bosing — yuklab olinadi va ōrnatiladi", 14, 0xFF8AB4F8, false));
        yangiKarta.setVisibility(View.GONE);
        yangiKarta.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Yangilash.ornat(Sozlash.this); }
        });
        LinearLayout.LayoutParams ylp = kartaLp();
        ylp.topMargin = dp(16);
        l.addView(yangiKarta, ylp);

        TextView izoh = matn("Ö Ş Ç Ğ harflari alohida tugmada, sōz takliflari va tuzatiş, "
                + "ovozli yozish, eski yozuvni yangisiga ōgiriş, bufer, smayliklar. "
                + "Yozganlaringiz hech qayerga yuborilmaydi — internet faqat yangi versiyani "
                + "tekşiriş uçun işlatiladi.", 15, KULRANG, false);
        izoh.setPadding(0, dp(18), 0, dp(10));
        l.addView(izoh);

        // 1-qadam
        k1 = karta();
        qadam1 = matn("1. Klaviaturani yoqiş", 17, MATN, true);
        holat1 = matn("", 14, KULRANG, false);
        k1.addView(qadam1);
        k1.addView(holat1);
        k1.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        l.addView(k1, kartaLp());

        // 2-qadam
        k2 = karta();
        qadam2 = matn("2. Asosiy klaviatura qiliş", 17, MATN, true);
        holat2 = matn("", 14, KULRANG, false);
        k2.addView(qadam2);
        k2.addView(holat2);
        k2.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
        l.addView(k2, kartaLp());

        // qo'llanma videosi
        LinearLayout vk = karta();
        vk.setBackground(fon(0xFF1B3A63, 14));
        vk.addView(matn("📹 Qanday işlatiladi — qisqa video (1:43)", 17, MATN, true));
        vk.addView(matn("Ōrnatiş, yangi harflar, sōz takliflari, ovoz, ōgiriş va fişkalar", 14, 0xFF8AB4F8, false));
        vk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { videoKorsat(); }
        });
        LinearLayout.LayoutParams vlp = kartaLp();
        vlp.topMargin = dp(14);
        l.addView(vk, vlp);

        // 3-qadam: sinov
        TextView q3 = matn("3. Sinab kōring", 17, MATN, true);
        q3.setPadding(0, dp(18), 0, dp(8));
        l.addView(q3);
        EditText sinov = new EditText(this);
        sinov.setHint("Bu yerga yozing...");
        sinov.setHintTextColor(KULRANG);
        sinov.setTextColor(MATN);
        sinov.setMinLines(3);
        sinov.setGravity(Gravity.TOP);
        sinov.setPadding(dp(14), dp(12), dp(14), dp(12));
        sinov.setBackground(fon(KARTA, 14));
        l.addView(sinov);

        // ---------- fikr ----------
        LinearLayout fk = karta();
        fk.addView(matn("💬 Fikr yoki taklif yuboriş", 17, MATN, true));
        fk.addView(matn("Nima yoqdi, nima yetişmayapti — yozing. Keyingi versiyalar şu fikrlar asosida çiqadi.", 14, KULRANG, false));
        fk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { fikrOynasi(); }
        });
        LinearLayout.LayoutParams flp = kartaLp();
        flp.topMargin = dp(18);
        l.addView(fk, flp);
        LinearLayout ak = karta();
        ak.addView(matn("✈️ Adminga Telegramda yoziş", 17, MATN, true));
        ak.addView(matn("Savol, taklif yoki muammo bōlsa — to'g'ridan-to'g'ri yozing", 14, KULRANG, false));
        ak.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { admingaYoz(); }
        });
        l.addView(ak, kartaLp());

        // ---------- sozlamalar ----------
        TextView sar = matn("Sozlamalar", 17, MATN, true);
        sar.setPadding(0, dp(24), 0, dp(4));
        l.addView(sar);
        final Context c = this;
        l.addView(tanlov("Mavzu", Sozlamalar.MAVZU_NOMLARI, new Ol() {
            @Override public int ol() {
                String m = Sozlamalar.mavzu(c);
                for (int i = 0; i < Sozlamalar.MAVZULAR.length; i++) if (Sozlamalar.MAVZULAR[i].equals(m)) return i;
                return 0;
            }
            @Override public void qoy(int i) { Sozlamalar.s(c, Sozlamalar.MAVZU, Sozlamalar.MAVZULAR[i]); }
        }));
        l.addView(tanlov("Tugmalar balandligi", new String[]{"Kiçik", "Oddiy", "Katta"}, new Ol() {
            @Override public int ol() { return Sozlamalar.balandlik(c); }
            @Override public void qoy(int i) { Sozlamalar.i(c, Sozlamalar.BALANDLIK, i); }
        }));
        l.addView(tanlov("Tebraniş", new String[]{"Yōq", "Yengil", "Kuçli"}, new Ol() {
            @Override public int ol() { return Sozlamalar.tebranish(c); }
            @Override public void qoy(int i) { Sozlamalar.i(c, Sozlamalar.TEBRANISH, i); }
        }));
        l.addView(kalit("Tugma tovuşi", null, Sozlamalar.TOVUSH));
        final Tovushlar namuna = new Tovushlar(this);
        l.addView(tanlov("Tovuş turi", Tovushlar.KORINISH, new Ol() {
            @Override public int ol() { return Tovushlar.indeks(Sozlamalar.tovushTuri(c)); }
            @Override public void qoy(int i) {
                Sozlamalar.s(c, Sozlamalar.TOVUSH_TURI, Tovushlar.NOMLAR[i]);
                namuna.namuna(Tovushlar.NOMLAR[i]);
            }
        }));
        l.addView(kalit("Raqamlar qatori", "Harflar ustida 1…0 qatori", Sozlamalar.RAQAMLAR));
        l.addView(kalit("Sōz takliflari", "Yozayotganda tepada 3 ta sōz", Sozlamalar.TAKLIFLAR));
        l.addView(kalit("Avtomatik tuzatiş", "Bōşliq bosilganda xatoni tuzatadi (⌫ — qaytarish)", Sozlamalar.AVTOTUZATISH));
        l.addView(kalit("Gap boşida katta harf", null, Sozlamalar.AVTO_KATTA));
        l.addView(kalit("Ikki marta bōşliq — nuqta", null, Sozlamalar.IKKI_BOSHLIQ));
        l.addView(kalit("English klaviatura", "🌐 bilan ōtiş", Sozlamalar.INGLIZCHA));
        l.addView(kalit("Русская клавиатура", "🌐 bilan ōtiş", Sozlamalar.RUSCHA));
        l.addView(kalit("Lug'atni boyitişga yordam",
                "Lug'atda yōq, siz kōp yozgan alohida sōzlar anonim yuboriladi. Gap, parol va "
                        + "şaxsiy matn hech qaçon yuborilmaydi. Kamida 3 kişi yozgan sōz hammaning lug'atiga qōşiladi.",
                Sozlamalar.HISSA));
        TextView unut = matn("Ōrganilgan sōzlarni ōçiriş", 15, 0xFFEF5350, false);
        unut.setPadding(dp(4), dp(16), dp(4), dp(8));
        unut.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new android.app.AlertDialog.Builder(Sozlash.this)
                        .setMessage("Klaviatura siz yozganlardan ōrgangan sōzlar ōçirilsinmi?")
                        .setPositiveButton("Ōçiriş", new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                Lugat.ol(Sozlash.this).hammasiniUnut();
                                android.widget.Toast.makeText(Sozlash.this, "Ōçirildi", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Yōq", null)
                        .show();
            }
        });
        l.addView(unut);

        TextView yordam = matn(
                "Maslahatlar\n"
                        + "• Tepadagi satr: 📋 bufer, o→ö eski matnni yangi alifboga ōgiriş, "
                        + "🎨 mavzu, ⚙ sozlamalar, 🎤 ovozli yozish\n"
                        + "• Avtomatik tuzatişdan keyin ⌫ bossangiz — yozganingiz qaytadi\n"
                        + "• ☺ — smayliklar; bosib tursangiz vergul va tutuq ʼ\n"
                        + "• . bosib tursangiz: , ? ! ʼ ' \" : ;\n"
                        + "• Bōşliqni çapga-ōngga surib — kursor yuradi\n"
                        + "• Bōşliqni bosib tursangiz — boşqa klaviaturani tanlaş\n"
                        + "• Ikki marta bōşliq — nuqta\n"
                        + "• ⇧ ni ikki marta tez bossangiz — doimiy katta harf\n"
                        + "• Telefon yorug' yoki qorong'i mavzusiga moslaşadi",
                14, KULRANG, false);
        yordam.setPadding(0, dp(22), 0, 0);
        yordam.setLineSpacing(dp(3), 1f);
        l.addView(yordam);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(FON);
        sv.setFillViewport(true);
        sv.addView(l);
        setContentView(sv);
        ildiz = l;
        if (yoqilganmi() && tanlanganmi()) Sinxron.sp(this).edit().putBoolean("sozlanmagan", false).apply();
        if (b == null) kirishAnimatsiyasi();
    }

    // ======================================================================
    //  Animatsiyalar
    // ======================================================================

    /** Ilova ochilganda: ikonka "sakrab" chiqadi, kartalar pastdan ketma-ket suzib keladi. */
    private void kirishAnimatsiyasi() {
        animatsiyaBoldi = true;
        android.view.animation.DecelerateInterpolator sekin = new android.view.animation.DecelerateInterpolator(2f);
        for (int i = 0; i < ildiz.getChildCount(); i++) {
            View v = ildiz.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(36));
            v.animate().alpha(1f).translationY(0).setStartDelay(120 + Math.min(i, 12) * 55L)
                    .setDuration(480).setInterpolator(sekin).start();
        }
        ikonka.setScaleX(0.3f);
        ikonka.setScaleY(0.3f);
        ikonka.setRotation(-30f);
        ikonka.animate().scaleX(1f).scaleY(1f).rotation(0f).setStartDelay(150).setDuration(700)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).start();
    }

    /** Navbatdagi qadam kartasi yumshoq "nafas oladi" - qayerni bosish kerakligi ko'rinib tursin. */
    private void pulsla(View karta) {
        if (pulsKarta == karta) return;
        pulsToxtat();
        if (karta == null) return;
        pulsKarta = karta;
        android.animation.PropertyValuesHolder sx = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.035f);
        android.animation.PropertyValuesHolder sy = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.035f);
        puls = android.animation.ObjectAnimator.ofPropertyValuesHolder(karta, sx, sy);
        puls.setDuration(850);
        puls.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        puls.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        puls.setStartDelay(900);
        puls.start();
        karta.setBackground(fon(0xFF26364D, 14));
    }

    private void pulsToxtat() {
        if (puls != null) puls.cancel();
        if (pulsKarta != null) {
            pulsKarta.setScaleX(1f);
            pulsKarta.setScaleY(1f);
            pulsKarta.setBackground(fon(KARTA, 14));
        }
        puls = null;
        pulsKarta = null;
    }

    /** Ikkala qadam bajarilganda - tabrik. */
    private void tabrikla() {
        final TextView t = matn("🎉", 64, MATN, false);
        t.setGravity(Gravity.CENTER);
        final android.widget.FrameLayout yuza = (android.widget.FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
        yuza.addView(t, new android.widget.FrameLayout.LayoutParams(-1, -1));
        t.setScaleX(0f);
        t.setScaleY(0f);
        t.animate().scaleX(1.4f).scaleY(1.4f).setDuration(450)
                .setInterpolator(new android.view.animation.OvershootInterpolator(3f))
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        t.animate().alpha(0f).scaleX(2f).scaleY(2f).setStartDelay(500).setDuration(500)
                                .withEndAction(new Runnable() { @Override public void run() { yuza.removeView(t); } }).start();
                    }
                }).start();
        android.widget.Toast.makeText(this, "Tayyor! Endi istalgan joyda yangi alifboda yozing 🎉",
                android.widget.Toast.LENGTH_LONG).show();
    }

    // ======================================================================
    //  Qo'llanma videosi
    // ======================================================================

    private void videoKorsat() {
        final android.widget.VideoView vv = new android.widget.VideoView(this);
        android.widget.FrameLayout f = new android.widget.FrameLayout(this);
        f.setBackgroundColor(0xFF000000);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(-1, -1);
        lp.gravity = Gravity.CENTER;
        f.addView(vv, lp);
        final android.app.Dialog d = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        d.setContentView(f);
        android.widget.MediaController mc = new android.widget.MediaController(this);
        mc.setAnchorView(vv);
        vv.setMediaController(mc);
        vv.setVideoURI(android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.qollanma));
        vv.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
            @Override public void onCompletion(android.media.MediaPlayer mp) { d.dismiss(); }
        });
        d.show();
        vv.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        yangila();
        Yangilash.tekshir(this, true, new Yangilash.Natija() {
            @Override public void tayyor(boolean bor) {
                if (bor) {
                    yangiMatn.setText("🆕 Yangi versiya: " + Yangilash.yangiNom(Sozlash.this));
                    yangiKarta.setVisibility(View.VISIBLE);
                } else {
                    yangiKarta.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean fokus) {
        super.onWindowFocusChanged(fokus);
        if (fokus) yangila(); // tanlash oynasi yopilganda
    }

    private void yangila() {
        boolean yoq = yoqilganmi(), tanl = tanlanganmi();
        holat1.setText(yoq ? "✅ Yoqilgan" : "Bosing va ro'yxatda «Sof Özbek» ni yoqing");
        holat1.setTextColor(yoq ? YASHIL : KULRANG);
        holat2.setText(tanl ? "✅ Tanlangan — tayyor!" : "Bosing va «Sof Özbek» ni tanlang");
        holat2.setTextColor(tanl ? YASHIL : KULRANG);
        // navbatdagi qadamni ko'rsatib turamiz
        pulsla(!yoq ? k1 : !tanl ? k2 : null);
        // 1-qadam bajarilib qaytilganda - 2-qadam oynasi o'zi ochiladi (qulay o'rnatish)
        if (yoq && !tanl && !pickerOchildi && hasWindowFocus()) {
            pickerOchildi = true;
            ildiz.postDelayed(new Runnable() {
                @Override public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && !tanlanganmi()) imm.showInputMethodPicker();
                }
            }, 700);
        }
        if (yoq && tanl && !tabriklandi) {
            tabriklandi = true;
            // birinchi marta tayyor bo'lganda (ilova oldin sozlangan bo'lsa - tabrik yo'q)
            if (Sinxron.sp(this).getBoolean("sozlanmagan", true)) {
                Sinxron.sp(this).edit().putBoolean("sozlanmagan", false).apply();
                tabrikla();
            }
        }
    }

    private boolean yoqilganmi() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getPackageName())) return true;
        }
        return false;
    }

    private boolean tanlanganmi() {
        String joriy = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return joriy != null && joriy.startsWith(getPackageName() + "/");
    }

    private String versiya() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    /** «Adminga yozish»: adminning Telegram akkaunti (server aytadi) ochiladi. */
    private void admingaYoz() {
        android.widget.Toast.makeText(this, "Telegram oçilmoqda...", android.widget.Toast.LENGTH_SHORT).show();
        Sinxron.aloqa(this, new Sinxron.Matn() {
            @Override public void tayyor(String username) {
                String u = username != null ? username : "claude_asadbek_bot";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/" + u)));
                } catch (Exception e) {
                    android.widget.Toast.makeText(Sozlash.this, "Telegram topilmadi: @" + u, android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int kod, int natija, Intent data) {
        super.onActivityResult(kod, natija, data);
        if (kod != RASM_TANLASH || natija != RESULT_OK || data == null || data.getData() == null) return;
        try {
            // rasmni kichraytirib JPEG qilamiz (ko'pi bilan 1280 px, ~200-400 KB)
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            android.graphics.BitmapFactory.decodeStream(in, null, o);
            in.close();
            int ol = 1;
            while (Math.max(o.outWidth, o.outHeight) / ol > 2560) ol *= 2;
            o = new android.graphics.BitmapFactory.Options();
            o.inSampleSize = ol;
            in = getContentResolver().openInputStream(data.getData());
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (bm == null) throw new Exception("rasm emas");
            float k = 1280f / Math.max(bm.getWidth(), bm.getHeight());
            if (k < 1f) bm = android.graphics.Bitmap.createScaledBitmap(bm, (int) (bm.getWidth() * k), (int) (bm.getHeight() * k), true);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos);
            fikrRasm = bos.toByteArray();
            if (fikrRasmHolat != null) fikrRasmHolat.setText("✅ Skrinşot qōşildi (" + fikrRasm.length / 1024 + " KB)");
        } catch (Exception e) {
            fikrRasm = null;
            android.widget.Toast.makeText(this, "Rasmni oçib bōlmadi", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void fikrOynasi() {
        fikrRasm = null;
        LinearLayout u = new LinearLayout(this);
        u.setOrientation(LinearLayout.VERTICAL);
        u.setPadding(dp(20), dp(8), dp(20), 0);
        final EditText e = new EditText(this);
        e.setHint("Fikringiz...");
        e.setMinLines(3);
        e.setGravity(Gravity.TOP);
        u.addView(e);
        TextView rasm = matn("📷 Skrinşot qōşiş", 16, 0xFF8AB4F8, true);
        rasm.setPadding(0, dp(14), 0, dp(4));
        rasm.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                try { startActivityForResult(Intent.createChooser(i, "Skrinşotni tanlang"), RASM_TANLASH); }
                catch (Exception ignored) { }
            }
        });
        u.addView(rasm);
        fikrRasmHolat = matn("", 14, YASHIL, false);
        u.addView(fikrRasmHolat);
        TextView tg = matn("✈️ Yoki adminga Telegramda yozing", 15, KULRANG, false);
        tg.setPadding(0, dp(12), 0, dp(6));
        tg.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { admingaYoz(); }
        });
        u.addView(tg);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Fikr yoki taklif")
                .setView(u)
                .setPositiveButton("Yuboriş", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String m = e.getText().toString().trim();
                        if (m.isEmpty() && fikrRasm == null) return;
                        if (m.isEmpty()) m = "(skrinşot)";
                        Sinxron.fikrYubor(Sozlash.this, m, fikrRasm, new Sinxron.Javob() {
                            @Override public void tayyor(boolean ok) {
                                android.widget.Toast.makeText(Sozlash.this, ok
                                        ? "Rahmat! Fikringiz yuborildi 🙏"
                                        : "Yuborib bōlmadi. Internetni tekşirib qayta urinib kōring.",
                                        android.widget.Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Bekor", null)
                .show();
    }

    private interface Ol {
        int ol();
        void qoy(int i);
    }

    private View tanlov(final String nom, final String[] variant, final Ol o) {
        LinearLayout q = new LinearLayout(this);
        q.setOrientation(LinearLayout.VERTICAL);
        q.setPadding(dp(4), dp(12), dp(4), dp(12));
        q.addView(matn(nom, 16, MATN, false));
        final TextView qiymat = matn(variant[Math.min(o.ol(), variant.length - 1)], 14, 0xFF8AB4F8, false);
        q.addView(qiymat);
        q.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new android.app.AlertDialog.Builder(Sozlash.this)
                        .setTitle(nom)
                        .setSingleChoiceItems(variant, o.ol(), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int i) {
                                o.qoy(i);
                                qiymat.setText(variant[i]);
                                d.dismiss();
                            }
                        })
                        .show();
            }
        });
        return q;
    }

    private View kalit(String nom, String izoh, final String k) {
        LinearLayout q = new LinearLayout(this);
        q.setOrientation(LinearLayout.HORIZONTAL);
        q.setGravity(Gravity.CENTER_VERTICAL);
        q.setPadding(dp(4), dp(10), 0, dp(10));
        LinearLayout matnlar = new LinearLayout(this);
        matnlar.setOrientation(LinearLayout.VERTICAL);
        matnlar.addView(matn(nom, 16, MATN, false));
        if (izoh != null) matnlar.addView(matn(izoh, 13, KULRANG, false));
        q.addView(matnlar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final android.widget.Switch sw = new android.widget.Switch(this);
        sw.setChecked(Sozlamalar.b(this, k));
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Sozlamalar.b(Sozlash.this, k, v);
            }
        });
        q.addView(sw);
        q.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sw.toggle(); }
        });
        return q;
    }

    private TextView matn(String s, int size, int rang, boolean qalin) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(rang);
        if (qalin) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private LinearLayout karta() {
        LinearLayout k = new LinearLayout(this);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setPadding(dp(16), dp(14), dp(16), dp(14));
        k.setBackground(fon(KARTA, 14));
        k.setClickable(true);
        return k;
    }

    private LinearLayout.LayoutParams kartaLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        return lp;
    }

    private GradientDrawable fon(int rang, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(rang);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), 0xFF30343B);
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}

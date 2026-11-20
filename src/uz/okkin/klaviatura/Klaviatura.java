package uz.okkin.klaviatura;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Sof o'zbek (2026-yilgi yangi lotin) klaviaturasi - Gboard uslubida. */
public class Klaviatura extends InputMethodService implements KlaviaturaView.Tinglovchi,
        EmojiPanel.Tinglovchi, BuferPanel.Tinglovchi, TahrirPanel.Tinglovchi,
        Asboblar.Tinglovchi, Ovoz.Tinglovchi {

    private final Handler h = new Handler(Looper.getMainLooper());

    private Asboblar ab;
    private KlaviaturaView kv;
    private EmojiPanel ep;
    private BuferPanel bp;
    private TahrirPanel tp;
    private Lugat lugat;
    private Ovoz ovoz;
    private OvozYozuvchi yozuvchi;
    private Vibrator vib;
    private AudioManager am;

    private final List<List<Tartib.Qator>> tillar = new ArrayList<>();
    private final List<Integer> tilKodlari = new ArrayList<>();
    private int tilIndeks = 0;
    private List<Tartib.Qator> belgi1, belgi2;
    private int sahifa = 0; // 0 harflar, 1 ?123, 2 =\<

    private long oxirgiShift, oxirgiBoshliq, sozlamaVaqti = -1;
    private boolean maxfiy;           // parol, email, havola - takliflarsiz
    private boolean takliflarYoq, avtotuzatish, avtoKattaYoq, ikkiBoshliq, tovush;
    private int tebranish;
    private String tovushTuri = "standart";
    private Tovushlar tovushlar;

    // avtomatik tuzatishni qaytarish uchun
    private String tuzAsl, tuzYangi, tuzAjrat;
    private final Set<String> tuzatilmasin = new HashSet<>();

    private final Runnable xabarYop = new Runnable() {
        @Override public void run() { if (ab != null) { ab.asboblar(); takliflarniYangila(); } }
    };

    // ======================================================================
    //  Ko'rinish
    // ======================================================================

    @Override
    public void onCreate() {
        super.onCreate();
        Sinxron.xatoUshlagich(this);
        lugat = Lugat.ol(this);
        Sinxron.qoshimchaniYukla(this);
        ovoz = new Ovoz(this, this);
        yozuvchi = new OvozYozuvchi(this, this);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        tovushlar = new Tovushlar(this);
    }

    @Override
    public View onCreateInputView() {
        KlaviaturaView.mavzu(this);
        sozlamalarniOqi();
        boolean raqamlar = Sozlamalar.b(this, Sozlamalar.RAQAMLAR);
        tillar.clear();
        tilKodlari.clear();
        tilKodlari.add(Tartib.UZ);
        if (Sozlamalar.b(this, Sozlamalar.INGLIZCHA)) tilKodlari.add(Tartib.EN);
        if (Sozlamalar.b(this, Sozlamalar.RUSCHA)) tilKodlari.add(Tartib.RU);
        for (int til : tilKodlari) tillar.add(Tartib.harflar(til, raqamlar));
        float jami = 0;
        for (Tartib.Qator q : tillar.get(0)) jami += q.bal;
        belgi1 = Tartib.belgi1(jami / 4);
        belgi2 = Tartib.belgi2(jami / 4);
        if (tilIndeks >= tillar.size()) tilIndeks = 0;

        if (bp != null) bp.yopil();
        ab = new Asboblar(this, this);
        kv = new KlaviaturaView(this, this);
        ep = new EmojiPanel(this, this);
        bp = new BuferPanel(this, this);
        tp = new TahrirPanel(this, this);

        FrameLayout konteyner = new FrameLayout(this);
        konteyner.addView(kv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        for (View v : new View[]{ep, bp, tp}) {
            v.setVisibility(View.GONE);
            konteyner.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        }
        LinearLayout ildiz = new LinearLayout(this);
        ildiz.setOrientation(LinearLayout.VERTICAL);
        ildiz.setBackgroundColor(KlaviaturaView.FON);
        ildiz.addView(ab, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ildiz.addView(konteyner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        sahifa = 0;
        kv.setSahifa(tillar.get(tilIndeks));
        ab.yangiVersiya(Yangilash.yangiBor(this));
        return ildiz;
    }

    private void sozlamalarniOqi() {
        sozlamaVaqti = Sozlamalar.ozgardi(this);
        takliflarYoq = !Sozlamalar.b(this, Sozlamalar.TAKLIFLAR);
        avtotuzatish = Sozlamalar.b(this, Sozlamalar.AVTOTUZATISH);
        avtoKattaYoq = !Sozlamalar.b(this, Sozlamalar.AVTO_KATTA);
        ikkiBoshliq = Sozlamalar.b(this, Sozlamalar.IKKI_BOSHLIQ);
        tovush = Sozlamalar.b(this, Sozlamalar.TOVUSH);
        tebranish = Sozlamalar.tebranish(this);
        tovushTuri = Sozlamalar.tovushTuri(this);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (kv == null) return;
        if (KlaviaturaView.mavzu(this) || Sozlamalar.ozgardi(this) != sozlamaVaqti) {
            setInputView(onCreateInputView());
        }
        harflarga();
        int sinf = info.inputType & InputType.TYPE_MASK_CLASS;
        int tur = info.inputType & InputType.TYPE_MASK_VARIATION;
        maxfiy = sinf != InputType.TYPE_CLASS_TEXT
                || tur == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || tur == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || tur == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                || tur == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || tur == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                || tur == InputType.TYPE_TEXT_VARIATION_URI
                || (info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0;
        if (sinf == InputType.TYPE_CLASS_NUMBER || sinf == InputType.TYPE_CLASS_PHONE
                || sinf == InputType.TYPE_CLASS_DATETIME) {
            sahifa = 1;
            kv.setSahifa(belgi1);
        }
        kv.setEnter(enterBelgisi(info));
        kv.setKatta(0);
        tuzAsl = null;
        tuzatilmasin.clear();
        avtoKatta();
        takliflarniYangila();
        yangiVersiyaniAyt();
        if (lugat.tayyormi()) Sinxron.kunlik(this);
    }

    /** Yangi versiya bo'lsa - kuniga bir marta tepada eslatadi. */
    private void yangiVersiyaniAyt() {
        Yangilash.tekshir(this, false, new Yangilash.Natija() {
            @Override public void tayyor(boolean bor) {
                if (ab != null) ab.yangiVersiya(bor);   // klaviatura tepasida yashil "yangilash" tugmasi
                if (!bor || ab == null) return;
                long oxirgi = Yangilash.sp(Klaviatura.this).getLong("aytildi", 0);
                if (System.currentTimeMillis() - oxirgi < 24 * 60 * 60 * 1000L) return;
                Yangilash.sp(Klaviatura.this).edit().putLong("aytildi", System.currentTimeMillis()).apply();
                xabar("🆕 Yangi versiya " + Yangilash.yangiNom(Klaviatura.this)
                        + " — yaşil ⬇ tugmani bosing", 5000);
            }
        });
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        if (kv != null) kv.faolTozala();
        if (ovoz != null && ovoz.ishlayaptimi()) ovoz.toxtat();
        if (yozuvchi != null) yozuvchi.bekor();
    }

    @Override
    public void onDestroy() {
        if (bp != null) bp.yopil();
        if (ovoz != null) ovoz.toxtat();
        super.onDestroy();
    }

    private static String enterBelgisi(EditorInfo ei) {
        if ((ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return "↵";
        switch (ei.imeOptions & EditorInfo.IME_MASK_ACTION) {
            case EditorInfo.IME_ACTION_SEARCH: return "🔍";
            case EditorInfo.IME_ACTION_SEND: return "➤";
            case EditorInfo.IME_ACTION_GO: return "→";
            case EditorInfo.IME_ACTION_NEXT: return "⇥";
            case EditorInfo.IME_ACTION_DONE: return "✓";
            default: return "↵";
        }
    }

    @Override
    public void onUpdateSelection(int oS, int oE, int nS, int nE, int cS, int cE) {
        super.onUpdateSelection(oS, oE, nS, nE, cS, cE);
        avtoKatta();
        takliflarniYangila();
    }

    private int til() {
        return tilKodlari.isEmpty() ? Tartib.UZ : tilKodlari.get(tilIndeks);
    }

    /** Gap boshida avtomatik katta harf. */
    private void avtoKatta() {
        if (kv == null || sahifa != 0 || kv.katta() == 2) return;
        if (avtoKattaYoq) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        boolean kerak = ei != null && ic != null && ei.inputType != InputType.TYPE_NULL
                && ic.getCursorCapsMode(ei.inputType) != 0;
        kv.setKatta(kerak ? 1 : 0);
    }

    // ======================================================================
    //  So'z takliflari va avtomatik tuzatish
    // ======================================================================

    private static boolean sozHarfi(char c) {
        return Character.isLetter(c) || c == Ogir.TUTUQ;
    }

    /** Kursordan oldingi tugallanmagan so'z. */
    private String joriySoz() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence s = ic.getTextBeforeCursor(40, 0);
        if (s == null) return "";
        int i = s.length();
        while (i > 0 && sozHarfi(s.charAt(i - 1))) i--;
        String w = s.subSequence(i, s.length()).toString();
        while (w.startsWith(String.valueOf(Ogir.TUTUQ))) w = w.substring(1);
        return w;
    }

    private boolean takliflarKerak() {
        return !takliflarYoq && !maxfiy && til() == Tartib.UZ && sahifa == 0
                && kv != null && kv.getVisibility() == View.VISIBLE;
    }

    /** Taklifni yozilgan so'z shakliga moslash (Katta harf, HAMMASI KATTA). */
    private static String holatMos(String asl, String s) {
        if (asl.isEmpty()) return s;
        if (asl.length() > 1 && asl.equals(asl.toUpperCase(Locale.ROOT)) && !asl.equals(asl.toLowerCase(Locale.ROOT))) {
            return s.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(asl.charAt(0))) {
            return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
        }
        return s;
    }

    private void takliflarniYangila() {
        if (ab == null || ab.xabarmi()) return;
        if (!takliflarKerak()) { ab.asboblar(); return; }
        String w = joriySoz();
        if (w.isEmpty() || !lugat.tayyormi()) { ab.asboblar(); return; }
        String kichik = w.toLowerCase(Locale.ROOT);
        String tuz = lugat.tuzat(w);
        List<String> oddiy = lugat.takliflar(w, 4);
        List<String> boshqa = new ArrayList<>();
        for (String s : oddiy) if (!s.equals(kichik) && !s.equals(tuz)) boshqa.add(s);
        List<String> chiq = new ArrayList<>();
        int asosiy;
        if (tuz != null) {
            chiq.add(w);
            chiq.add(holatMos(w, tuz));
            if (!boshqa.isEmpty()) chiq.add(holatMos(w, boshqa.get(0)));
            asosiy = 1;
        } else {
            chiq.add(w);
            for (int i = 0; i < boshqa.size() && chiq.size() < 3; i++) chiq.add(holatMos(w, boshqa.get(i)));
            // o'rtadagi eng ehtimolli taklif bo'lsin
            if (chiq.size() >= 2) { String a = chiq.get(0); chiq.set(0, chiq.get(1)); chiq.set(1, a); }
            asosiy = -1;
        }
        ab.takliflar(chiq, asosiy);
    }

    @Override
    public void taklifTanlandi(String soz) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String w = joriySoz();
        ic.beginBatchEdit();
        if (!w.isEmpty()) ic.deleteSurroundingText(w.length(), 0);
        ic.commitText(soz + " ", 1);
        ic.endBatchEdit();
        lugat.orgat(soz);
        tuzatilmasin.add(soz.toLowerCase(Locale.ROOT));
        tuzAsl = null;
        if (kv.katta() == 1) kv.setKatta(0);
        avtoKatta();
    }

    /** Bo'shliq yoki tinish belgisidan oldin: so'zni tuzatish va o'rganish. */
    private void sozTugadi(InputConnection ic, String ajratuvchi) {
        if (maxfiy || til() != Tartib.UZ) return;
        String w = joriySoz();
        if (w.length() < 2) return;
        String kichik = w.toLowerCase(Locale.ROOT);
        if (avtotuzatish && !tuzatilmasin.contains(kichik)) {
            String tuz = lugat.tuzat(w);
            if (tuz != null) {
                String yangi = holatMos(w, tuz);
                ic.deleteSurroundingText(w.length(), 0);
                ic.commitText(yangi, 1);
                tuzAsl = w;
                tuzYangi = yangi;
                tuzAjrat = ajratuvchi;
                return;
            }
        }
        lugat.orgat(w);
    }

    // ======================================================================
    //  KlaviaturaView
    // ======================================================================

    @Override
    public void matn(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long hozir = System.currentTimeMillis();
        boolean ajratuvchi = s.length() == 1 && " .,!?;:".indexOf(s.charAt(0)) >= 0;
        if (" ".equals(s)) {
            // ikki marta bo'shliq -> ". "
            if (ikkiBoshliq && hozir - oxirgiBoshliq < 700) {
                CharSequence b = ic.getTextBeforeCursor(2, 0);
                if (b != null && b.length() == 2 && b.charAt(1) == ' '
                        && Character.isLetterOrDigit(b.charAt(0))) {
                    ic.deleteSurroundingText(1, 0);
                    ic.commitText(". ", 1);
                    oxirgiBoshliq = 0;
                    tuzAsl = null;
                    avtoKatta();
                    return;
                }
            }
            oxirgiBoshliq = hozir;
        } else {
            oxirgiBoshliq = 0;
        }
        tuzAsl = null;
        ic.beginBatchEdit();
        if (ajratuvchi) sozTugadi(ic, s);
        ic.commitText(s, 1);
        ic.endBatchEdit();
        if (kv.katta() == 1) kv.setKatta(0);
        avtoKatta();
    }

    @Override
    public void maxsus(int kod) {
        InputConnection ic = getCurrentInputConnection();
        if (kod != Tartib.OCHIR && kod != Tartib.SHIFT) tuzAsl = null;
        switch (kod) {
            case Tartib.SHIFT: {
                long hozir = System.currentTimeMillis();
                int k = kv.katta();
                if (k == 0) k = 1;
                else if (k == 1 && hozir - oxirgiShift < 350) k = 2;
                else k = 0;
                oxirgiShift = hozir;
                kv.setKatta(k);
                break;
            }
            case Tartib.OCHIR:
                ochir();
                break;
            case Tartib.BELGI:
                sahifa = 1;
                kv.setSahifa(belgi1);
                takliflarniYangila();
                break;
            case Tartib.BELGI2:
                sahifa = 2;
                kv.setSahifa(belgi2);
                break;
            case Tartib.HARF:
                sahifa = 0;
                kv.setSahifa(tillar.get(tilIndeks));
                avtoKatta();
                takliflarniYangila();
                break;
            case Tartib.ENTER:
                if (ic != null) enter(ic);
                break;
            case Tartib.EMOJI:
                panelKorsat(ep);
                ep.ochildi();
                break;
            case Tartib.TIL:
                if (tillar.size() > 1) {
                    tilIndeks = (tilIndeks + 1) % tillar.size();
                    sahifa = 0;
                    kv.setSahifa(tillar.get(tilIndeks));
                    avtoKatta();
                    takliflarniYangila();
                } else {
                    tilAlmashtir();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void kursor(int qadam) {
        int kod = qadam < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
        for (int i = 0; i < Math.abs(qadam); i++) sendDownUpKeyEvents(kod);
    }

    @Override
    public void tilTanlash() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    @Override
    public void tegdi(int kod) {
        if (tebranish > 0 && vib != null) {
            try {
                int ms = tebranish == 2 ? 28 : 12;
                if (Build.VERSION.SDK_INT >= 26) {
                    vib.vibrate(VibrationEffect.createOneShot(ms, tebranish == 2 ? 200 : 90));
                } else {
                    vib.vibrate(ms);
                }
            } catch (Exception ignored) { }
        }
        if (tovush) {
            try { tovushlar.chal(tovushTuri, kod); } catch (Exception ignored) { }
        }
    }

    private void tilAlmashtir() {
        boolean almashdi = false;
        if (Build.VERSION.SDK_INT >= 28) almashdi = switchToNextInputMethod(false);
        if (!almashdi) tilTanlash();
    }

    private void enter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        int amal = ei == null ? EditorInfo.IME_ACTION_UNSPECIFIED : ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean yangiQator = ei == null || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        sozTugadi(ic, "\n");
        tuzAsl = null;
        if (!yangiQator && amal != EditorInfo.IME_ACTION_NONE && amal != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(amal);
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        }
        oxirgiBoshliq = 0;
        avtoKatta();
    }

    // ======================================================================
    //  Panellar
    // ======================================================================

    private void panelKorsat(View panel) {
        int bal = kv.getHeight();
        if (bal == 0) bal = (int) (270 * getResources().getDisplayMetrics().density);
        kv.faolTozala();
        for (View v : new View[]{ep, bp, tp}) {
            if (v == panel) {
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = bal;
                v.setLayoutParams(lp);
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }
        kv.setVisibility(View.INVISIBLE); // balandlik saqlansin
        ab.asboblar();
    }

    @Override
    public void emoji(String e) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(e, 1);
    }

    @Override
    public void harflarga() {
        for (View v : new View[]{ep, bp, tp}) if (v != null) v.setVisibility(View.GONE);
        if (kv != null) {
            kv.setVisibility(View.VISIBLE);
            if (sahifa != 0) { sahifa = 0; kv.setSahifa(tillar.get(tilIndeks)); }
            avtoKatta();
            takliflarniYangila();
        }
    }

    @Override
    public void ochir() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        // avtomatik tuzatishni bekor qilish: tuzatilgan so'z o'rniga yozilgani qaytadi
        if (tuzAsl != null) {
            String kutilgan = tuzYangi + tuzAjrat;
            CharSequence oldin = ic.getTextBeforeCursor(kutilgan.length(), 0);
            if (oldin != null && oldin.toString().equals(kutilgan)) {
                ic.beginBatchEdit();
                ic.deleteSurroundingText(kutilgan.length(), 0);
                ic.commitText(tuzAsl, 1);
                ic.endBatchEdit();
                tuzatilmasin.add(tuzAsl.toLowerCase(Locale.ROOT));
                lugat.orgat(tuzAsl);
                tuzAsl = null;
                return;
            }
            tuzAsl = null;
        }
        CharSequence tanlangan = ic.getSelectedText(0);
        if (tanlangan != null && tanlangan.length() > 0) {
            ic.commitText("", 1);
            return;
        }
        CharSequence oldin = ic.getTextBeforeCursor(32, 0);
        if (oldin == null || oldin.length() == 0) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            return;
        }
        // emoji va murakkab belgilarni butunligicha o'chirish
        String s = oldin.toString();
        BreakIterator bi = BreakIterator.getCharacterInstance();
        bi.setText(s);
        int oxir = s.length();
        int bosh = bi.preceding(oxir);
        if (bosh == BreakIterator.DONE || bosh >= oxir) bosh = oxir - 1;
        ic.deleteSurroundingText(oxir - bosh, 0);
    }

    @Override
    public void tahrir(int amal, boolean belgilash) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int meta = belgilash ? KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON : 0;
        switch (amal) {
            case TahrirPanel.CHAP: tugma(ic, KeyEvent.KEYCODE_DPAD_LEFT, meta); break;
            case TahrirPanel.ONG: tugma(ic, KeyEvent.KEYCODE_DPAD_RIGHT, meta); break;
            case TahrirPanel.TEPA: tugma(ic, KeyEvent.KEYCODE_DPAD_UP, meta); break;
            case TahrirPanel.PAST: tugma(ic, KeyEvent.KEYCODE_DPAD_DOWN, meta); break;
            case TahrirPanel.BOSH: tugma(ic, KeyEvent.KEYCODE_MOVE_HOME, meta); break;
            case TahrirPanel.OXIR: tugma(ic, KeyEvent.KEYCODE_MOVE_END, meta); break;
            case TahrirPanel.HAMMASI: ic.performContextMenuAction(android.R.id.selectAll); break;
            case TahrirPanel.NUSXA: ic.performContextMenuAction(android.R.id.copy); break;
            case TahrirPanel.KESISH: ic.performContextMenuAction(android.R.id.cut); break;
            case TahrirPanel.QOYISH: ic.performContextMenuAction(android.R.id.paste); break;
            default: break;
        }
    }

    private static void tugma(InputConnection ic, int kod, int meta) {
        long t = SystemClock.uptimeMillis();
        ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, kod, 0, meta));
        ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, kod, 0, meta));
    }

    // ======================================================================
    //  Asboblar satri
    // ======================================================================

    private void xabar(String s, long ms) {
        h.removeCallbacks(xabarYop);
        ab.xabar(s, false);
        h.postDelayed(xabarYop, ms);
    }

    @Override
    public void asbob(int qaysi) {
        switch (qaysi) {
            case Asboblar.BUFER:
                panelKorsat(bp);
                bp.ochildi();
                break;
            case Asboblar.TAHRIR:
                panelKorsat(tp);
                break;
            case Asboblar.OGIR:
                ogir();
                break;
            case Asboblar.MAVZU: {
                String joriy = Sozlamalar.mavzu(this);
                int i = 0;
                for (int j = 0; j < Sozlamalar.MAVZULAR.length; j++) if (Sozlamalar.MAVZULAR[j].equals(joriy)) i = j;
                i = (i + 1) % Sozlamalar.MAVZULAR.length;
                Sozlamalar.s(this, Sozlamalar.MAVZU, Sozlamalar.MAVZULAR[i]);
                KlaviaturaView.mavzu(this);
                setInputView(onCreateInputView());
                harflarga();
                xabar("🎨 " + Sozlamalar.MAVZU_NOMLARI[i], 1500);
                break;
            }
            case Asboblar.SOZLAMA: {
                Intent i = new Intent(this, Sozlash.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
            }
            case Asboblar.OVOZ:
                ovozBoshla();
                break;
            case Asboblar.YANGI: {
                // yangi versiyani yuklab o'rnatish (o'rnatish oynasini faqat ilova oynasi ocha oladi)
                xabar("⬇ Yangi versiya yuklanmoqda...", 2500);
                Intent i = new Intent(this, Sozlash.class);
                i.putExtra("yangila", true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                break;
            }
            default:
                break;
        }
    }

    /** Belgilangan matnni (yoki maydondagi butun matnni) yangi alifboga o'girish. */
    private void ogir() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence tanlangan = ic.getSelectedText(0);
        if (tanlangan != null && tanlangan.length() > 0) {
            ic.commitText(Ogir.yangiga(tanlangan), 1);
            xabar("✅ Belgilangan matn yangi alifboga ōgirildi", 1800);
            return;
        }
        ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
        if (et == null || et.text == null || et.text.length() == 0) {
            xabar("Ōgiriş uçun matn yōq. Kirill yoki eski lotinda yozilgan matnni qōying.", 2500);
            return;
        }
        String eski = et.text.toString();
        String yangi = Ogir.yangiga(eski);
        if (yangi.equals(eski)) {
            xabar("✅ Matn allaqaçon yangi alifboda", 1800);
            return;
        }
        int bosh = et.startOffset;
        ic.beginBatchEdit();
        ic.setSelection(bosh, bosh + eski.length());
        ic.commitText(yangi, 1);
        ic.endBatchEdit();
        xabar("✅ Matn yangi alifboga ōgirildi", 1800);
    }

    // ======================================================================
    //  Ovozli yozish
    // ======================================================================

    private String ovozTili() {
        switch (til()) {
            case Tartib.EN: return "en-US";
            case Tartib.RU: return "ru-RU";
            default: return "uz-UZ";
        }
    }

    private void ovozBoshla() {
        if (yozuvchi.ishlayaptimi()) { yozuvchi.toxtat(); return; }
        if (ovoz.ishlayaptimi()) { ovoz.yetarli(); return; }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(this, Ruxsat.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }
        h.removeCallbacks(xabarYop);
        // 1) sof o'zbekcha: ovoz serverda (Gemini) matnga aylanadi; 2) ishlamasa - telefonning o'z xizmati
        Runnable zaxira = new Runnable() {
            @Override public void run() { telefonOvozi(); }
        };
        String t = til() == Tartib.EN ? "en" : til() == Tartib.RU ? "ru" : "uz";
        if (!yozuvchi.boshla(t, zaxira)) telefonOvozi();
    }

    private void telefonOvozi() {
        if (!Ovoz.mavjud(this)) {
            xabar("Ovozni tanib bōlmadi: internetni tekşiring", 3000);
            return;
        }
        ovoz.boshla(ovozTili());
    }

    private String ovozMatni(String s) {
        return til() == Tartib.UZ ? Ogir.yangiga(s) : s;
    }

    @Override
    public void ovozHolat(String matn) {
        h.removeCallbacks(xabarYop);
        if (ab != null) ab.xabar(ovozMatni(matn), true);
    }

    @Override
    public void ovozNatija(String matn) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            String s = ovozMatni(matn);
            CharSequence oldin = ic.getTextBeforeCursor(1, 0);
            boolean boshida = oldin == null || oldin.length() == 0 || Character.isWhitespace(oldin.charAt(0));
            if (boshida && kv.katta() == 1 && !s.isEmpty()) {
                s = s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
            }
            ic.commitText((boshida ? "" : " ") + s, 1);
        }
        if (ab != null) { ab.asboblar(); takliflarniYangila(); }
    }

    @Override
    public void ovozXato(String sabab) {
        xabar("🎤 " + sabab, 2500);
    }

    @Override
    public void ovozToxtat() {
        if (yozuvchi.ishlayaptimi()) { yozuvchi.toxtat(); return; }
        ovoz.yetarli();
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (!ovoz.ishlayaptimi() && ab != null && ab.xabarmi()) { ab.asboblar(); takliflarniYangila(); }
            }
        }, 4000);
    }
}

package uz.okkin.klaviatura;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/** Ovozli yozish: telefondagi nutqni tanish xizmati orqali. */
final class Ovoz implements RecognitionListener {

    interface Tinglovchi {
        void ovozHolat(String matn);       // "Gapiring...", oraliq natija
        void ovozNatija(String matn);      // yakuniy matn
        void ovozXato(String sabab);
    }

    private final Context c;
    private final Tinglovchi t;
    private SpeechRecognizer sr;
    private boolean ishlayapti;

    Ovoz(Context c, Tinglovchi t) {
        this.c = c;
        this.t = t;
    }

    static boolean mavjud(Context c) {
        return SpeechRecognizer.isRecognitionAvailable(c);
    }

    boolean ishlayaptimi() { return ishlayapti; }

    void boshla(String til) {
        toxtat();
        sr = SpeechRecognizer.createSpeechRecognizer(c);
        sr.setRecognitionListener(this);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, til);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, til);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, c.getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        ishlayapti = true;
        t.ovozHolat("🎤 Gapiring...");
        sr.startListening(i);
    }

    void toxtat() {
        ishlayapti = false;
        if (sr != null) {
            try { sr.cancel(); sr.destroy(); } catch (Exception ignored) { }
            sr = null;
        }
    }

    void yetarli() { // gapirib bo'ldim - natijani kutamiz
        if (sr != null) {
            try { sr.stopListening(); } catch (Exception ignored) { }
        }
    }

    private static String birinchi(Bundle b) {
        ArrayList<String> r = b == null ? null : b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return r == null || r.isEmpty() ? null : r.get(0);
    }

    @Override public void onReadyForSpeech(Bundle params) { t.ovozHolat("🎤 Gapiring..."); }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { t.ovozHolat("⏳ Tanilmoqda..."); }
    @Override public void onEvent(int eventType, Bundle params) { }

    @Override
    public void onPartialResults(Bundle b) {
        String s = birinchi(b);
        if (s != null && !s.isEmpty()) t.ovozHolat(s);
    }

    @Override
    public void onResults(Bundle b) {
        ishlayapti = false;
        String s = birinchi(b);
        toxtat();
        if (s == null || s.isEmpty()) t.ovozXato("Hech narsa eşitilmadi");
        else t.ovozNatija(s);
    }

    @Override
    public void onError(int kod) {
        ishlayapti = false;
        toxtat();
        String s;
        switch (kod) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: s = "Hech narsa eşitilmadi"; break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: s = "Internet yōq — ovozni tanib bōlmadi"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: s = "Mikrofonga ruxsat kerak"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: s = "Ovoz xizmati band, qayta urinib kōring"; break;
            default: s = "Ovozni tanib bōlmadi (" + kod + ")"; break;
        }
        t.ovozXato(s);
    }
}

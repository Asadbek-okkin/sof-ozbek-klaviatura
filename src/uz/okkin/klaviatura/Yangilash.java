package uz.okkin.klaviatura;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yangi versiyani tekshirish va o'rnatish.
 * Manba: GitHub reliz (server orqali): https://inok.uz/claude-pc/reliz/sof-ozbek-klaviatura
 * Reliz nomi: "Sof Özbek klaviatura 2.1 (8)" - qavs ichida versionCode.
 * Internet FAQAT shu tekshiruv va yuklab olish uchun ishlatiladi.
 */
final class Yangilash {
    static final String MANBA = "https://inok.uz/claude-pc/reliz/sof-ozbek-klaviatura";
    private static final long ORALIQ = 6 * 60 * 60 * 1000L; // 6 soatda bir

    interface Natija { void tayyor(boolean yangiBor); }

    private Yangilash() { }

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("yangilash", Context.MODE_PRIVATE);
    }

    static int joriyKod(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static boolean yangiBor(Context c) {
        return sp(c).getInt("kod", 0) > joriyKod(c) && !sp(c).getString("url", "").isEmpty();
    }

    static String yangiNom(Context c) { return sp(c).getString("nom", ""); }

    static String yangiIzoh(Context c) { return sp(c).getString("izoh", ""); }

    /** Fon oqimida tekshiradi. majburiy=false bo'lsa 6 soatda bir martadan ko'p emas. */
    static void tekshir(final Context c, boolean majburiy, final Natija natija) {
        final Context ac = c.getApplicationContext();
        long oxirgi = sp(ac).getLong("vaqt", 0);
        if (!majburiy && System.currentTimeMillis() - oxirgi < ORALIQ) {
            if (natija != null) natija.tayyor(yangiBor(ac));
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                boolean bor = false;
                try {
                    JSONObject r = new JSONObject(ol(MANBA));
                    String nom = r.optString("name", r.optString("tag_name", ""));
                    Matcher m = Pattern.compile("\\((\\d+)\\)").matcher(nom);
                    int kod = m.find() ? Integer.parseInt(m.group(1)) : 0;
                    String url = "";
                    JSONArray as = r.optJSONArray("assets");
                    for (int i = 0; as != null && i < as.length(); i++) {
                        JSONObject a = as.getJSONObject(i);
                        if (a.optString("name").endsWith(".apk")) {
                            url = a.optString("browser_download_url", a.optString("url"));
                            break;
                        }
                    }
                    sp(ac).edit().putLong("vaqt", System.currentTimeMillis()).putInt("kod", kod)
                            .putString("url", url).putString("nom", r.optString("tag_name", ""))
                            .putString("izoh", r.optString("body", "")).apply();
                    bor = yangiBor(ac);
                } catch (Exception ignored) { }
                if (natija != null) {
                    final boolean b = bor;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override public void run() { natija.tayyor(b); }
                    });
                }
            }
        }, "yangilash").start();
    }

    private static String ol(String manzil) throws Exception {
        HttpURLConnection u = (HttpURLConnection) new URL(manzil).openConnection();
        u.setConnectTimeout(10000);
        u.setReadTimeout(15000);
        u.setRequestProperty("User-Agent", "SofOzbekKlaviatura");
        try (InputStream in = u.getInputStream()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } finally {
            u.disconnect();
        }
    }

    /** Yuklab oladi va o'rnatish oynasini ochadi. */
    static void ornat(final Activity a) {
        String url = sp(a).getString("url", "");
        if (url.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(a, "Ruxsat bering: «Bu manbadan ōrnatiş», keyin yana «Yangilaş» ni bosing",
                    Toast.LENGTH_LONG).show();
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + a.getPackageName())));
            return;
        }
        try {
            final DownloadManager dm = (DownloadManager) a.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.setTitle("Sof Özbek klaviatura " + yangiNom(a));
            r.setMimeType("application/vnd.android.package-archive");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalFilesDir(a, Environment.DIRECTORY_DOWNLOADS,
                    "SofOzbekKlaviatura-" + System.currentTimeMillis() + ".apk");
            final long id = dm.enqueue(r);
            Toast.makeText(a, "Yuklanmoqda...", Toast.LENGTH_SHORT).show();
            BroadcastReceiver br = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return;
                    try { c.unregisterReceiver(this); } catch (Exception ignored) { }
                    Uri apk = dm.getUriForDownloadedFile(id);
                    if (apk == null) {
                        Toast.makeText(c, "Yuklab bōlmadi. Internetni tekşirib qayta urinib kōring.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent o = new Intent(Intent.ACTION_VIEW);
                    o.setDataAndType(apk, "application/vnd.android.package-archive");
                    o.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(o);
                }
            };
            IntentFilter f = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= 33) a.getApplicationContext().registerReceiver(br, f, Context.RECEIVER_EXPORTED);
            else a.getApplicationContext().registerReceiver(br, f);
        } catch (Exception e) {
            // zaxira: brauzerda ochamiz
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }
}

package uz.okkin.klaviatura;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

/** Mikrofonga ruxsat so'rash uchun shaffof oyna (klaviaturaning o'zi ruxsat so'ray olmaydi). */
public class Ruxsat extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] r, int[] natija) {
        boolean berildi = natija.length > 0 && natija[0] == PackageManager.PERMISSION_GRANTED;
        Toast.makeText(this, berildi
                ? "Ruxsat berildi. Endi 🎤 ni yana bosing."
                : "Ovozli yozish uçun mikrofonga ruxsat kerak.", Toast.LENGTH_LONG).show();
        finish();
    }
}

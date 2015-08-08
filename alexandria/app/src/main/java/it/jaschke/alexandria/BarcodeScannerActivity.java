package it.jaschke.alexandria;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.zxing.Result;
import com.jwetherell.quick_response_code.DecoderActivity;
import com.jwetherell.quick_response_code.DecoderActivityHandler;

import java.util.Timer;
import java.util.TimerTask;

public class BarcodeScannerActivity extends DecoderActivity {

    private static final String LOG_TAG = BarcodeScannerActivity.class.getSimpleName();

    public static final int BARCODE_RESULT = 100;

    private Timer timer;

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode) {
        drawResultPoints(barcode, rawResult);
        String value = rawResult.getText();
        Log.d(LOG_TAG, "Decoded value: " + value);
        final Intent intent = new Intent().putExtra(Intent.EXTRA_TEXT, value);

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setResult(BARCODE_RESULT, intent);
                finish();
            }
        }, 1000);
    }
}
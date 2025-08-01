package com.example.buddychat.stt;

import com.example.buddychat.R;

import android.os.Handler;
import android.os.Looper;

import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;



// ====================================================================
// Handles Recognized Speech Events
// ====================================================================
public class STTCallbacks implements STTListener {
    private static final String TAG  = "STTCallback";

    // UI references that will be modified
    private final TextView sttView;

    // Handler to hop onto UI thread.
    private final Handler ui = new Handler(Looper.getMainLooper());

    /** Initialization */
    public STTCallbacks(TextView sttView) {
        this.sttView = sttView;
    }


    // --------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------
    @Override
    public void onText(String utterance, float confidence, String rule) {
        ui.post(() -> {
            String log = String.format("%s (conf: %f, rule: %s)", utterance, confidence, rule);
            Log.d(TAG, log);
            sttView.setText(log);
            Toast.makeText(sttView.getContext(), "Speech recognized", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String e) { Log.e(TAG,  " error: " + e); }

}

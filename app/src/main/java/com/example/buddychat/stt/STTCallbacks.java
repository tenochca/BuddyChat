package com.example.buddychat.stt;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.widget.TextView;
import android.widget.Toast;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.example.buddychat.utils.Emotions;
import com.example.buddychat.utils.AudioTracking;

// ====================================================================
// Handles Recognized Speech Events
// ====================================================================
public class STTCallbacks implements STTListener {
    private static final String TAG  = "[DPU_STTCallback]";

    /// UI references that will be modified
    private final TextView          sttView;
    private final UtteranceCallback utteranceCallback;

    /// Handler to hop onto UI thread.
    private final Handler ui = new Handler(Looper.getMainLooper());

    /** Initialization */
    public STTCallbacks(TextView sttView, UtteranceCallback utteranceCallback) {
        this.sttView = sttView;
        this.utteranceCallback = utteranceCallback;
    }

    // --------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------
    @SuppressLint("DefaultLocale")
    @Override
    public void onText(String utterance, float confidence, String rule) {
        ui.post(() -> {
            // Send the message over the WebSocket
            utteranceCallback.sendString(utterance);

            // ToDo: Trying an idea out
            Emotions.setMood(FacialExpression.THINKING);
            float averageAngle = AudioTracking.averageAngle();
            Log.i(TAG, String.format("%s Recent average LocationAngle: %.4f", TAG, averageAngle));

            // Logging the message
            Log.i(TAG, String.format("%s Utt: %s (conf: %.3f, rule: %s)", TAG, utterance, confidence, rule));
            sttView.setText(String.format("User (%.3f): %s", (confidence/1_000), utterance));
            // Toast.makeText(sttView.getContext(), "Speech recognized", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onError(String e) { Log.e(TAG,  " error: " + e); }

}

package com.example.buddychat.network.ws;

import com.example.buddychat.R;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.util.Consumer;

import org.json.JSONObject;
import org.json.JSONException;

// Text-To-Speech wrapper class
import com.example.buddychat.tts.BuddyTTS;

// Emotion Response Handling
import com.example.buddychat.utils.Emotions;
import com.example.buddychat.utils.IntentDetector;

// =======================================================================
// Handles the WebSocket responses
// =======================================================================
// UI updates, logs, start/end button
public class ChatUiCallbacks implements ChatListener {
    private static final String TAG  = "DPU_ChatListener";

    // UI references that will be modified
    private final TextView statusView;
    private final Button   startEndBtn;

    // Let MainActivity know whether chat is running (true/false)
    private final Consumer<Boolean> runningStateSink;

    // Handler to hop onto UI thread.
    private final Handler ui = new Handler(Looper.getMainLooper());

    public ChatUiCallbacks(TextView statusView, Button startEndBtn, Consumer<Boolean> runningStateSink) {
        this.statusView        = statusView;
        this.startEndBtn       = startEndBtn;
        this.runningStateSink  = runningStateSink;
    }

    // -----------------------------------------------------------------------
    // ChatListener
    // -----------------------------------------------------------------------
    @Override public void onOpen() {
        ui.post(() -> {
            runningStateSink.accept(true);  // tells MainActivity
            startEndBtn.setText(R.string.end_chat);
            Toast.makeText(startEndBtn.getContext(), "Chat started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Chat started");
        });
    }

    @Override public void onMessage(String raw) {
        try {
            // Process the data we received & act accordingly
            JSONObject obj  = new JSONObject(raw);
            String type     = obj.optString("type", "");

            switch (type) {
                case "llm_response" : onLLMResponse(obj); break;
                case "affect"       : onAffect     (obj); break;
                case "expression"   : onExpression (obj); break;
            }

        } catch (JSONException e) { Log.e(TAG, String.format("%s Bad JSON: %s", TAG, e.getMessage())); }
    }

    @Override public void onClosed() {
        ui.post(() -> {
            runningStateSink.accept(false);  // tells MainActivity
            startEndBtn.setText(R.string.start_chat);
            Toast.makeText(startEndBtn.getContext(), "Chat ended", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Chat ended");
        });
    }

    @Override public void onError(Throwable t) {
        ui.post(() -> {
            String wsError = String.format("WS error: %s", t.getMessage());
            Toast.makeText(startEndBtn.getContext(), wsError, Toast.LENGTH_LONG).show();
            Log.d(TAG, wsError);
        });
    }

    // -----------------------------------------------------------------------
    // Handle different types of WS messages
    // -----------------------------------------------------------------------
    /** Handle "llm_response" data from the backend (an utterance from the LLM). */
    private void onLLMResponse(JSONObject obj) {
        final String body = obj.optString("data", "(empty)");
        final String time = obj.optString("time", "");

        // ToDo: Testing the "Yes" detection functionality here
        IntentDetector.IntentDetection(body);

        // Hop to UI thread to do actions
        ui.post(() -> {
            Log.i(TAG, String.format("%s %s: %s", TAG, time, body));
            BuddyTTS.speak(body);
            statusView.setText(String.format("Buddy: %s (%s)", body, time));
        });
    }

    /** Handle "affect" data from the backend (valence+arousal emotion values for the face). */
    private static void onAffect(JSONObject obj) {
        final float valence = (float) obj.optDouble("valence", 0.5);
        final float arousal = (float) obj.optDouble("arousal", 0.5);
        Emotions.setMood("NEUTRAL"); // Buddy's expression must be "NEUTRAL" for these values
        Emotions.setPositivityEnergy(valence, arousal);
    }

    /** Handle "expression" data */
    private static void onExpression(JSONObject obj) {
        final String rawExpression = obj.optString("expression", "NEUTRAL");
        Emotions.setMood(rawExpression, 3_000L);
    }

}

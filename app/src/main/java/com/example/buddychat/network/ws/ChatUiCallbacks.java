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

// ====================================================================
// Handles the WebSocket responses
// ====================================================================
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

    // --------------------------------------------------------------------
    // ChatListener
    // --------------------------------------------------------------------
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

            if      ("llm_response".equals(type)) { onLLMResponse(obj); } // Received LLM response
            else if ("affect"      .equals(type)) { onAffect     (obj); } // Received emotion data

        } catch (JSONException e) {
            ui.post(() -> Toast.makeText(
                    startEndBtn.getContext(), "Bad JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
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

    // --------------------------------------------------------------------
    // Handle different types of WS messages
    // --------------------------------------------------------------------
    // For now just "llm_response" & "affect"
    /** Handle "llm_response" data from the backend (an utterance from the LLM). */
    private void onLLMResponse(JSONObject obj) {
        final String body = obj.optString("data", "(empty)");
        final String time = obj.optString("time", "");

        // Hop to UI thread to do actions
        ui.post(() -> {
            // Log the message & display it on the screen
            Log.d(TAG, String.format("%s: %s", time, body));
            statusView.setText(String.format("Buddy: %s (%s)", body, time));

            // Fire off text-to-speech for this message
            BuddyTTS.speak(body);
        });
    }

    /** Handle "affect" data from the backend (valence+arousal emotion values for the face). */
    private static void onAffect(JSONObject obj) {
        final float valence = (float) obj.optDouble("valence", 0.5);
        final float arousal = (float) obj.optDouble("arousal", 0.5);
        Emotions.setPositivityEnergy(valence, arousal);
    }

}

package com.example.buddychat.network.ws;

// Keep existing imports
import com.example.buddychat.MainActivity; // <<< ADD THIS IMPORT
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

public class ChatUiCallbacks implements ChatListener {
    private static final String TAG  = "LISTENER";

    // UI references that will be modified
    private final TextView statusView;

    // Let MainActivity know whether chat is running (true/false)
    private final Consumer<Boolean> runningStateSink;

    // Handler to hop onto UI thread.
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final MainActivity mainActivity;

    public ChatUiCallbacks(MainActivity mainActivity, TextView statusView, Consumer<Boolean> runningStateSink) { // <<< MODIFY CONSTRUCTOR
        this.mainActivity = mainActivity;
        this.statusView = statusView;
        this.runningStateSink = runningStateSink;
    }

    // --------------------------------------------------------------------
    // ChatListener
    // --------------------------------------------------------------------
    @Override public void onOpen() {
        ui.post(() -> {
            runningStateSink.accept(true);  // tells MainActivity
            //Toast.makeText(startEndBtn.getContext(), "Chat started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Chat started");
        });
    }

    @Override public void onMessage(String raw) { // raw is the full JSON string
        try {
            JSONObject obj  = new JSONObject(raw);
            String type     = obj.optString("type", "");

            // Ensure we only process messages intended for display/speech
            if (!"llm_response".equals(type)) {
                Log.d(TAG, "Ignoring message of type: " + type);
                return;
            }

            final String body = obj.optString("data", ""); // This is what we want to speak
            final String time = obj.optString("time", "");

            // Update the UI TextView
            ui.post(() -> {
                Log.d(TAG, String.format("LLM Response for UI [%s]: %s", time, body));
                statusView.setText(String.format("%s \n %s", body, time));

                // --- TTS Integration ---
                // Call MainActivity to handle speaking the response.
                // We pass the 'body' directly as that's the text to speak.
                // If processAndSpeakApiResponse needed the full JSON, you'd pass 'raw'.
                if (!body.trim().isEmpty()) {
                    mainActivity.processAndSpeakApiResponse(body); // TODO CREATE AND CALL METHOD ON MAINACTIVITY
                } else {
                    Log.w(TAG, "LLM response data is empty, nothing to speak.");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON in onMessage: " + e.getMessage() + " | Raw JSON: " + raw);
            //ui.post(() -> Toast.makeText(startEndBtn.getContext(), "Bad JSON from server: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override public void onClosed() {
        ui.post(() -> {
            runningStateSink.accept(false);  // tells MainActivity
            //Toast.makeText(startEndBtn.getContext(), "Chat ended", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Chat ended");
        });
    }

    @Override public void onError(Throwable t) {
        ui.post(() -> {
            String wsError = String.format("WS error: %s", t.getMessage());
            //Toast.makeText(startEndBtn.getContext(), wsError, Toast.LENGTH_LONG).show();
            Log.d(TAG, wsError);
        });
    }
}

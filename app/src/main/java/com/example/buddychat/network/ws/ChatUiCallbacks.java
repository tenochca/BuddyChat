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

// ====================================================================
// Handles the WebSocket responses
// ====================================================================
// UI updates, logs, start/end button
public class ChatUiCallbacks implements ChatListener {
    private static final String TAG  = "LISTENER";

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
            // Process the data we received
            JSONObject obj  = new JSONObject(raw);
            String type     = obj.optString("type", "");
            if (!"llm_response".equals(type)) return;     // ignore other message kinds

            final String body = obj.optString("data", "(empty)");
            final String time = obj.optString("time", "");

            // Hop to UI thread to do actions
            ui.post(() -> {
                // Log the message
                Log.d(TAG, String.format("%s: %s", time, body));
                statusView.setText(String.format("%s \n %s", body, time));

                // Fire off text-to-speech for this message
                BuddyTTS.speak(body);
            });

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
}

package com.example.buddychat.network.ws;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.util.Consumer;

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

    public ChatUiCallbacks(TextView statusView, Button   startEndBtn, Consumer<Boolean> runningStateSink) {
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
            startEndBtn.setText("End Chat");
            Toast.makeText(startEndBtn.getContext(), "Chat started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Chat started");
        });
    }

    @Override public void onMessage(String json) {
        ui.post(() -> {
            statusView.setText(String.format("RX: %s", json));
            Log.d(TAG, String.format("RX: %s", json));
        });
    }

    @Override public void onClosed() {
        ui.post(() -> {
            runningStateSink.accept(false);  // tells MainActivity
            startEndBtn.setText("Start Chat");
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

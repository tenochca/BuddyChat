package com.example.buddychat.network.ws;

import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.*;

import com.example.buddychat.network.NetworkUtils;

// ====================================================================
// WebSocket Manager
// ====================================================================
// Connect, disconnect, send...
// Refers to a separate ChatListener utility for handling messages
public class ChatSocketManager extends WebSocketListener {
    private static final String TAG  = "ChatWS";
    private static final String URL  = "wss://sandbox.cognibot.org/ws/chat/";
    private static final OkHttpClient CLIENT = NetworkUtils.CLIENT;  // reused

    private WebSocket socket;
    private ChatListener listener;

    // --------------------------------------------------------------------
    // Socket control functions
    // --------------------------------------------------------------------
    public void connect(@NonNull String accessToken, @NonNull ChatListener listener) {
        this.endChat();
        this.listener = listener;

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("sandbox.cognibot.org")
                .addPathSegments("ws/chat/")   // keeps the trailing slash
                .addQueryParameter("token",  accessToken)   // raw value
                .addQueryParameter("source", "buddyrobot")
                .build();

        Log.d("WS", String.format("Connecting to: %s", url.toString()));
        Request req = new Request.Builder().url(url).build();
        socket = CLIENT.newWebSocket(req, this);   // async open
    }

    // Called from UI thread
    public void sendJson(String json) { if (socket != null) socket.send(json); }

    public void endChat() {
        if (socket != null) {
            socket.send(String.format("{\"type\":\"end_chat\", \"data\":%s}", System.currentTimeMillis()));
            socket.close(1000, "user ended");
            socket = null;
        }
    }

    // --------------------------------------------------------------------
    // WebSocketListener Callbacks
    // --------------------------------------------------------------------
    @Override public void onOpen(@NonNull WebSocket ws, @NonNull Response res) {
        Log.d(TAG, "Connected");
        if (listener != null) listener.onOpen();
    }

    @Override public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
        if (listener != null) listener.onMessage(text);
    }

    @Override public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
        Log.d(TAG, "Closing: " + reason);
        ws.close(code, reason);
    }

    @Override public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
        if (listener != null) listener.onClosed();
    }

    @Override public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response res) {
        Log.e(TAG, "Error", t);
        if (listener != null) listener.onError(t);
    }
}

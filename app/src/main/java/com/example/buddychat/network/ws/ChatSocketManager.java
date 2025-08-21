package com.example.buddychat.network.ws;

import com.example.buddychat.BuildConfig;

import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.*;

import com.example.buddychat.network.NetworkUtils;

// ====================================================================
// WebSocket Manager
// ====================================================================
/// Connect, disconnect, send...
/// Refers to a separate ChatListener utility for handling messages
public class ChatSocketManager extends WebSocketListener {
    private static final String TAG  = "ChatWS";
    private static final OkHttpClient CLIENT = NetworkUtils.CLIENT;  // reused

    private WebSocket    socket;
    private ChatListener listener;

    // --------------------------------------------------------------------
    // Socket control functions
    // --------------------------------------------------------------------
    public void connect(@NonNull String accessToken, @NonNull ChatListener listener) {
        this.endChat();
        this.listener = listener;

        // URL of the WebSocket server
        HttpUrl url;

        // For local Docker container
        if (BuildConfig.TEST_LOCAL == "1") {
            url = new HttpUrl.Builder().scheme("http").host("10.0.2.2").port(8000).addPathSegments("ws/chat/")
                    .addQueryParameter("token",  accessToken)
                    .addQueryParameter("source", "buddyrobot")
                    .build();
        }
        // For cloud server connection
        else {
             String host = "cognibot.org"; // sandbox.cognibot.org | cognibot.org
             url = new HttpUrl.Builder().scheme("https").host(host).addPathSegments("ws/chat/")
                    .addQueryParameter("token",  accessToken)
                    .addQueryParameter("source", "buddyrobot")
                    .build();
        }

        // Connect
        Log.d("WS", String.format("Connecting to: %s", url.toString()));
        Request req = new Request.Builder().url(url).build();
        socket = CLIENT.newWebSocket(req, this);   // async open
    }

    /** Send a string the WebSocket (called from the main thread) (auto-format as JSON) */
    public void sendString(String text) {
        String stringJson = String.format("{\"type\": \"transcription\", \"data\": \"%s\"}", text);
        sendJson(stringJson);
    }

    /** Send JSON the WebSocket (called from the main thread) */
    public void sendJson(String json) { if (socket != null) socket.send(json); }

    /** End the chat */
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

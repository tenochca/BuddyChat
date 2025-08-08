package com.example.buddychat.network.ws;

// interface for inbound websocket events
public interface ChatListener {
    void onOpen();
    void onMessage(String json);
    void onClosed();
    void onError(Throwable t);
}

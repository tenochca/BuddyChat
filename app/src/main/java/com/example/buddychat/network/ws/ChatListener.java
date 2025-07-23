package com.example.buddychat.network.ws;

// Interface for inbound WebSocket events
public interface ChatListener {
    void onOpen();
    void onMessage(String json);
    void onClosed();
    void onError(Throwable t);
}

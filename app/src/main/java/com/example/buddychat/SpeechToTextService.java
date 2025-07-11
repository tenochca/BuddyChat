package com.example.buddychat;

public interface SpeechToTextService {
    void startListening(SttListener listener);
    void stopListening();
}

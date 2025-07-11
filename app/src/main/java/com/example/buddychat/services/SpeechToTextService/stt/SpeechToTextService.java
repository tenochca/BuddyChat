package com.example.buddychat.services.SpeechToTextService.stt;

public interface SpeechToTextService {
    void startListening(SttListener listener);
    void stopListening();
}

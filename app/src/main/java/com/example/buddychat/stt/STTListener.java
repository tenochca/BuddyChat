package com.example.buddychat.stt;

public interface STTListener {
    void onText (String utterance, float confidence, String rule);
    void onError(String err);
}

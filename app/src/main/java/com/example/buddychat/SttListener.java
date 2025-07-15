package com.example.buddychat;

public interface SttListener {
    //called when speech is successfully recognized
    void onSpeechResult(String utterance, float confidence);

    void onError(String errorMessage);

    //called when the STT engine is ready to listen
    void onSttReady();

    void onSttPaused();

    void onSttStopped();
}

package com.example.buddychat.stt;

import java.util.Locale;

public interface SpeechToTextService {

    //Enum for STT Engine selection
    enum SttEngineType {
        GOOGLE,
        CERENCE_FREE_SPEECH,
        CERENCE_LOCAL_FCF
    }

    //Creates and configures STT task for a specific engine and locale
    void prepareSTTEngine(SttEngineType engineType, Locale locale);

    //initializes the created STT task
    void initializeListening(SttListener listener);

    //starts the listening process, this assumes initializeListening has been called
    void startListening(boolean listenContinuously);

    void pauseListening();

    void stopListening();

    void releaseService();
}

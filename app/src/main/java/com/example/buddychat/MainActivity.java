package com.example.buddychat;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;

import com.example.buddychat.BuddySpeechToTextService;
import com.example.buddychat.SpeechToTextService;
import com.example.buddychat.SttListener;

import java.util.Locale;

public class MainActivity extends BuddyActivity implements SttListener {

    @Override
    public void onSpeechResult(String utterance, float confidence) {

    }

    @Override
    public void onError(String errorMessage) {

    }

    @Override
    public void onSttReady() {

    }

    @Override
    public void onSttPaused() {

    }

    @Override
    public void onSttStopped() {

    }
}

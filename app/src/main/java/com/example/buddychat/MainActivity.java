package com.example.buddychat;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;

import com.example.buddychat.BuddySpeechToTextService;
import com.example.buddychat.SpeechToTextService;
import com.example.buddychat.SttListener;

import java.util.Locale;

public class MainActivity extends BuddyActivity implements SttListener {

    private static final String TAG = "MainActivity";

    private SpeechToTextService sttService;
    private TextView textViewRecognizedText;
    private TextView textViewStatus;
    private Button buttonToggleListen;
    private Button buttonPrepareEngine;
    private Spinner spinnerSttEngine;
    private Spinner spinnerLanguage;
    private CheckBox checkboxContinuousListen;


    private boolean isListening = false;
    private boolean isEnginePrepared = false;
    private boolean isEngineInitialized = false;

    private SpeechToTextService.SttEngineType selectedEngineType = SpeechToTextService.SttEngineType.GOOGLE;
    private Locale selectedLocale = Locale.ENGLISH; // Default

    private final String[] sttEngineDisplayItems = {"Google", "Cerence Free Speech", "Cerence Local .fcf"};
    private final SpeechToTextService.SttEngineType[] sttEngineEnumItems = {
            SpeechToTextService.SttEngineType.GOOGLE,
            SpeechToTextService.SttEngineType.CERENCE_FREE_SPEECH,
            SpeechToTextService.SttEngineType.CERENCE_LOCAL_FCF
    };

    private final Locale[] languageItems = {Locale.ENGLISH, Locale.FRENCH};

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

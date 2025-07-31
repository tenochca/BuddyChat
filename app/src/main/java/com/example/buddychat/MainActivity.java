package com.example.buddychat;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bfr.buddysdk.BuddyActivity;

import com.example.buddychat.stt.BuddySpeechToTextService;
import com.example.buddychat.stt.SpeechToTextService;
import com.example.buddychat.stt.SttListener;

import java.util.Locale;

public class MainActivity extends BuddyActivity implements SttListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    // private static final int PERMISSION_REQ_ID_BUDDY_SDK = 23;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting UI inflation");
        setContentView(R.layout.main_activity);
        Log.d(TAG, "UI inflated");

        textViewRecognizedText = findViewById(R.id.textViewRecognizedText);
        textViewStatus = findViewById(R.id.textViewStatus);
        buttonToggleListen = findViewById(R.id.buttonToggleListen);
        buttonPrepareEngine = findViewById(R.id.buttonPrepareEngine);
        spinnerSttEngine = findViewById(R.id.spinnerSttEngine);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        checkboxContinuousListen = findViewById(R.id.checkboxContinuousListen);

        // We still need to explicitly ask for RECORD_AUDIO
        checkAndRequestRecordAudioPermission();

        // The BuddySpeechToTextService is instantiated AFTER onSDKReady or ensure BuddySDK.isSDKReady()
        // For now, we'll initialize it in onSDKReady.

        buttonToggleListen.setOnClickListener(v -> toggleListeningState());
        buttonPrepareEngine.setOnClickListener(v -> prepareAndInitializeEngine());

        setupSpinners();
        updateUIStates();
    }


    private void checkAndRequestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_ID_RECORD_AUDIO);
        }
    }

    private void setupSpinners() {
        // STT Engine Spinner
        ArrayAdapter<String> sttEngineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sttEngineDisplayItems);
        spinnerSttEngine.setAdapter(sttEngineAdapter);
        spinnerSttEngine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEngineType = sttEngineEnumItems[position];
                // Invalidate current preparation if engine changes
                isEnginePrepared = false;
                isEngineInitialized = false;
                updateUIStates();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Language Spinner
        ArrayAdapter<Locale> languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languageItems);
        spinnerLanguage.setAdapter(languageAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocale = languageItems[position];
                // Invalidate current preparation if language changes
                isEnginePrepared = false;
                isEngineInitialized = false;
                updateUIStates();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onSDKReady() {
        // This is crucial. BuddySDK is ready here.
        Log.i(TAG, "Buddy SDK is Ready!");
        Toast.makeText(this, "Buddy SDK Ready!", Toast.LENGTH_SHORT).show();

        // Now it's safe to instantiate BuddySpeechToTextService
        sttService = new BuddySpeechToTextService(getApplicationContext().getAssets());
        // sttService.initializeService(); // Though in our current implementation, this doesn't do much

        // Enable UI elements that depend on the SDK
        buttonPrepareEngine.setEnabled(true);
        spinnerLanguage.setEnabled(true);
        spinnerSttEngine.setEnabled(true);
        textViewStatus.setText("Status: Buddy SDK Ready. Please prepare an STT engine.");
    }

    private void prepareAndInitializeEngine() {
        if (sttService == null) {
            textViewStatus.setText("Status: Error - STT Service not available (BuddySDK not ready?)");
            Log.e(TAG, "STT Service is null in prepareAndInitializeEngine. BuddySDK might not be ready.");
            return;
        }
        if (!checkRecordAudioPermission()) return;

        textViewStatus.setText("Status: Preparing " + selectedEngineType + " for " + selectedLocale.getDisplayLanguage() + "...");
        sttService.prepareSTTEngine(selectedEngineType, selectedLocale);
        isEnginePrepared = true; // Assume preparation itself is quick or synchronous for now.
        // A more robust solution might have a callback for preparation.

        textViewStatus.setText("Status: Initializing " + selectedEngineType + "...");
        sttService.initializeListening(this); // 'this' activity is the SttListener
        // onSttReady() callback will set isEngineInitialized = true
    }

    private void toggleListeningState() {
        if (!isEngineInitialized) {
            Toast.makeText(this, "Please prepare and initialize the STT Engine first.", Toast.LENGTH_SHORT).show();
            textViewStatus.setText("Status: Engine not initialized. Click 'Prepare Engine'.");
            return;
        }

        if (!checkRecordAudioPermission()) return;

        if (isListening) {
            sttService.stopListening(); // This will trigger onSttStopped -> isListening = false
            // UI update will happen in onSttStopped
        } else {
            textViewRecognizedText.setText(""); // Clear previous text
            boolean continuous = checkboxContinuousListen.isChecked();
            sttService.startListening(continuous);
            isListening = true; // Set immediately, callback might confirm/deny
            textViewStatus.setText("Status: Listening...");
        }
        updateUIStates();
    }

    private boolean checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQ_ID_RECORD_AUDIO);
            Toast.makeText(this, "Microphone permission is required to listen.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void updateUIStates() {
        buttonPrepareEngine.setEnabled(true);
        if (isEngineInitialized) {
            buttonToggleListen.setEnabled(true);
            buttonToggleListen.setText(isListening ? "Stop Listening" : "Start Listening");
        } else {
            buttonToggleListen.setEnabled(false);
            buttonToggleListen.setText("Start Listening");
        }
    }

    //Sttlistener callbacks start here

    @Override
    public void onSpeechResult(final String utterance, final float confidence) {
        runOnUiThread(() -> {
            Log.i(TAG, "onSpeechResult: " + utterance + " (Confidence: " + confidence + ")");
            textViewRecognizedText.append(utterance + " (Conf: " + String.format("%.2f", confidence) + ")\n");
            // If not continuous, stop listening and update UI
            if (!checkboxContinuousListen.isChecked()) {
                isListening = false; // Manually update if stopListening isn't called immediately by service
                textViewStatus.setText("Status: Finished listening.");
                updateUIStates();
            } else {
                textViewStatus.setText("Status: Listening (heard something)...");
            }
        });
    }

    @Override
    public void onError(final String errorMessage) {
        runOnUiThread(() -> {
            Log.e(TAG, "STT onError: " + errorMessage);
            textViewStatus.setText("Status: Error - " + errorMessage);
            Toast.makeText(MainActivity.this, "STT Error: " + errorMessage, Toast.LENGTH_LONG).show();
            isListening = false;
            updateUIStates();
        });
    }

    @Override
    public void onSttReady() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT Engine is Initialized and Ready!");
            textViewStatus.setText("Status: Engine Ready. Click 'Start Listening'.");
            isEngineInitialized = true;
            updateUIStates();
        });
    }

    @Override
    public void onSttPaused() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT Paused");
            textViewStatus.setText("Status: Paused.");
            isListening = false; // Assuming pause means not actively listening for new input
            updateUIStates();
        });
    }

    @Override
    public void onSttStopped() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT Stopped");
            textViewStatus.setText("Status: Stopped. Ready to start again or re-prepare.");
            isListening = false;
            // isEngineInitialized remains true, as the task might just need to be started again.
            // If stopListening in service invalidates initialization, then set isEngineInitialized = false
            updateUIStates();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sttService != null && isListening) {
            sttService.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sttService != null) {
            sttService.releaseService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Important for BuddyActivity
        if (requestCode == PERMISSION_REQ_ID_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "RECORD_AUDIO permission granted.");
                // You might want to auto-click prepare or enable button here.
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied.");
                Toast.makeText(this, "Microphone permission denied. STT will not work.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

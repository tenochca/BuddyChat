package com.example.buddychat;

// Android and Support Imports
import android.Manifest;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

// BuddySDK Imports
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;

// Application-specific STT imports
import com.example.buddychat.stt.BuddySpeechToTextService;
import com.example.buddychat.stt.SpeechToTextService;
import com.example.buddychat.stt.SttListener;

// Application-specific Network imports
import com.example.buddychat.network.ws.NetworkUtils; // Assuming this is correct, else adjust
import com.example.buddychat.network.ws.ChatSocketManager;
import com.example.buddychat.network.ws.ChatUiCallbacks;
import com.example.buddychat.network.ws.ChatListener;

// TTS imports
import com.example.buddychat.tts.BuddyTTSManager;

// JSON processing
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends BuddyActivity implements
        SttListener,
        NetworkUtils.AuthCallback {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    // --- UI Elements ---
    private TextView textViewRecognizedText;
    private TextView textViewStatus;
    private TextView textViewChatbotResponse;
    private Button buttonToggleListen;
    private Button buttonPrepareEngine;
    private Button buttonSignIn;
    private Button buttonConnectChat; // New Button
    private Spinner spinnerSttEngine;
    private Spinner spinnerLanguage;
    private CheckBox checkboxContinuousListen;

    // --- STT Service ---
    private SpeechToTextService sttService;
    private boolean isListening = false;
    private boolean isEnginePrepared = false;
    private boolean isEngineInitialized = false;
    private SpeechToTextService.SttEngineType selectedEngineType = SpeechToTextService.SttEngineType.GOOGLE;
    private Locale selectedLocale = Locale.ENGLISH;

    private final String[] sttEngineDisplayItems = {"Google", "Cerence Free Speech", "Cerence Local .fcf"};
    private final SpeechToTextService.SttEngineType[] sttEngineEnumItems = {
            SpeechToTextService.SttEngineType.GOOGLE,
            SpeechToTextService.SttEngineType.CERENCE_FREE_SPEECH,
            SpeechToTextService.SttEngineType.CERENCE_LOCAL_FCF
    };
    private final Locale[] languageItems = {Locale.ENGLISH, Locale.FRENCH};

    // --- Authentication & Networking ---
    private String accessToken;
    private boolean isSignedIn = false;

    // --- WebSocket Chat ---
    private ChatSocketManager chatSocketManager;
    private ChatUiCallbacks chatUiCallbacks;
    private boolean isChatConnected = false;

    // --- TTS Manager ---
    public BuddyTTSManager buddyTTSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "YOU SHOULD BE SEEING THIS");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Log.d(TAG, "UI inflated");

        buddyTTSManager = new BuddyTTSManager(getApplicationContext());

        initializeUI();
        checkAndRequestRecordAudioPermission();

        NetworkUtils.pingHealth();
        chatSocketManager = new ChatSocketManager();

        Consumer<Boolean> chatRunningStateConsumer = running -> {
            isChatConnected = running;
            runOnUiThread(() -> {
                if (isChatConnected) {
                    textViewStatus.setText("Status: Chat connected. Ready to listen.");
                } else {
                    if (isListening) {
                        sttService.stopListening();
                    }
                    textViewStatus.setText("Status: Chat disconnected. Connect to chat or Sign In.");
                }
                updateUIStates(); // This will also update buttonConnectChat text
            });
        };

        // Pass the new buttonConnectChat to ChatUiCallbacks
        chatUiCallbacks = new ChatUiCallbacks(this, textViewChatbotResponse, chatRunningStateConsumer);

        // --- Setup Listeners ---
        buttonSignIn.setOnClickListener(v -> signIn()); // Changed from handleSignInOrConnectChat
        //buttonConnectChat.setOnClickListener(v -> handleConnectOrDisconnectChat()); // New handler
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

    private void initializeUI() {
        textViewRecognizedText = findViewById(R.id.textViewRecognizedText);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewChatbotResponse = findViewById(R.id.textViewChatbotResponse);
        buttonToggleListen = findViewById(R.id.buttonToggleListen);
        buttonPrepareEngine = findViewById(R.id.buttonPrepareEngine);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        //buttonConnectChat = findViewById(R.id.buttonConnectChat); // Initialize the new button
        spinnerSttEngine = findViewById(R.id.spinnerSttEngine);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        checkboxContinuousListen = findViewById(R.id.checkboxContinuousListen);
    }

    private void setupSpinners() {
        ArrayAdapter<String> sttEngineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sttEngineDisplayItems);
        spinnerSttEngine.setAdapter(sttEngineAdapter);
        spinnerSttEngine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (selectedEngineType != sttEngineEnumItems[position]) {
                    selectedEngineType = sttEngineEnumItems[position];
                    resetSttEngineStates();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<Locale> languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languageItems);
        spinnerLanguage.setAdapter(languageAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!selectedLocale.equals(languageItems[position])) {
                    selectedLocale = languageItems[position];
                    resetSttEngineStates();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void resetSttEngineStates() {
        if (isListening) {
            sttService.stopListening();
        }
        if (sttService != null) {
            sttService.releaseService();
            sttService = new BuddySpeechToTextService(getApplicationContext().getAssets());
        }
        isEnginePrepared = false;
        isEngineInitialized = false;
        isListening = false;
        textViewStatus.setText("Status: Engine/language changed. Please prepare engine.");
        updateUIStates();
    }

    @Override
    public void onSDKReady() {
        Log.i(TAG, "Buddy SDK is Ready!");
        Toast.makeText(this, "Buddy SDK Ready!", Toast.LENGTH_SHORT).show();
        if (sttService == null) {
            sttService = new BuddySpeechToTextService(getApplicationContext().getAssets());
        }

        Log.i(TAG, "Initializing Buddy TTS via manager...");
        if (buddyTTSManager != null) {
            buddyTTSManager.initializeTTS();
        } else {
            Log.w(TAG, "buddyTTSManager is null in onSDKReady. Cannot initialize TTS.");
        }
        textViewStatus.setText("Status: Buddy SDK Ready. Please Sign In.");
        updateUIStates();
    }

    // --- Sign In Logic (Simplified) ---
    private void signIn() {
        if (!isSignedIn) {
            textViewStatus.setText("Status: Signing in...");
            buttonSignIn.setEnabled(false);
            NetworkUtils.login(this);
        } else {
            Toast.makeText(this, "Already signed in.", Toast.LENGTH_SHORT).show();
            // signOut(); // need to implement signOut logic
        }
    }

    // --- AuthCallback ---
    @Override
    public void onSuccess(String token) {
        this.accessToken = token;
        this.isSignedIn = true;
        runOnUiThread(() -> {
            Log.i(TAG, "Sign-In Successful. Access Token: " + token);
            Toast.makeText(MainActivity.this, "Sign-In Successful!", Toast.LENGTH_SHORT).show();
            textViewStatus.setText("Status: Signed In. You can now connect to chat.");
            updateUIStates();
            connectToChat();
            textViewStatus.setText("Status: Connecting to chat...");
            Log.i(TAG, "Connected to chat.");
        });
    }

    @Override
    public void onError(Throwable t) {
        this.accessToken = null;
        this.isSignedIn = false;
        runOnUiThread(() -> {
            Log.e(TAG, "Sign-In Failed", t);
            Toast.makeText(MainActivity.this, "Sign-In Failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            textViewStatus.setText("Status: Sign-In Failed. Please try again.");
            updateUIStates();
        });
    }

    private void connectToChat() {
        if (accessToken == null || accessToken.isEmpty()) {
            Log.w(TAG, "ConnectToChat called but no token.");
            textViewStatus.setText("Status: Error - No access token. Please sign in again.");
            return;
        }
        textViewStatus.setText("Status: Connecting to chat...");
        chatSocketManager.connect(accessToken, chatUiCallbacks);
        // updateUIStates() will be called by the consumer when connection state changes
    }

    private void disconnectFromChat() {
        if (isChatConnected) {
            chatSocketManager.endChat();
            textViewStatus.setText("Status: Disconnecting chat...");
        }
        // updateUIStates() will be called by the consumer
    }

    // --- STT Engine Preparation and Listening Logic ---
    private void prepareAndInitializeEngine() {
        if (sttService == null) {
            textViewStatus.setText("Status: Error - BuddySDK not ready or STT Service unavailable.");
            Log.e(TAG, "STT Service is null or SDK not ready in prepareAndInitializeEngine.");
            Toast.makeText(this, "Buddy SDK not ready. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isSignedIn) {
            textViewStatus.setText("Status: Please Sign In before preparing the engine.");
            Toast.makeText(this, "Please Sign In first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isChatConnected) { // Now explicitly require chat to be connected BEFORE preparing STT
            textViewStatus.setText("Status: Please connect to chat before preparing STT engine.");
            Toast.makeText(this, "Please connect to chat first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkRecordAudioPermission()) return;

        textViewStatus.setText("Status: Preparing " + selectedEngineType + " for " + selectedLocale.getDisplayLanguage() + "...");
        sttService.prepareSTTEngine(selectedEngineType, selectedLocale);
        isEnginePrepared = true;

        textViewStatus.setText("Status: Initializing " + selectedEngineType + " for listening...");
        sttService.initializeListening(this);
    }

    private void toggleListeningState() {
        if (!isSignedIn) {
            Toast.makeText(this, "Please Sign In first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isChatConnected) {
            Toast.makeText(this, "Chat is not connected. Please connect first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!isEngineInitialized) {
            Toast.makeText(this, "STT Engine not initialized. Please 'Prepare Engine'.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkRecordAudioPermission()) return;

        if (isListening) {
            sttService.stopListening();
        } else {
            textViewRecognizedText.setText("");
            textViewChatbotResponse.setText("");
            boolean continuous = checkboxContinuousListen.isChecked();
            sttService.startListening(continuous);
            // isListening will be set by onSttListening or implicitly before speech result
            textViewStatus.setText("Status: Listening...");
        }
        updateUIStates();
    }

    // --- SttListener Callbacks ---
    @Override
    public void onSpeechResult(final String utterance, final float confidence) {
        runOnUiThread(() -> {
            Log.i(TAG, "onSpeechResult: " + utterance + " (Confidence: " + confidence + ")");
            textViewRecognizedText.append(utterance + " (Conf: " + String.format(Locale.US, "%.2f", confidence) + ")\n");

            if (!isChatConnected) {
                Log.w(TAG, "Chat not connected. Cannot send utterance.");
                textViewStatus.setText("Status: Recognized, but chat disconnected.");
                Toast.makeText(MainActivity.this, "Chat not connected. Utterance not sent.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject messageJson = new JSONObject();
                messageJson.put("type", "transcription");
                messageJson.put("data", utterance);
                chatSocketManager.sendJson(messageJson.toString());
                Log.d(TAG, "Sent to WebSocket: " + messageJson.toString());
                textViewStatus.setText("Status: Utterance sent to chat API.");
            } catch (JSONException e) {
                Log.e(TAG, "JSONException while creating WebSocket message: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Error creating message for chat.", Toast.LENGTH_SHORT).show();
            }

            if (!checkboxContinuousListen.isChecked() && isListening) {
                textViewStatus.setText("Status: Finished listening (single utterance).");
                // The STT service should call onSttStopped or onSttPaused if it stops.
                // We rely on those callbacks to set isListening = false.
            } else if (isListening) {
                textViewStatus.setText("Status: Listening (heard something)...");
            }
        });
    }

    @Override
    public void onError(final String errorMessage) { // STT Error
        runOnUiThread(() -> {
            Log.e(TAG, "STT onError: " + errorMessage);
            textViewStatus.setText("Status: STT Error - " + errorMessage);
            Toast.makeText(MainActivity.this, "STT Error: " + errorMessage, Toast.LENGTH_LONG).show();
            isListening = false;
            updateUIStates();
        });
    }

    @Override
    public void onSttReady() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT Engine is Initialized and Ready!");
            textViewStatus.setText("Status: STT Engine Ready. Click 'Start Listening'.");
            isEngineInitialized = true;
            isEnginePrepared = true;
            isListening = false;
            updateUIStates();
        });
    }


    @Override
    public void onSttPaused() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT Paused");
            textViewStatus.setText("Status: STT Paused.");
            isListening = false;
            updateUIStates();
        });
    }

    @Override
    public void onSttStopped() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT Stopped");
            textViewStatus.setText("Status: STT Stopped. Ready to start or re-prepare.");
            isListening = false;
            updateUIStates();
        });
    }

    // --- UI Update Logic ---
    private void updateUIStates() {
        // Sign In Button
        buttonSignIn.setEnabled(!isSignedIn); // Enabled if SDK ready AND not signed in

        // STT Engine Preparation
        // Can only prepare if SDK ready, signed in, AND CHAT IS CONNECTED
        boolean canPrepareStt = isSignedIn && isChatConnected;
        buttonPrepareEngine.setEnabled(canPrepareStt && !isEngineInitialized);
        spinnerLanguage.setEnabled(canPrepareStt && !isEngineInitialized && !isListening);
        spinnerSttEngine.setEnabled(canPrepareStt && !isEngineInitialized && !isListening);

        // STT Listening Button
        boolean canListenStt = isSignedIn && isChatConnected && isEngineInitialized;
        buttonToggleListen.setEnabled(canListenStt);
        buttonToggleListen.setText(isListening ? "Stop Listening" : "Start Listening");
        checkboxContinuousListen.setEnabled(canListenStt && !isListening);
    }

    // --- Permissions ---
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "RECORD_AUDIO permission granted.");
                textViewStatus.setText("Status: Mic permission granted. Sign In and connect to chat.");
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied.");
                Toast.makeText(this, "Microphone permission denied. STT will not work.", Toast.LENGTH_LONG).show();
                textViewStatus.setText("Status: Mic permission denied. STT disabled.");
            }
            updateUIStates();
        }
    }

    // --- TTS Logic ---
    public void processAndSpeakApiResponse(String textToSpeak) {
        // This method now directly receives the text to speak, not the full JSON string.
        Log.i(TAG, "Received text from ChatUiCallbacks to speak: " + textToSpeak);

        if (textToSpeak == null || textToSpeak.trim().isEmpty()) {
            Log.w(TAG, "processAndSpeakApiResponse called with empty or null text. Nothing to speak.");
            runOnUiThread(() -> textViewChatbotResponse.append("\n[Bot: Received an empty response.]"));
            return;
        }

        if (buddyTTSManager != null) {
            Log.i(TAG, "Asking BuddyTTSManager to speak: '" + textToSpeak + "' with locale: " + selectedLocale.getDisplayLanguage());
            buddyTTSManager.speak(textToSpeak, selectedLocale);
        } else {
            Log.e(TAG, "BuddyTTSManager is null in processAndSpeakApiResponse. Cannot speak.");
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "TTS Manager not available. Cannot speak.", Toast.LENGTH_SHORT).show();
                textViewChatbotResponse.append("\n[Bot: TTS system error, cannot speak response.]");
            });
        }
    }

    // --- Activity Lifecycle ---
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
            sttService = null;
        }
        if (chatSocketManager != null) {
            chatSocketManager.endChat();
        }
    }
}

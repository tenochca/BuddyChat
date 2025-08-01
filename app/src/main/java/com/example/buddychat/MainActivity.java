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
import androidx.core.util.Consumer; // For ChatUiCallbacks

// BuddySDK Imports
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK; // Added for isSDKReady check

// Application-specific STT imports
import com.example.buddychat.stt.BuddySpeechToTextService;
import com.example.buddychat.stt.SpeechToTextService;
import com.example.buddychat.stt.SttListener;

// Application-specific Network imports
import com.example.buddychat.network.ws.NetworkUtils; // Corrected package if it's not in .ws
import com.example.buddychat.network.model.Profile;   // For profile fetching
import com.example.buddychat.network.ws.ChatSocketManager;
import com.example.buddychat.network.ws.ChatUiCallbacks;
import com.example.buddychat.network.ws.ChatListener; // ChatSocketManager needs this for its listener

// JSON processing
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends BuddyActivity implements
        SttListener,
        NetworkUtils.AuthCallback { // Implements AuthCallback for login results

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    // --- UI Elements ---
    private TextView textViewRecognizedText;
    private TextView textViewStatus;
    private TextView textViewChatbotResponse; // Added for future use
    private Button buttonToggleListen;
    private Button buttonPrepareEngine;
    private Button buttonSignIn; // Added
    private Spinner spinnerSttEngine;
    private Spinner spinnerLanguage;
    private CheckBox checkboxContinuousListen;

    // --- STT Service ---
    private SpeechToTextService sttService;
    private boolean isListening = false;
    private boolean isEnginePrepared = false; // STT engine selected and configured by prepareSTTEngine
    private boolean isEngineInitialized = false; // STT service initialized by initializeListening (onSttReady callback)
    private SpeechToTextService.SttEngineType selectedEngineType = SpeechToTextService.SttEngineType.GOOGLE;
    private Locale selectedLocale = Locale.ENGLISH;

    private final String[] sttEngineDisplayItems = {"Google", "Cerence Free Speech", "Cerence Local .fcf"};
    private final SpeechToTextService.SttEngineType[] sttEngineEnumItems = {
            SpeechToTextService.SttEngineType.GOOGLE,
            SpeechToTextService.SttEngineType.CERENCE_FREE_SPEECH,
            SpeechToTextService.SttEngineType.CERENCE_LOCAL_FCF
    };
    private final Locale[] languageItems = {Locale.ENGLISH, Locale.FRENCH}; // Add more as needed

    // --- Authentication & Networking ---
    private String accessToken;
    private boolean isSignedIn = false;

    // --- WebSocket Chat ---
    private ChatSocketManager chatSocketManager;
    private ChatUiCallbacks chatUiCallbacks; // Will primarily handle incoming messages (later)
    private boolean isChatConnected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Log.d(TAG, "UI inflated");

        // Initialize UI Elements
        initializeUI();

        checkAndRequestRecordAudioPermission();

        // --- Initialize Networking Components ---
        // NetworkUtils is static, no instance needed. Ping for testing.
        NetworkUtils.pingHealth();

        chatSocketManager = new ChatSocketManager();

        // Consumer for ChatUiCallbacks to update MainActivity's view of WS connection state
        Consumer<Boolean> chatRunningStateConsumer = running -> {
            isChatConnected = running;
            runOnUiThread(() -> {
                if (isChatConnected) {
                    textViewStatus.setText("Status: Chat connected. Ready to listen.");
                } else {
                    // ChatUiCallbacks itself will show "Chat ended" or error toasts.
                    // If STT was active, might need to stop it here.
                    if (isListening) {
                        sttService.stopListening();
                    }
                    textViewStatus.setText("Status: Chat disconnected. Sign in and prepare engine.");
                }
                updateUIStates();
            });
        };
        // Even if not displaying responses yet, ChatSocketManager needs a ChatListener.
        // ChatUiCallbacks updates UI, so it's the right fit.
        // Pass a button that ChatUiCallbacks can manage (e.g., a dedicated connect/disconnect chat button,
        // or let it manage buttonSignIn for now, though its text changes might be odd for a sign-in button).
        // For this step, let's keep it simple and pass buttonSignIn. We can refine this.
        chatUiCallbacks = new ChatUiCallbacks(textViewChatbotResponse, buttonSignIn, chatRunningStateConsumer);


        // --- Setup Listeners ---
        buttonSignIn.setOnClickListener(v -> handleSignInOrConnectChat());
        buttonToggleListen.setOnClickListener(v -> toggleListeningState());
        buttonPrepareEngine.setOnClickListener(v -> prepareAndInitializeEngine());

        setupSpinners();
        updateUIStates(); // Initial UI state
    }

    private void checkAndRequestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_ID_RECORD_AUDIO);
        }
    }

    private void initializeUI() {
        textViewRecognizedText = findViewById(R.id.textViewRecognizedText);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewChatbotResponse = findViewById(R.id.textViewChatbotResponse); // Initialize the new TextView
        buttonToggleListen = findViewById(R.id.buttonToggleListen);
        buttonPrepareEngine = findViewById(R.id.buttonPrepareEngine);
        buttonSignIn = findViewById(R.id.buttonSignIn); // Initialize the new Button
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
            sttService.stopListening(); // Stop if running
        }
        if (sttService != null) {
            sttService.releaseService(); // Release current engine
            // Re-instantiate if necessary, or ensure prepareSTTEngine handles being called again
            sttService = new BuddySpeechToTextService(getApplicationContext().getAssets());
        }
        isEnginePrepared = false;
        isEngineInitialized = false;
        isListening = false;
        textViewStatus.setText("Status: Engine/language changed. Please prepare engine.");
        updateUIStates();
    }

    // --- SDK Ready ---
    @Override
    public void onSDKReady() {
        Log.i(TAG, "Buddy SDK is Ready!");
        Toast.makeText(this, "Buddy SDK Ready!", Toast.LENGTH_SHORT).show();

        // Instantiate STT Service now that SDK is ready
        sttService = new BuddySpeechToTextService(getApplicationContext().getAssets());

        textViewStatus.setText("Status: Buddy SDK Ready. Please Sign In.");
        updateUIStates(); // Update button states now that SDK is ready
    }

    // --- Sign In and Chat Connection Logic ---
    private void handleSignInOrConnectChat() {
        if (!isSignedIn) {
            textViewStatus.setText("Status: Signing in...");
            buttonSignIn.setEnabled(false); // Disable while signing in
            NetworkUtils.login(this); // 'this' is the AuthCallback
        } else if (!isChatConnected) {
            // Already signed in, but chat not connected. Attempt to connect.
            connectToChat();
        } else {
            // Already signed in AND chat connected. Maybe disconnect? Or do nothing.
            // For now, let's make it disconnect chat.
            disconnectFromChat();
        }
    }

    @Override // NetworkUtils.AuthCallback
    public void onSuccess(String token) { // Login successful
        this.accessToken = token;
        this.isSignedIn = true;
        runOnUiThread(() -> {
            Log.i(TAG, "Sign-In Successful. Access Token: " + token);
            Toast.makeText(MainActivity.this, "Sign-In Successful!", Toast.LENGTH_SHORT).show();
            textViewStatus.setText("Status: Signed In. Connecting to chat...");
            // Automatically try to connect to chat after successful sign-in
            connectToChat();
            // Optionally fetch profile
            // fetchUserProfile();
            updateUIStates();
        });
    }

    @Override // NetworkUtils.AuthCallback
    public void onError(Throwable t) { // Login failed
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
        if (!isSignedIn || accessToken == null || accessToken.isEmpty()) {
            Log.w(TAG, "ConnectToChat called but not signed in or no token.");
            textViewStatus.setText("Status: Please sign in first.");
            Toast.makeText(this, "Please sign in to connect to chat.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isChatConnected) {
            Log.i(TAG, "Chat already connected.");
            return;
        }
        textViewStatus.setText("Status: Connecting to chat...");
        // ChatSocketManager expects a ChatListener. ChatUiCallbacks implements this.
        chatSocketManager.connect(accessToken, chatUiCallbacks);
        updateUIStates(); // Update button text for connect/disconnect
    }

    private void disconnectFromChat() {
        if (isChatConnected) {
            chatSocketManager.endChat(); // This will trigger onClosed in ChatUiCallbacks
            // isChatConnected will be set to false by the chatRunningStateConsumer
            textViewStatus.setText("Status: Disconnecting chat...");
        }
        updateUIStates();
    }


    // --- STT Engine Preparation and Listening Logic ---
    private void prepareAndInitializeEngine() {
        if (!(sttService == null)) {
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
        // Chat should be connected before preparing engine ideally, or at least before listening
        if (!isChatConnected) {
            textViewStatus.setText("Status: Please ensure chat is connected (after sign-in).");
            Toast.makeText(this, "Please ensure chat is connected first.", Toast.LENGTH_SHORT).show();
            // return; // You might choose to allow preparation but not listening
        }

        if (!checkRecordAudioPermission()) return;

        textViewStatus.setText("Status: Preparing " + selectedEngineType + " for " + selectedLocale.getDisplayLanguage() + "...");
        sttService.prepareSTTEngine(selectedEngineType, selectedLocale);
        isEnginePrepared = true; // Mark as prepared. Real preparation might be async with its own callback.

        textViewStatus.setText("Status: Initializing " + selectedEngineType + " for listening...");
        sttService.initializeListening(this); // 'this' activity is the SttListener
        // onSttReady() callback will set isEngineInitialized = true and update UI
    }

    private void toggleListeningState() {
        if (!isSignedIn) {
            Toast.makeText(this, "Please Sign In first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isChatConnected) {
            Toast.makeText(this, "Chat is not connected. Please connect first.", Toast.LENGTH_LONG).show();
            textViewStatus.setText("Status: Chat not connected.");
            // Optionally, try to auto-connect chat here if signed in:
            // if (isSignedIn && !isChatConnected) connectToChat();
            return;
        }
        if (!isEngineInitialized) { // Check if STT engine is actually ready (via onSttReady callback)
            Toast.makeText(this, "STT Engine not initialized. Please 'Prepare Engine'.", Toast.LENGTH_SHORT).show();
            textViewStatus.setText("Status: Engine not initialized. Click 'Prepare Engine'.");
            return;
        }
        if (!checkRecordAudioPermission()) return;

        if (isListening) {
            sttService.stopListening();
            // isListening state and UI will be updated by onSttStopped/onSttPaused
        } else {
            textViewRecognizedText.setText(""); // Clear previous recognized text
            textViewChatbotResponse.setText(""); // Clear previous bot response (for future)
            boolean continuous = checkboxContinuousListen.isChecked();
            sttService.startListening(continuous);
            // Assume listening starts; onSttListening or onSttReady might confirm
            // For now, let's manage isListening primarily through callbacks
            textViewStatus.setText("Status: Listening...");
            // isListening = true; // Better to set this in a callback like onSttListening
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
                return; // Don't proceed to send if chat is not connected
            }

            // --- Send utterance to WebSocket API ---
            try {
                JSONObject messageJson = new JSONObject();
                messageJson.put("type", "transcription"); // As per your example
                messageJson.put("data", utterance);
                // You might want to add other fields like a timestamp or userID if your API needs them

                chatSocketManager.sendJson(messageJson.toString());
                Log.d(TAG, "Sent to WebSocket: " + messageJson.toString());
                textViewStatus.setText("Status: Utterance sent to chat API.");
                // We are not handling the response display in this step.
                // textViewChatbotResponse.setText("Waiting for API response...");

            } catch (JSONException e) {
                Log.e(TAG, "JSONException while creating WebSocket message: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Error creating message for chat.", Toast.LENGTH_SHORT).show();
            }
            // --- End send utterance ---

            if (!checkboxContinuousListen.isChecked() && isListening) {
                // If not continuous, and we were listening, STT might stop itself,
                // or we might need to explicitly stop it. BuddySpeechToTextService behavior dependent.
                // For now, assume if not continuous, one result means we stop.
                // sttService.stopListening(); // This would trigger onSttStopped
                // isListening state will be managed by onSttStopped.
                textViewStatus.setText("Status: Finished listening (single utterance).");
            } else if (isListening) { // Still listening in continuous mode
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
            isEngineInitialized = true; // CRITICAL: This confirms STT is usable
            isEnginePrepared = true;    // If it's ready, it must have been prepared
            isListening = false;      // Not actively listening yet
            updateUIStates();
        });
    }

    // Optional: Add onSttListening if your service provides it for more precise state
    /*
    public void onSttListening() {
        runOnUiThread(() -> {
            Log.i(TAG, "STT is actively listening.");
            isListening = true;
            textViewStatus.setText("Status: Listening...");
            updateUIStates();
        });
    }
    */

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
            // isEngineInitialized remains true typically, unless stop invalidates it.
            updateUIStates();
        });
    }

    // --- UI Update Logic ---
    private void updateUIStates() {

        // Sign In Button
        // It acts as Sign In, Connect Chat, Disconnect Chat based on state
        buttonSignIn.setEnabled(true); // Always enabled if SDK is ready, logic inside handler
        if (!isSignedIn) {
            buttonSignIn.setText("Sign In");
        } else if (!isChatConnected) {
            buttonSignIn.setText("Connect Chat");
        } else {
            buttonSignIn.setText("Disconnect Chat");
        }

        // STT Engine Preparation
        boolean canPrepareStt = sttService != null && isSignedIn; // Require sign-in to prepare
        buttonPrepareEngine.setEnabled(canPrepareStt && !isEngineInitialized); // Can prepare if not already initialized
        spinnerLanguage.setEnabled(canPrepareStt && !isEngineInitialized);
        spinnerSttEngine.setEnabled(canPrepareStt && !isEngineInitialized);

        // STT Listening Button
        boolean canListenStt = isSignedIn && isChatConnected && isEngineInitialized;
        buttonToggleListen.setEnabled(canListenStt);
        buttonToggleListen.setText(isListening ? "Stop Listening" : "Start Listening");
        checkboxContinuousListen.setEnabled(canListenStt && !isListening); // Can change mode if not actively listening
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
                textViewStatus.setText("Status: Mic permission granted. Prepare engine or Sign In.");
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied.");
                Toast.makeText(this, "Microphone permission denied. STT will not work.", Toast.LENGTH_LONG).show();
                textViewStatus.setText("Status: Mic permission denied. STT disabled.");
            }
            updateUIStates();
        }
    }

    // --- Activity Lifecycle ---
    @Override
    protected void onPause() {
        super.onPause();
        if (sttService != null && isListening) {
            sttService.stopListening();
        }
        // Consider if WebSocket should be disconnected onPause if not actively used in background
        // if (isChatConnected) {
        //     disconnectFromChat();
        // }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sttService != null) {
            sttService.releaseService();
            sttService = null;
        }
        if (chatSocketManager != null) {
            chatSocketManager.endChat(); // Ensure chat is ended
        }
        // OkHttpClient used by NetworkUtils and ChatSocketManager might have its own shutdown logic if needed
        // but typically you don't explicitly shut down the shared client unless app is fully exiting and
        // you need to release all resources immediately.
    }
}

package com.example.buddychat;

import android.os.Bundle;
import android.util.Log;
import androidx.core.util.Consumer;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.util.Locale;

// Buddy SDK
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.BuddyActivity;

// Speech System
import com.example.buddychat.network.NetworkUtils;
import com.example.buddychat.network.model.Profile;
import com.example.buddychat.network.ws.ChatSocketManager;
import com.example.buddychat.network.ws.ChatUiCallbacks;

// BuddySDK.Speech wrappers
import com.example.buddychat.stt.STTCallbacks;
import com.example.buddychat.tts.BuddyTTS;
import com.example.buddychat.stt.BuddySTT;
import com.example.buddychat.stt.BuddySTT.Engine;

/// Okay so we are just going to restart here from scratch.
/// Okay we have the login, next step is doing the websocket connection.
/// This we will still do with a button. => clean up the current code first before we don any nonsense...

// ==================================================================== ===========================
// Main Activity of the app; runs on startup
// ==================================================================== ===========================
public class ChatActivity extends BuddyActivity {
    /// Constants
    private static final String TAG = "MAIN";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    // --------------------------------------------------------------------
    // Persistent variables
    // --------------------------------------------------------------------
    /// UI References
    private TextView textUserInfo;
    private TextView textStatus;
    private TextView sttView;
    private Button   buttonHello;
    private Button   buttonStartEnd;
    private Button   buttonToggleTTS;

    /// WebSocket related
    private volatile String            authToken;
    private          boolean           isRunning = false;
    private final    ChatSocketManager chat      = new ChatSocketManager();

    // --- I feel like this is something else
    private          ChatUiCallbacks   chatCallbacks;
    private          STTCallbacks      sttCallbacks;

    // ==================================================================== ===========================
    // Startup code
    // ==================================================================== ===========================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /// Setup the app & layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TEST", "App running");

        /// Setup the UI
        initializeUI(); wireButtons();

        /// WebSocket callback object & STT callback object (we can pass it stuff here, like the textView)
        chatCallbacks = new ChatUiCallbacks(textStatus, buttonStartEnd, (Consumer<Boolean>) running -> isRunning = running);
        sttCallbacks  = new STTCallbacks(sttView);

        /// Login automatically on app startup (good to initialize the UI before this then, so we can set the result)
        Log.d("API", "Attempting login...");
        Toast.makeText(this, "Attempting login...", Toast.LENGTH_SHORT).show();
        doLoginAndProfile();
    }

    // --------------------------------------------------------------------
    // Called when the BuddyRobot SDK is ready
    // --------------------------------------------------------------------
    @Override
    public void onSDKReady() {
        /// --- Might want to move button/UI setup into here....
        Log.d("SDK", "Buddy SDK ready --------------");
        Toast.makeText(this, "Buddy SDK Ready!", Toast.LENGTH_SHORT).show();

        // Transfer the touch information to BuddyCore in the background
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));

        // Setup STT & TTS
        BuddySTT.init(this, Locale.ENGLISH, Engine.GOOGLE, true);
        BuddyTTS.init(getApplicationContext());
    }

    /** Came from the STT example... not sure if needed? */
    @Override
    public void onResume() { super.onResume(); }

    // ==================================================================== ===========================
    // UI Setup
    // ==================================================================== ===========================
    /** Get each UI element (not sure if this is the best way to do this) */
    private void initializeUI() {
        /// Text boxes
        textUserInfo   = findViewById(R.id.textUserInfo);
        textStatus     = findViewById(R.id.textStatus  );
        sttView        = findViewById(R.id.sttView     );

        /// Button setup
        buttonHello     = findViewById(R.id.buttonHello    );
        buttonStartEnd  = findViewById(R.id.buttonStartEnd );
        buttonToggleTTS = findViewById(R.id.buttonToggleTTS);
    }

    // --------------------------------------------------------------------
    // Button Listeners
    // --------------------------------------------------------------------
    /** Enable button functionality */
    private void wireButtons() {
        /// Start/Stop button
        buttonStartEnd.setOnClickListener(v -> {
            if (!isRunning) { chat.connect(authToken, chatCallbacks);           }
            else            { chat.endChat(); BuddyTTS.stop(); BuddySTT.stop(); }
        });

        /// Hello button
        buttonHello.setOnClickListener(view -> {
            String testMessage = "{\"type\":\"transcription\",\"data\":\"Hello, how are you doing today?\"}";
            Log.d("WS", String.format("Sending: %s", testMessage));
            chat.sendJson(testMessage);
        });


        /// Toggle TTS button ---- using this to toggle both right now ----
        buttonToggleTTS.setOnClickListener(v -> {
            BuddyTTS.toggle();
            //BuddySTT.start(sttCallbacks);
        });
    }







    // ====================================================================
    // Handle API requests for logging in
    // ====================================================================
    // Adds some stuff to the UI (username, etc), and sets the auth token.
    private void doLoginAndProfile() {
        /// Test the API
        NetworkUtils.pingHealth();

        /// Login
        NetworkUtils.login(new NetworkUtils.AuthCallback() {
            @Override public void onSuccess(String token) {
                authToken = token;
                Log.d("LOGIN", "Token ok, fetching profile...");

                NetworkUtils.fetchProfile(token, new NetworkUtils.ProfileCallback() {
                    @Override public void onSuccess(Profile p) {
                        runOnUiThread(() -> textUserInfo.setText(String.format("%s %s | %s", p.plwd.first_name, p.plwd.last_name, p.plwd.username)));
                        runOnUiThread(() -> textStatus  .setText(String.format("Welcome %s", p.plwd.username)));
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, String.format("Welcome %s", p.plwd.username), Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onError(Throwable t) {runOnUiThread(() -> textStatus.setText(String.format("Profile fail: %s", t.getMessage())));}
                });
            }

            @Override public void onError(Throwable t) {
                Log.d("LOGIN", String.format("Login failed: %s", t.getMessage()));
                runOnUiThread(() -> textStatus.setText(String.format("Login failed: %s", t.getMessage())));
            }
        });
    }











}

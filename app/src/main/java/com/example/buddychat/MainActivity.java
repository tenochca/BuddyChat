package com.example.buddychat;

import android.os.Bundle;
import android.util.Log;
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

// ====================================================================
// Main Activity of the app; runs on startup
// ====================================================================
// In the official examples when running on the BuddyRobot, we need to
// overlay the interface of this app on top of the core application. In
// the examples they have code setup to relay clicks from our app UI to
// whatever is below it.
public class MainActivity extends BuddyActivity {
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
    private          ChatUiCallbacks   chatCallbacks;
    private          STTCallbacks      sttCallbacks;

    // ====================================================================
    // Startup code
    // ====================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /// Setup the app & layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TEST", "App running");

        /// Setup UI
        initializeUI(); wireButtons();

        /// WebSocket callback object
        chatCallbacks = new ChatUiCallbacks(textStatus, buttonStartEnd, running -> isRunning = running);

        /// STT callback object (we can pass it stuff here, like the textView)
        sttCallbacks = new STTCallbacks(sttView, chat::sendString);

        /// Login + set tokens
        Log.d("API", "Logging in on app startup...");
        textStatus.setText(R.string.logging_in);
        doLoginAndProfile();
    }

    // ====================================================================
    // Called when the BuddyRobot SDK is ready
    // ====================================================================
    @Override
    public void onSDKReady() {
        Log.d("SDK", "Buddy SDK ready --------------");
        Toast.makeText(this, "Buddy SDK Ready!", Toast.LENGTH_SHORT).show();

        // Transfer the touch information to BuddyCore in the background
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));

        // Setup STT & TTS
        BuddyTTS.init(getApplicationContext());
        BuddySTT.init(this, Locale.ENGLISH, Engine.GOOGLE, true);

    }

    /** Came from the STT example... not sure if needed? */
    @Override
    public void onResume() { super.onResume(); }

    // ====================================================================
    // UI Elements
    // ====================================================================
    /** Initialize UI element references */
    private void initializeUI() {
        textUserInfo   = findViewById(R.id.textUserInfo);
        textStatus     = findViewById(R.id.textStatus  );
        sttView        = findViewById(R.id.sttView     );

        // Button setup
        buttonHello     = findViewById(R.id.buttonHello    );
        buttonStartEnd  = findViewById(R.id.buttonStartEnd );
        buttonToggleTTS = findViewById(R.id.buttonToggleTTS);
    }

    /** Set button listeners */
    private void wireButtons() {
        /// Hello button
        buttonHello.setOnClickListener(view -> {chat.sendString("Hello, how are you doing today?");});
        
        /// Start/Stop button
        buttonStartEnd.setOnClickListener(v -> {
            if (!isRunning) { chat.connect(authToken, chatCallbacks);           }
            else            { chat.endChat(); BuddyTTS.stop(); BuddySTT.stop(); }
        });

        /// Toggle TTS button ---- using this to toggle both right now ----
        buttonToggleTTS.setOnClickListener(v -> {
            BuddyTTS.toggle();
            BuddySTT.start(sttCallbacks);
        });
    }



    // ====================================================================
    // Handle API requests for logging in
    // ====================================================================
    /** Adds some stuff to the UI (username, etc), and sets the auth token. */
    private void doLoginAndProfile() {
        /// Test the API
        NetworkUtils.pingHealth();

        /// Login
        NetworkUtils.login(new NetworkUtils.AuthCallback() {
            @Override public void onSuccess(String token) {
                authToken = token;
                runOnUiThread(() -> textStatus.setText(R.string.get_profile));

                NetworkUtils.fetchProfile(token, new NetworkUtils.ProfileCallback() {
                    @Override public void onSuccess(Profile p) {
                        runOnUiThread(() -> textUserInfo.setText(String.format("%s %s | %s", p.plwd.first_name, p.plwd.last_name, p.plwd.username)));
                        runOnUiThread(() -> textStatus  .setText(String.format("Welcome %s", p.plwd.username)));
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, String.format("Welcome %s", p.plwd.username), Toast.LENGTH_SHORT).show());
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




















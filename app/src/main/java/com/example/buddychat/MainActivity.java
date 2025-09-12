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

// Buddy Features
import com.example.buddychat.utils.AudioTracking;
import com.example.buddychat.utils.RotateBody;

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
    private final String TAG = "BuddyChatMain";

    /// UI References
    private TextView textUserInfo;
    private TextView userView;
    private TextView botView;
    private Button   buttonStartEnd;

    /// WebSocket related
    private volatile String            authToken;
    private          boolean           isRunning = false;
    private final    ChatSocketManager chat      = new ChatSocketManager();
    private          ChatUiCallbacks   chatCallbacks;
    private          STTCallbacks      sttCallbacks;

    // ====================================================================
    // Startup code
    // ====================================================================
    // I don't know if maybe all of this should just go into the onSDKReady() function...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /// Setup the app & layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("TEST", "-------------- App running --------------");

        /// Setup UI
        initializeUI(); wireButtons();

        /// WebSocket callback object
        chatCallbacks = new ChatUiCallbacks(botView, buttonStartEnd, running -> isRunning = running);

        /// STT callback object (we can pass it stuff here, like the textView)
        sttCallbacks = new STTCallbacks(userView, chat::sendString);

        /// Login + set tokens
        Log.d("API", "Logging in on app startup...");
        textUserInfo.setText(R.string.logging_in);
        doLoginAndProfile();
    }

    // ====================================================================
    // Called when the BuddyRobot SDK is ready
    // ====================================================================
    @Override
    public void onSDKReady() {
        Log.i("SDK", "-------------- Buddy SDK ready --------------");
        Toast.makeText(this, "Buddy SDK Ready!", Toast.LENGTH_SHORT).show();

        // Transfer the touch information to BuddyCore in the background
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));

        // Setup STT & TTS
        BuddyTTS.init(getApplicationContext());
        BuddySTT.init(this, Locale.ENGLISH, Engine.CERENCE_FREE, true);

        // Setup AudioTracking
        AudioTracking.setupSensors();
        AudioTracking.toggleTracking(false);
    }

    // --------------------------------------------------------------------
    // App Behavior
    // --------------------------------------------------------------------
    // The wheels example project had onStop and onDestroy disable the wheels...
    // I'm doing the emergency stop here too. That function disables the wheels at the end.
    @Override public void onPause  () { super.onPause  (); Log.i(TAG, "onPause"  ); }
    @Override public void onResume () { super.onResume (); Log.i(TAG, "onResume" ); }
    @Override public void onStop   () { super.onStop   (); Log.i(TAG, "onStop"   ); RotateBody.StopMotors(); }
    @Override public void onDestroy() { super.onDestroy(); Log.i(TAG, "onDestroy"); RotateBody.StopMotors(); }


    // ====================================================================
    // UI Elements
    // ====================================================================
    /** Initialize UI element references */
    private void initializeUI() {
        textUserInfo   = findViewById(R.id.textUserInfo  );
        userView       = findViewById(R.id.userView      );
        botView        = findViewById(R.id.botView       );
        buttonStartEnd = findViewById(R.id.buttonStartEnd);
    }

    /** Set button listeners */
    private void wireButtons() {
        buttonStartEnd.setOnClickListener(v -> {
            if (!isRunning) { chat.connect(authToken, chatCallbacks); }
            else            { chat.endChat();                         }
            BuddyTTS.toggle(); BuddySTT.toggle(sttCallbacks);

            // Logging
            String logMsg = "Chat connected; STT & TTS started.";
            if (!isRunning) { logMsg = "Chat ended; STT & TTS paused."; }
            Toast.makeText(this, logMsg, Toast.LENGTH_LONG).show();
        });
    }

    // ====================================================================
    // Handle API requests for logging in
    // ====================================================================
    // TODO: This probably should be put into a separate file
    /** Adds some stuff to the UI (username, etc), and sets the auth token. */
    private void doLoginAndProfile() {
        /// Test the API
        NetworkUtils.pingHealth();

        /// Login
        NetworkUtils.login(new NetworkUtils.AuthCallback() {
            @Override public void onSuccess(String token) {
                authToken = token;
                runOnUiThread(() -> botView.setText(R.string.get_profile));

                NetworkUtils.fetchProfile(token, new NetworkUtils.ProfileCallback() {
                    @Override public void onSuccess(Profile p) {
                        runOnUiThread(() -> textUserInfo.setText(String.format("%s %s | %s", p.plwd.first_name, p.plwd.last_name, p.plwd.username)));
                        runOnUiThread(() -> botView     .setText(String.format("Welcome %s", p.plwd.username)));
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, String.format("Welcome %s", p.plwd.username), Toast.LENGTH_LONG).show());
                    }
                    @Override public void onError(Throwable t) {runOnUiThread(() -> botView.setText(String.format("Profile fail: %s", t.getMessage())));}
                });
            }

            @Override public void onError(Throwable t) {
                Log.d("LOGIN", String.format("Login failed: %s", t.getMessage()));
                runOnUiThread(() -> botView.setText(String.format("Login failed: %s", t.getMessage())));
            }
        });
    }
}






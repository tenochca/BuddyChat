package com.example.buddychat;

import android.os.Bundle;
import android.util.Log;
import androidx.core.util.Consumer;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.bfr.buddysdk.BuddyActivity;

// Speech System
import com.example.buddychat.network.NetworkUtils;
import com.example.buddychat.network.model.Profile;
import com.example.buddychat.network.ws.ChatSocketManager;
import com.example.buddychat.network.ws.ChatUiCallbacks;


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
    // UI References
    private TextView textUserInfo;
    private TextView textStatus;
    private Button   buttonHello;
    private Button   buttonLogin;
    private Button   buttonStartEnd;

    // WebSocket related
    private volatile String            authToken;
    private          boolean           isRunning = false;
    private final    ChatSocketManager chat      = new ChatSocketManager();
    private          ChatUiCallbacks   chatCallbacks;

    // ====================================================================
    // Startup code
    // ====================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup the app & layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TEST", "App running");

        // --------------------------------------------------------------------
        // Get each UI element (not sure if this is the best way to do this)
        // --------------------------------------------------------------------
        textUserInfo   = findViewById(R.id.textUserInfo  );
        textStatus     = findViewById(R.id.textStatus    );

        // Button setup
        buttonHello    = findViewById(R.id.buttonHello   );
        buttonLogin    = findViewById(R.id.buttonLogin   );
        buttonStartEnd = findViewById(R.id.buttonStartEnd);
        wireButtons();

        // WebSocket callback object
         chatCallbacks = new ChatUiCallbacks(textStatus, buttonStartEnd,
                (Consumer<Boolean>) running -> isRunning = running  // lambda target
        );

        // Test the API
        NetworkUtils.pingHealth();
    }

    // ====================================================================
    // Button Listeners
    // ====================================================================
    private void wireButtons() {
        // Hello button
        buttonHello.setOnClickListener(view -> {
            String testMessage = "{\"type\":\"transcription\",\"data\":\"Hello, how are you doing today?\"}";
            Log.d("WS", String.format("Sending: %s", testMessage));
            chat.sendJson(testMessage);
        });

        // Login button
        buttonLogin.setOnClickListener(view -> {
            Log.d("API", "Login button clicked, signing in!");
            NetworkUtils.getTokens();
            textStatus.setText("Signing in...");
            doLoginAndProfile();
        });

        // Start/Stop button
        buttonStartEnd.setOnClickListener(v -> {
            if (!isRunning) { chat.connect(authToken, chatCallbacks); }
            else            { chat.endChat();                         }
        });

    }

    // ====================================================================
    // Handle API requests for logging in
    // ====================================================================
    // Adds some stuff to the UI (username, etc), and sets the auth token.
    private void doLoginAndProfile() {
        NetworkUtils.login(new NetworkUtils.AuthCallback() {
            @Override public void onSuccess(String token) {
                authToken = token;
                runOnUiThread(() -> textStatus.setText("Token ok, fetching profile..."));

                NetworkUtils.fetchProfile(token, new NetworkUtils.ProfileCallback() {
                    @Override public void onSuccess(Profile p) {
                        runOnUiThread(() -> textUserInfo.setText(String.format("%s %s | %s | %s", p.plwd.first_name, p.plwd.last_name, p.plwd.username, p.role)));
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




















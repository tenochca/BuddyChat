package com.example.buddychat.network;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.example.buddychat.R;
import com.example.buddychat.network.model.AuthListener;
import com.example.buddychat.network.model.Profile;

// =======================================================================
// Login & Profile Setter
// =======================================================================
public class LoginAndProfile {
    private static final String  TAG = "[DPU_Login]";

    // Allows us to run UI updates on the main UI thread
    private static final Handler UI  = new Handler(Looper.getMainLooper());

    // Need some way to set the Main token
    public volatile String authToken;

    // UI References
    private final TextView textUserInfo;
    private final TextView botView;

    // Initialization method to get the UI elements
    public LoginAndProfile(TextView textUserInfo, TextView botView) {
        this.textUserInfo = textUserInfo; this.botView = botView;
    }

    // --------------------------------------------------------------------
    // Handles API requests for logging in
    // --------------------------------------------------------------------
    /** Adds some stuff to the UI (username, etc), and sets the auth token. */
    public void doLoginAndProfile(AuthListener listener) {
        Log.d(TAG, String.format("%s Logging in on app startup...", TAG));
        NetworkUtils.pingHealth();  // Test the API
        NetworkUtils.login(new NetworkUtils.AuthCallback() {
            @Override public void onSuccess(String    accessToken) { onLoginSuccess(accessToken); listener.onSuccess(accessToken); }
            @Override public void onError  (Throwable t          ) { onLoginError  (t          ); listener.onError  (t          ); }
        });
    }

    // --------------------------------------------------------------------
    // Login Callbacks
    // --------------------------------------------------------------------
    private void onLoginSuccess(String accessToken) {
        authToken = accessToken;
        UI.post(() -> botView.setText(R.string.get_profile));

        NetworkUtils.fetchProfile(accessToken, new NetworkUtils.ProfileCallback() {
            @Override public void onSuccess(Profile   p) { onProfileSuccess(p); }
            @Override public void onError  (Throwable t) { onProfileError  (t); }
        });
    }
    private void onLoginError(Throwable t) {
        final String eMessage = String.format("%s Login failed: %s", TAG, t.getMessage());
        Log.d(TAG, eMessage);
        UI.post(() -> Toast.makeText(botView.getContext(), eMessage, Toast.LENGTH_LONG).show());
    }

    // --------------------------------------------------------------------
    // Profile Callbacks
    // --------------------------------------------------------------------
    private void onProfileError  (Throwable t) { Log.e(TAG, String.format("%s Profile fetch failed: %s", TAG, t.getMessage())); }
    private void onProfileSuccess(Profile   p) {
        UI.post(() -> {
            textUserInfo.setText(String.format("%s %s | %s", p.plwd.first_name, p.plwd.last_name, p.plwd.username));
            botView     .setText(String.format("Welcome %s", p.plwd.username));
            Toast.makeText(botView.getContext(), String.format("Welcome %s", p.plwd.username), Toast.LENGTH_LONG).show();
        });
    }
}

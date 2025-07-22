package com.example.buddychat;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.buddychat.network.NetworkUtils;


import com.example.buddychat.R;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;


// ====================================================================
// Main Activity of the app; runs on startup
// ====================================================================
// In the official examples when running on the BuddyRobot, we need to
// overlay the interface of this app on top of the core application. In
// the examples they have code setup to relay clicks from our app UI to
// whatever is below it.
public class MainActivity extends BuddyActivity {
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TEST", "App running");
        setContentView(R.layout.activity_main);

        // --------------------------------------------------------------------
        // Text box
        // --------------------------------------------------------------------
        textView  = findViewById(R.id.textView);

        // --------------------------------------------------------------------
        // "Hello" Button
        // --------------------------------------------------------------------
        findViewById(R.id.buttonHello).setOnClickListener(view -> {
            Log.d("TEST", "Hello button clicked!");
            NetworkUtils.pingHealth();
        });

        // --------------------------------------------------------------------
        // Login Button
        // --------------------------------------------------------------------
        findViewById(R.id.buttonLogin).setOnClickListener(view -> {
            Log.d("TEST", "Login button clicked!");
            NetworkUtils.getTokens();
        });

    }
}

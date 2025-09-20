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

import com.bfr.buddy.ui.shared.FacialExpression;

// Speech System
import com.example.buddychat.network.LoginAndProfile;
import com.example.buddychat.network.model.AuthListener;
import com.example.buddychat.network.ws.ChatSocketManager;
import com.example.buddychat.network.ws.ChatUiCallbacks;

// Buddy Features
import com.example.buddychat.utils.AudioTracking;
import com.example.buddychat.utils.Emotions;
import com.example.buddychat.utils.RotateBody;
import com.example.buddychat.utils.HeadMotors;

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
    private static final String TAG = "[DPU_Main]";

    // UI References
    private TextView textUserInfo;   // Username (currently hidden)
    private TextView userView;       // Display user's most recent message
    private TextView botView;        // Display Buddy's most recent message
    private Button   buttonStartEnd; // Start or end the chat/backend websocket connection

    private Button   buttonTester1;  // [Development] Trigger features to be tested
    private Button   buttonTester2;  // [Development] Emergency stop any motors/movements
    private Button   buttonTester3;  // [Development] Trigger features to be tested
    private TextView testView1;

    // WebSocket related
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
        // Setup the app & layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("TEST", "-------------- App running --------------");
        Log.i(TAG, String.format("%s <========== onCreate ==========>", TAG));

        // Setup UI
        initializeUI(); wireButtons();

        // WebSocket callback object
        chatCallbacks = new ChatUiCallbacks(botView, buttonStartEnd, running -> isRunning = running);

        // STT callback object (we can pass it stuff here, like the textView)
        sttCallbacks = new STTCallbacks(userView, testView1, chat::sendString);

        // Login, set auth tokens, and fetch the profile. ToDo: Could also use this to set the profile information here...
        final LoginAndProfile loginAndProfile = new LoginAndProfile(textUserInfo, botView);
        loginAndProfile.doLoginAndProfile(new AuthListener() {
            @Override public void onSuccess(String    token) { authToken = token; }
            @Override public void onError  (Throwable t    ) { Log.e(TAG, "Login failed"); }
        });
    }

    // ====================================================================
    // Called when the BuddyRobot SDK is ready
    // ====================================================================
    @Override
    public void onSDKReady() {
        Log.i("SDK", "-------------- Buddy SDK ready --------------");

        // Transfer the touch information to BuddyCore in the background
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));

        // Setup STT & TTS
        BuddyTTS.init(getApplicationContext());
        BuddySTT.init(this, Locale.ENGLISH, Engine.CERENCE_FREE, true);

        // Call an initial emotion to show (resets back to NEUTRAL)
        Emotions.setMood(FacialExpression.HAPPY, 2_000L);

        // Setup AudioTracking & HeadMotors
        AudioTracking.setupSensors(); // ToDo: Move the callback starting to happen with the WS on/off
        //AudioTracking.toggleUsbCallback();
    }

    // ====================================================================
    // App Behavior
    // ====================================================================
    // The wheels example project had onStop and onDestroy disable the wheels...
    // I'm doing the emergency stop here too. That function disables the wheels at the end.
    @Override public void onPause() {
        super.onPause(); Log.i(TAG, String.format("%s <========== onPause ==========>", TAG));
        //RotateBody.StopMotors();
        //HeadMotors.StopMotors();

        //Emotions.cancelPendingReset();
        //AudioTracking.toggleTracking(false);

        // 2) unregister callbacks BEFORE any vendor command
        //try { BuddySDK.USB.unRegisterCb(AudioTracking.usbCallback); } catch (Exception ignored) {}

    }

    @Override public void onResume () {
        super.onResume ();
        Log.i(TAG, String.format("%s <========== onResume ==========>", TAG));
    }

    // Do NOT call enable/disable motors here.
    //@Override public void onStop   () {super.onStop   (); Log.i(TAG, String.format("%s <========== onStop ==========>", TAG));}

    // Only release our own resources here (handlers, queues, etc.)
    @Override public void onDestroy() {
        super.onDestroy(); Log.i(TAG, String.format("%s <========== onDestroy ==========>", TAG));
        //RotateBody.StopMotors();
        //HeadMotors.StopMotors();
    }


    // ====================================================================
    // UI Elements
    // ====================================================================
    /** Initialize UI element references */
    private void initializeUI() {
        textUserInfo   = findViewById(R.id.textUserInfo  );
        userView       = findViewById(R.id.userView      );
        botView        = findViewById(R.id.botView       );
        buttonStartEnd = findViewById(R.id.buttonStartEnd);

        buttonTester1  = findViewById(R.id.buttonTester1 );
        buttonTester2  = findViewById(R.id.buttonTester2 );
        buttonTester3  = findViewById(R.id.buttonTester3 );
        testView1      = findViewById(R.id.testView1     );
    }

    /** Set button listeners */
    private void wireButtons() {
        // Start or end the chat/backend websocket connection
        buttonStartEnd.setOnClickListener(v -> {
            Log.w(TAG, String.format("%s StartEnd Button pressed", TAG));
            if (!isRunning) { chat.connect(authToken, chatCallbacks); } else { chat.endChat(); }

            BuddyTTS.toggle(); BuddySTT.toggle(sttCallbacks);

            Toast.makeText(this, (isRunning ? "Chat connected; STT & TTS started.": "Chat ended; STT & TTS paused."), Toast.LENGTH_LONG).show();
        });

        // --------------------------------------------------------------------
        // Testing Buttons
        // --------------------------------------------------------------------
        // Testing Button #1: Trigger features to be tested
        buttonTester1.setOnClickListener(v -> {
            Log.w(TAG, String.format("%s Testing Button #1 pressed.", TAG));
            HeadMotors.getHeadMotorStatus();

            HeadMotors.nodYes();

            Emotions.setMood(FacialExpression.ANGRY, 2_000L);

            HeadMotors.getHeadMotorStatus();
        });

        // Testing Button #3: Trigger more features
        buttonTester3.setOnClickListener(v -> {
            Log.w(TAG, String.format("%s Testing Button #3 pressed.", TAG));
            HeadMotors.getHeadMotorStatus();

            HeadMotors.resetYesPosition(); // Reset Yes motor to position 0

            Emotions.setMood(FacialExpression.HAPPY, 2_000L);

            HeadMotors.getHeadMotorStatus();
        });




        // Testing Button #2: Emergency stop any motors/movements
        buttonTester2.setOnClickListener(v -> {
            Log.w(TAG, String.format("%s !!! Emergency Stop Button Activated !!! -------", TAG));
            RotateBody.StopMotors(); RotateBody.emergencyStopped = true;
            HeadMotors.StopMotors(); HeadMotors.emergencyStopped = true;
        });

    }

}

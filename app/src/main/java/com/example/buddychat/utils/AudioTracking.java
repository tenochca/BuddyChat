package com.example.buddychat.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

// ====================================================================
// AudioTracking
// ====================================================================
// ToDo: This implementation is based on the sample projects they have on GitHub. But the SDK shows a way to actually subscribe to the sensors...
public class AudioTracking {
    private static final String TAG = "DPU_AudioTracking";

    private static final int MIN_DECIBELS       = 40;
    private static final int MIN_ANGLE          = 150;
    private static final int UPDATE_INTERVAL_MS = 3_000;

    private static          Handler handler;
    private static volatile boolean enabled = false;

    // Microphone utility
    static float AmbientNoise  = 0; // Sound volume in Decibels
    static float LocationAngle = 0; // Degree location of the sound

    // --------------------------------------------------------------------
    // Setup Continuous Tracking
    // --------------------------------------------------------------------
    /** Setup listening for microphone sensors */
    public static void setupSensors() {
        Log.i(TAG, "Setting up microphone sensors...");

        // After SDK launches we can setup the sensors
        BuddySDK.USB.enableSensorModule(true, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, "-------------- Enabled Sensors --------------"); toggleTracking(true); }
            @Override public void onFailed (String s) { Log.i(TAG, "Fail to Enable sensors:" + s); }
        });
    }

    // --------------------------------------------------------------------
    // Thread Control
    // --------------------------------------------------------------------
    /** Runs the continuous checks for the sensors. */
    private static final Runnable trackingRunnable = new Runnable() {
        @Override public void run() { if (enabled) { updateSensors(); handler.postDelayed(this, UPDATE_INTERVAL_MS); }}
    };

    /** Turn the audio tracking on or off */
    public static void toggleTracking(boolean enable) {
        // Enable everything
        if (enable) {
            if (!enabled) { enabled = true;
                if (handler == null) { handler = new Handler(Looper.getMainLooper()); }
                handler.post(trackingRunnable);
            }
        }
        // Disable tracking
        else { if (enabled) { enabled = false; handler.removeCallbacks(trackingRunnable); } }
    }


    // --------------------------------------------------------------------
    // Check the sensors; act accordingly
    // --------------------------------------------------------------------
    // ToDo: Should use callbacks that disable the audio tracking while it is moving and re-enables it once the Rotate call is completed.
    public static void updateSensors() {
        AmbientNoise  = BuddySDK.Sensors.Microphone().getAmbiantSound();
        LocationAngle = BuddySDK.Sensors.Microphone().getSoundLocalisation();

        if ((AmbientNoise > MIN_DECIBELS) & (LocationAngle > 0)) {
            Log.i(TAG, String.format("Ambient dB: %s, Angle: %s", AmbientNoise, LocationAngle));

            // ToDo: Temporarily calling the Emotions code here to test if it works
            // If noise is loud enough and angle is different enough, rotate buddy to face the source
            if (LocationAngle > MIN_ANGLE) {
                RotateBody.Rotate(10, LocationAngle);
                //Emotions.setPositivityEnergy((float) 0.1, (float) 0.1);
                Emotions.setMood(true);
            }
            else {
                //Emotions.setPositivityEnergy((float) 0.9, (float) 0.9);
                Emotions.setMood(false);
            }
        }
    }

    // No instances
    private AudioTracking() {}
}

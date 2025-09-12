package com.example.buddychat.utils;

import android.util.Log;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

import com.example.buddychat.utils.RotateBody;

// ====================================================================
// AudioTracking
// ====================================================================
// TODO: I feel like the RemoteException stuff is to make the while loop not crash the app? not sure...
public class AudioTracking {
    private static final String TAG          = "DPU_AudioTracking";
    private static final int    MIN_DECIBELS = 40;
    private static final int    MIN_ANGLE    = 40;

    // Microphone utility
    static float AmbientNoise  = 0; // Sound volume in Decibels
    static float LocationAngle = 0; // Degree location of the sound

    // Run the tracking process in a thread
    static boolean enabled  = true;
    static Thread SensorsTh = new Thread(AudioTracking::trackContinuously);

    // --------------------------------------------------------------------
    // Setup Continuous Tracking
    // --------------------------------------------------------------------
    /** Setup listening for microphone sensors */
    public static void setupSensors() {
        Log.i(TAG, "Setting up microphone sensors...");

        // After SDK launches we can setup the sensors
        BuddySDK.USB.enableSensorModule(true, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, "Enabled Sensors"            ); toggleTracking(false); }
            @Override public void onFailed (String s) { Log.i(TAG, "Fail to Enable sensors:" + s); }
        });
    }

    // --------------------------------------------------------------------
    // Thread Control
    // --------------------------------------------------------------------
    public static void trackContinuously() { while (enabled) updateSensors(); }
    // TODO: better way to do toggling here, I don't want to double start the thread
    public static void toggleTracking(boolean toggleOff) {
        if (toggleOff) { enabled = false;                   }
        else           { enabled = true; SensorsTh.start(); }
    }
    // ToDo: Should use callbacks that disable the audio tracking while it is moving and re-enables it once the Rotate call is completed.
    public static void updateSensors() {
        AmbientNoise = BuddySDK.Sensors.Microphone().getAmbiantSound();
        if (AmbientNoise > MIN_DECIBELS) {
            LocationAngle = BuddySDK.Sensors.Microphone().getSoundLocalisation();
            Log.d(TAG, String.format("Ambient dB: %s, Angle: %s", AmbientNoise, LocationAngle));

            // If noise is loud enough and angle is different enough, rotate buddy to face the source
            if (LocationAngle > MIN_ANGLE) { RotateBody.Rotate(30, LocationAngle); }
        }
    }

    // No instances
    private AudioTracking() {}
}

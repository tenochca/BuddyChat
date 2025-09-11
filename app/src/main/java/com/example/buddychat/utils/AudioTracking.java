package com.example.buddychat.utils;

import android.util.Log;
import android.os.RemoteException;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

// ====================================================================
// AudioTracking
// ====================================================================
// TODO: I feel like the RemoteException stuff is to make the while loop not crash the app? not sure...
public class AudioTracking {
    private static final String TAG          = "DPU_AudioTracking";
    private static final int    MIN_DECIBELS = 40;

    // Microphone utility
    float AmbientNoise  = 0; // Sound volume in Decibels
    float LocationAngle = 0; // Degree location of the sound

    // Run the tracking process in a thread
    boolean enabled  = true;
    Thread SensorsTh = new Thread(this::trackContinuously);

    // --------------------------------------------------------------------
    // Setup Continuous Tracking
    // --------------------------------------------------------------------
    /** Setup listening for microphone sensors */
    public void setupSensors() {
        Log.i(TAG, "Setting up microphone sensors...");

        // After SDK launches we can setup the sensors
        BuddySDK.USB.enableSensorModule(true, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) throws RemoteException { Log.i(TAG, "Enabled Sensors"            ); toggleTracking(false); }
            @Override public void onFailed (String s) throws RemoteException { Log.i(TAG, "Fail to Enable sensors:" + s);                   }
        });
    }

    // --------------------------------------------------------------------
    // Thread Control
    // --------------------------------------------------------------------
    public void trackContinuously() { while (enabled) updateSensors(); }
    // TODO: better way to do toggling here, I don't want to double start the thread
    public void toggleTracking   (boolean toggleOff) {
        if (toggleOff) { enabled = false;                   }
        else           { enabled = true; SensorsTh.start(); }
    }
    public void updateSensors() {
        AmbientNoise = BuddySDK.Sensors.Microphone().getAmbiantSound();
        if (AmbientNoise > MIN_DECIBELS) {
            LocationAngle = BuddySDK.Sensors.Microphone().getSoundLocalisation();
            Log.d(TAG, String.format("Ambient dB: %s, Angle: %s", AmbientNoise, LocationAngle));
            // TODO: Call a function/class/object to turn buddy to face the noise.
            // TODO: We will provide it with the callback for toggling tracking
        }
    }

    // No instances
    private AudioTracking() {}
}

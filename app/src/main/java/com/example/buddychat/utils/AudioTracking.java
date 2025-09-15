package com.example.buddychat.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

// BuddySDK imports
import com.bfr.buddy.usb.shared.IUsbAidlCbListener;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;

import com.bfr.buddy.usb.shared.BodySensorData;
import com.bfr.buddy.usb.shared.HeadSensorData;
import com.bfr.buddy.usb.shared.MotorHeadData;
import com.bfr.buddy.usb.shared.MotorMotionData;
import com.bfr.buddy.usb.shared.VocalData;

import com.bfr.buddysdk.BuddySDK;

// ====================================================================
// AudioTracking
// ====================================================================
// ToDo: Maybe collect values for a while, and then if we are supposed to start talking...
// ToDo: Take the average of those values and turn that way.
// ToDo: We know that we are turning towards the audio source because
public class AudioTracking {
    private static final String TAG = "DPU_AudioTracking";

    private static final int MIN_DECIBELS       =    40;
    private static final int MIN_ANGLE          =   150;
    private static final int UPDATE_INTERVAL_MS = 3_000;

    private static          Handler handler;
    private static volatile boolean enabled = false;

    // Microphone utility
    static float AmbientNoise  = 0; // Sound volume in Decibels
    static float LocationAngle = 0; // Degree location of the sound

    // --------------------------------------------------------------------
    // Setup Sensors
    // --------------------------------------------------------------------
    // Needs to be called after the SDK launches
    public static void setupSensors() {
        BuddySDK.USB.enableSensorModule(true, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, "-------------- Enabled Sensors --------------");  }
            @Override public void onFailed (String s) { Log.i(TAG, "Failed to Enable sensors:" + s); }
        });
    }

    // ====================================================================
    // Method #1: Looped "Runnable" call with a set update interval
    // ====================================================================
    /** Runs the continuous checks for the sensors. */
    private static final Runnable trackingRunnable = new Runnable() {
        @Override public void run() { if (enabled) { updateSensors(); handler.postDelayed(this, UPDATE_INTERVAL_MS); }}
    };

    /** Check the sensors. */
    public static void updateSensors() {
        float ambientNoise  = BuddySDK.Sensors.Microphone().getAmbiantSound();
        float locationAngle = BuddySDK.Sensors.Microphone().getSoundLocalisation();
        processAudioData(ambientNoise, locationAngle);
    }

    /** Turn the audio tracking on or off */
    public static void toggleTracking(boolean enable) {
        if (handler == null) { handler = new Handler(Looper.getMainLooper()); } // Make sure the handler is initialized
        // Enable & disable
        if      ( enable & !enabled) { enabled = true;  handler.post           (trackingRunnable); }
        else if (!enable &  enabled) { enabled = false; handler.removeCallbacks(trackingRunnable); }
    }

    // ====================================================================
    // Method #2: Subscriber
    // ====================================================================
    public static final IUsbAidlCbListener usbCallback = new IUsbAidlCbListener.Stub() {
        @Override public void ReceiveMotorMotionData(MotorMotionData motorMotionData) throws RemoteException { }
        @Override public void ReceiveMotorHeadData  (MotorHeadData   motorHeadData  ) throws RemoteException { }
        @Override public void ReceiveHeadSensorData (HeadSensorData  headSensorData ) throws RemoteException { }
        @Override public void ReceiveBodySensorData (BodySensorData  bodySensorData ) throws RemoteException { }

        @Override public void ReceivedVocalData(VocalData vocalData) throws RemoteException {
            float ambientNoise  = vocalData.AmbientSoundLevel;       // Sound level in dB
            float locationAngle = vocalData.SoundSourceLocalisation; // Direction of the sound in degrees
            processAudioData(ambientNoise, locationAngle);
        }
    };

    // Toggle the callback version of AudioTracking
    public static void EnableUsbCallback () { Log.d(TAG, String.format("%s Enabling USB callback",  TAG)); BuddySDK.USB.registerCb  (usbCallback); }
    public static void DisableUsbCallback() { Log.d(TAG, String.format("%s Disabling USB callback", TAG)); BuddySDK.USB.unRegisterCb(usbCallback); }


    // ====================================================================
    // Do something with the sensor values...
    // ====================================================================
    // ToDo: Should use callbacks that disable the audio tracking while it is moving and re-enables it once the Rotate call is completed.
    // For now, we are just going to do logging
    private static void processAudioData(float ambientNoise, float locationAngle) {
        if (locationAngle == -100) { return; } // -100 locationAngle is the equivalent of Null

        // Check the change in angle
        float angleChange = Math.abs(locationAngle - LocationAngle);
        Log.d(TAG, String.format("===== Ambient dB: %s, Angle: %s | dAngle: %s =====", ambientNoise, locationAngle, angleChange));

        // Perform actions as a result of this
        if ((ambientNoise > MIN_DECIBELS) & (angleChange > MIN_ANGLE)) {
            // Rotate Buddy to look at the source of the sound
            RotateBody.Rotate(10, locationAngle);

            // ToDo: Temporarily calling the Emotions code here to test if it works
            // Emotions.setPositivityEnergy((float) 0.9, (float) 0.9);
            // HeadMotors.buddyYesMove();
        }

        // Update the stored values when we are done
        AmbientNoise = ambientNoise; LocationAngle = locationAngle;
    }

}

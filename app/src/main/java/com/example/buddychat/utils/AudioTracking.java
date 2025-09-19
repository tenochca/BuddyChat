package com.example.buddychat.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;
import java.util.List;

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
/** AudioTracking
 * <br>
 * (Right now there are two methods for this, but we probably will settle on using method #2 if it
 * works well in testing.)
 * <br><br>
 * We use a queue to help smooth the sensor reading out due to its' instability. When we make a
 * call to actually rotate buddy and use the queues value, we take the average and then clear the
 * queue. We should (probably) only call the function to rotate Buddy on STT detection, because
 * with that we know that the user was just speaking for at least a short period before the
 * detection event.
 * <br><br>
 * There are four important methods: <ul>
 *   <li> setupSensors()       => Sets up the sensor module for the SDK
 *   <li> EnableUsbCallback()  => Registers a CB with the sensor module (reads sensor data)
 *   <li> DisableUsbCallback() => Unregisters the CB (no more data will be read)
 *   <li> rotateTowardsAudio() => Takes the average angle from the recent queue, rotates Buddy that direction
 * </ul>
 */
public class AudioTracking {
    private static final String TAG = "DPU_AudioTracking";

    private static final int UPDATE_INTERVAL_MS = 3_000;

    private static          Handler handler;
    private static volatile boolean enabled = false;

    // Microphone utility
    static float AmbientNoise  = 0; // Sound volume in Decibels
    static float LocationAngle = 0; // Degree location of the sound

    // Queue of recent values for the LocationAngle
    private static final AngleBuffer angleBuf = AngleBuffer.defaultAudio(/*capacity*/ 20);


    // --------------------------------------------------------------------
    // Setup Sensors
    // --------------------------------------------------------------------
    // Needs to be called after the SDK launches
    public static void setupSensors() {
        BuddySDK.USB.enableSensorModule(true, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, "-------------- Enabled Sensors --------------");  }
            @Override public void onFailed (String s) { Log.w(TAG, "Failed to Enable sensors:" + s); }
        });
    }

    // ====================================================================
    // Method #1: Looped "Runnable" call with a set update interval
    // ====================================================================
    // ToDo: This will be deleted but I'm just keeping it for now since I might want to keep it as a reference
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
        if      ( enable && !enabled) { enabled = true;  handler.post           (trackingRunnable); }
        else if (!enable &&  enabled) { enabled = false; handler.removeCallbacks(trackingRunnable); }
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
            angleBuf.push(vocalData.SoundSourceLocalisation);

            // float ambientNoise  = vocalData.AmbientSoundLevel;       // Sound level in dB
            // float locationAngle = vocalData.SoundSourceLocalisation; // Direction of the sound in degrees
            // processAudioData(ambientNoise, locationAngle);
        }
    };

    // Toggle the callback version of AudioTracking
    public static void EnableUsbCallback () { Log.d(TAG, String.format("%s Enabling USB callback",  TAG)); BuddySDK.USB.registerCb  (usbCallback); }
    public static void DisableUsbCallback() { Log.d(TAG, String.format("%s Disabling USB callback", TAG)); BuddySDK.USB.unRegisterCb(usbCallback); }


    // ====================================================================
    // Do something with the sensor values...
    // ====================================================================
    // For now, we are just going to do logging
    private static void processAudioData(float ambientNoise, float locationAngle) {
        if (locationAngle == -100) { return; } // -100 locationAngle is the equivalent of Null

        //Log.d(TAG, String.format("%s ===== Ambient dB: %s, Angle: %s =====", TAG, ambientNoise, locationAngle));
        // Update the stored values
        //AmbientNoise = ambientNoise; LocationAngle = locationAngle;
    }

    // ToDo: Should use callbacks that disable the audio tracking while it is moving and re-enables it once the Rotate call is completed.
    /** Use the queue of Buddy's most recent audioLocation readings to turn Buddy. */
    public static void rotateTowardsAudio() {
        // Get the average of the recent angles & reset the queue
        final float meanAngle = angleBuf.averageCircularAndClear(); // atomic grab+clear
        Log.i(TAG, String.format("%s averageAngle: %.2f | attempting to rotate Buddy", TAG, meanAngle));

        // Send the command to rotate
        RotateBody.Rotate(5, meanAngle);
    }

    public static float getRecentAngle() { return angleBuf.averageCircularAndClear(); }

}

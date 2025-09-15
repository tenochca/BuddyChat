package com.example.buddychat.utils;

import android.util.Log;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

// ====================================================================
// Rotate the body of the robot
// ====================================================================
public class RotateBody {
    private static final String TAG = "[DPU_RotateBody]";
    public  static boolean wheelsEnabled    = false;
    public  static boolean emergencyStopped = false;

    // --------------------------------------------------------------------
    // Rotate Buddy a set number of degrees at a set speed
    // --------------------------------------------------------------------
    // Speed  : in deg/s (>0 to go forward, <0 to go backward)
    // Degree : degree of rotation
    // ToDo: Specifics here probably need to be adjusted... like callbacks should be given for onCompletion, etc.
    public static void Rotate(float speed, float degree) {
        Log.d(TAG, String.format("%s Attempting to rotate Buddy %.3f degrees at %.3f speed", TAG, degree, speed));

        /* ToDo: Commenting this out for now so we don't actually move. Want to make sure other stuff works first.
        // Check if wheels are enabled first
        if (!wheelsEnabled) { EnableWheels(true); }
        BuddySDK.USB.rotateBuddy(speed, degree, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, "Rotate success: " + s); }
            @Override public void onFailed (String s) { Log.d(TAG, "Rotate failed:  " + s); }
        });
        EnableWheels(false); // Disable wheels when done
        */

        Log.d(TAG, String.format("%s Actual movement temporarily disabled...", TAG));
    }

    // --------------------------------------------------------------------
    // Emergency stop for the motors
    // --------------------------------------------------------------------
    /** Emergency stop for the motors (Stop motors AND disable wheels..?). */
    public static void StopMotors() {
        BuddySDK.USB.emergencyStopMotors(new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, String.format("%s StopMotors success: %s", TAG, s)); }
            @Override public void onFailed (String s) { Log.w(TAG, String.format("%s StopMotors failed: %s",  TAG, s)); }
        });
        EnableWheels(false);
    }

    // --------------------------------------------------------------------
    // Toggle the wheel motors (enable or disable both happen here)
    // --------------------------------------------------------------------
    public static void EnableWheels(boolean enable) {
        Log.d(TAG, String.format("%s Toggling wheels %s", TAG, (enable ? "on" : "off")));
        if (emergencyStopped) { return; } // ToDo: Controlled in MainApplication; might need changing...

        BuddySDK.USB.enableWheels(enable, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, String.format("%s EnableWheels success: %s", TAG, s)); wheelsEnabled = enable; }
            @Override public void onFailed (String s) { Log.w(TAG, String.format("%s EnableWheels failed: %s",  TAG, s)); }
        });
    }

    // No instances
    private RotateBody() {}
}

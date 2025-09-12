package com.example.buddychat.utils;

import android.util.Log;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

// ====================================================================
// Rotate the body of the robot
// ====================================================================
public class RotateBody {
    private static final String  TAG           = "DPU_RotateBody";
    public  static       boolean wheelsEnabled = false;

    // --------------------------------------------------------------------
    // Rotate Buddy a set number of degrees at a set speed
    // --------------------------------------------------------------------
    // Speed  : in deg/s (>0 to go forward, <0 to go backward)
    // Degree : degree of rotation
    // ToDO: Specifics here probably need to be adjusted... like callbacks should be given etc.
    /** Rotate Buddy a set number of degrees at a set speed */
    public static void Rotate(float speed, float degree) {
        Log.d(TAG, String.format("Attempting to rotate Buddy %.3f degrees at %.3f speed", degree, speed));

        // Check if wheels are enabled
        if (!wheelsEnabled) { EnableWheels(1, 1); }

        BuddySDK.USB.rotateBuddy(speed, degree, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, "Rotate success: " + s); }
            @Override public void onFailed (String s) { Log.d(TAG, "Rotate failed:  " + s); }
        });

        // Disable wheels when done
        EnableWheels(0, 0);
    }

    // --------------------------------------------------------------------
    // Emergency stop for the motors
    // --------------------------------------------------------------------
    /** Emergency stop for the motors */
    public static void StopMotors() {
        BuddySDK.USB.emergencyStopMotors(new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, "StopMotors success: " + s); }
            @Override public void onFailed (String s) { Log.d(TAG, "StopMotors failed: "  + s); }
        });

        // Stop motors AND disable wheels..?
        EnableWheels(0, 0);
    }

    // --------------------------------------------------------------------
    // Toggle the wheel motors (enable or disable both happen here)
    // --------------------------------------------------------------------
    // turnOnLeftWheel  : setting the Left  wheel motor on On of "int" type (On=1) (Off=0)
    // turnOnRightWheel : setting the right wheel motor on On of "int" type (On=1) (Off=0)
    /** Toggle wheel motors of the robot */
    public static void EnableWheels(int turnOnLeftWheel, int turnOnRightWheel) {
        Log.d(TAG, String.format("Toggling wheels -- left: %d, right: %d", turnOnLeftWheel, turnOnRightWheel));

        boolean enabling = (turnOnLeftWheel == 1) & (turnOnRightWheel == 1);

        // ToDo: Not sure why it says the method is deprecated... SDK documentation shows it...
        BuddySDK.USB.enableWheels(turnOnLeftWheel, turnOnRightWheel, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, "EnableWheels success: " + s); wheelsEnabled = enabling; }
            @Override public void onFailed (String s) { Log.d(TAG, "EnableWheels failed: "  + s); }
        });
    }


    // No instances
    private RotateBody() {}
}

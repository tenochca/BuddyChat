package com.example.buddychat.utils;

import android.util.Log;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

// ====================================================================
// Move the head in "Yes" or "No" motions
// ====================================================================
// Page 33 of the SDK guide
public class HeadMotors {
    private static final String TAG = "[DPU_HeadMotors]";
    private static volatile boolean running          = false;
    public  static          boolean emergencyStopped = false;

    // --------------------------------------------------------------------
    // Toggle the motors on and off
    // --------------------------------------------------------------------
    // ToDo: Check the status of the motor first?
    public static void toggleYesMotor(boolean iEnable) {
        //String yesStatus = BuddySDK.Actuators.getYesStatus();
        BuddySDK.USB.enableYesMove(iEnable, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, String.format("%s ToggleYesMotor (%s) success: %s", TAG, iEnable, s)); }
            @Override public void onFailed (String s) { Log.i(TAG, String.format("%s ToggleYesMotor (%s) failure: %s", TAG, iEnable, s)); }
        });
    }
    public static void toggleNoMotor(boolean iEnable) {
        BuddySDK.USB.enableNoMove(iEnable, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, String.format("%s ToggleNoMotor (%s) success: %s", TAG, iEnable, s)); }
            @Override public void onFailed (String s) { Log.i(TAG, String.format("%s ToggleNoMotor (%s) failure: %s", TAG, iEnable, s)); }
        });
    }

    /** Toggle the head (yes/no) motors on or off. */
    public static void toggleMotors(boolean iEnable) { toggleYesMotor(iEnable); toggleNoMotor(iEnable); }

    // Just check if it is on yet, if not, activate it
    public static void activateYesMotor() {
        String yesStatus = BuddySDK.Actuators.getYesStatus().toUpperCase();
        if (yesStatus.contains("DISABLE")) { toggleYesMotor(true); }
    }

    // Check if the specified motor is ready (check page 32 of the SDK guide)
    // "DISABLE => Motor is disabled
    // "STOP"   => Motor is enabled
    // "SET"    => Motor is moving
    // "NONE"   => Default (what does that mean? maybe if the board isn't responding?)
    private static boolean motorEnabled(String type) {
        if      (type.equals("YES")) { return BuddySDK.Actuators.getYesStatus().toUpperCase().contains("STOP"); }
        else if (type.equals("NO" )) { return BuddySDK.Actuators.getNoStatus ().toUpperCase().contains("STOP"); }
        else { return false; }
    }

    // --------------------------------------------------------------------
    // Perform the yes/no head movements
    // --------------------------------------------------------------------
    // ToDo: Why can the speed by positive or negative?
    public static void buddyYesMove() {
        if (!running) { activateYesMotor(); running = true; buddyYesMove(10, -20, 2); }
        else          { Log.w(TAG, String.format("%s BuddyYesMove() call failed -- already running.", TAG)); }
    }

    // Angle range is -35 and 45; Speed range is -49.2 to 49.2 but we should keep it low.
    private static void buddyYesMove(float speed, float angle, int remaining) {
        if (!motorEnabled("YES") | emergencyStopped) { return; } // Motor isn't ready or we are emergency stopped

        // 3 parts: down (-20), up (+20), back to home (0)
        BuddySDK.USB.buddySayYes(speed, angle, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String success) {
                // When it succeeds, we should call it another time
                if (success.equals("YES_MOVE_FINISHED")) {
                    Log.d(TAG, String.format("%s YesMove %d/3 finished -- angle: %f", TAG, (3-remaining), angle));
                    if      (remaining == 2) { buddyYesMove(speed, -angle, 1); }
                    else if (remaining == 1) { buddyYesMove(speed, 0, 0); }
                    else                     { running = false; toggleYesMotor(false); }
                }
            }
            @Override public void onFailed(String s) { running = false; toggleYesMotor(false); Log.w(TAG, String.format("%s YesMove failure: %s", TAG, s)); }
        });
    }

    // ToDo: BuddyNoMove



}

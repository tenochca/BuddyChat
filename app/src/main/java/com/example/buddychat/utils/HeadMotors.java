package com.example.buddychat.utils;

import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

// =======================================================================
// Move the head in "Yes" or "No" motions
// =======================================================================
// Page 33 of the SDK guide
// ToDo: Why can the speed be positive or negative? is that why it isn't working?
// ToDo: BuddyNoMove
// ToDo: The style of the old and new versions are still kind of clashing and need to be cleaned up
public class HeadMotors {
    private static final String TAG = "[DPU_HeadMotors]";

    // Motion parameters (tune as needed)
    private static final float  SPEED         =   5f;   // 0...~49, keep low
    private static final float  DOWN_ANGLE    = -10f;   // degrees
    private static final float  UP_ANGLE      =  10f;   // degrees
    private static final float  HOME_ANGLE    =   0f;   // degrees
    private static final long   SETTLE_MS     =  60L;   // small pause between steps
    private static final int    MAX_RETRIES   =   2;

    // Simple state machine for a nod
    private enum Step { DOWN, UP, HOME, DONE }

    // Dedicated motor thread (serialize commands, keep off UI)
    private static final HandlerThread motorThread = new HandlerThread("MotorBus");
    private static final Handler motor;
    static { motorThread.start(); motor = new Handler(motorThread.getLooper()); }

    // Status checks
    private static volatile boolean running          = false;
    public  static          boolean emergencyStopped = false;

    // =======================================================================
    // Public Motor Utilities
    // =======================================================================
    // Get motor positions (was yPos: -10.0, nPos: -4.0 when I started)
    public static void getHeadMotorStatus() {
        final String yLog = String.format("YES Status %s, Pos:  %s", BuddySDK.Actuators.getYesStatus(), BuddySDK.Actuators.getYesPosition());
        final String nLog = String.format( "NO Status %s, Pos:  %s", BuddySDK.Actuators.getNoStatus (), BuddySDK.Actuators.getNoPosition ());
        Log.d(TAG, String.format("%s %s | %s", TAG, yLog, nLog));
    }

    // -----------------------------------------------------------------------
    // Toggle the motors on and off
    // -----------------------------------------------------------------------
    /** Toggle the head motors (Yes/No) off. */
    public static void StopMotors() { toggleYesMotor(false); toggleNoMotor(false); }

    // ToDo: Right now we are going to say that it is probably fine to "double toggle" a motor on...
    // ToDo: Combine these two with the function at the bottom? Just for more informative logs
    public static void toggleYesMotor(boolean iEnable) {
        final String ogS = BuddySDK.Actuators.getYesStatus();
        BuddySDK.USB.enableYesMove(iEnable, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, String.format("%s ToggleYesMotor [%s] success: %s (was: %s)", TAG, (iEnable ? "ON" : "OFF"), s, ogS)); }
            @Override public void onFailed (String s) { Log.d(TAG, String.format("%s ToggleYesMotor [%s] failure: %s (was: %s)", TAG, (iEnable ? "ON" : "OFF"), s, ogS)); }
        });
    }

    public static void toggleNoMotor(boolean iEnable) {
        final String ogS = BuddySDK.Actuators.getNoStatus();
        BuddySDK.USB.enableNoMove(iEnable, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.d(TAG, String.format("%s ToggleNoMotor [%s] success: %s (was: %s)", TAG, (iEnable ? "ON" : "OFF"), s, ogS)); }
            @Override public void onFailed (String s) { Log.d(TAG, String.format("%s ToggleNoMotor [%s] failure: %s (was: %s)", TAG, (iEnable ? "ON" : "OFF"), s, ogS)); }
        });
    }

    // -----------------------------------------------------------------------
    // Check if the specified motor is ready (check page 32 of the SDK guide)
    // -----------------------------------------------------------------------
    // ToDo: Needs to also check for "SET" -- I think this might have been my issue
    // "DISABLE => Motor is disabled
    // "STOP"   => Motor is enabled
    // "SET"    => Motor is moving
    // "NONE"   => Default (what does that mean? maybe if the board isn't responding?)
    private static boolean motorEnabled(String type) {
        if      (type.equals("YES")) { return BuddySDK.Actuators.getYesStatus().toUpperCase().contains("STOP"); }
        else if (type.equals("NO" )) { return BuddySDK.Actuators.getNoStatus ().toUpperCase().contains("STOP"); }
        else { return false; }
    }



    // =======================================================================
    // Public Entry to Motor Control
    // =======================================================================
    /** Public entry: perform a YES nod (down → up → home). No-ops if already running. */
    public static void nodYes() {
        if (running || emergencyStopped) { Log.w(TAG, String.format("%s nodYes() ignored (running=%s, eStop=%s)", TAG, running, emergencyStopped)); }
        else                             { running = true; motor.post(HeadMotors::enableAndStartYesSequence);                                       }
    }

    // ToDo: IDK should integrate it with the rest of the code here better
    public static void resetYesPosition() {
        // Check if motor is activated, if so, try to move it to   BuddySDK.Actuators.getYesPosition())
        final String yesStatus   = BuddySDK.Actuators.getYesStatus();
        final float  yesPosition = BuddySDK.Actuators.getYesPosition();
        Log.d(TAG, String.format("%s resetYesPosition() called (running=%s, eStop=%s, status=%s, pos=%s)", TAG, running, emergencyStopped, yesStatus, yesPosition));

        if (running || emergencyStopped || (yesPosition == 0)) { Log.w(TAG, String.format("%s resetYesPosition() ignored", TAG)); }
        else                                                   { running = true; motor.post(HeadMotors::resetYes);                }
    }
    private static void resetYes() { toggleYesMotor(true); startYesStep(Step.HOME, 0); }


    // -----------------------------------------------------------------------
    // Internal Methods
    // -----------------------------------------------------------------------
    private static void enableAndStartYesSequence() {
        if (emergencyStopped) { running = false; return; }
        final String before = BuddySDK.Actuators.getYesStatus();

        // Only start the chained sequence after the controller reports enabled
        BuddySDK.USB.enableYesMove(true, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) {
                Log.d(TAG, String.format("%s enableYesMove ON success (before=%s, after=%s)", TAG, before, BuddySDK.Actuators.getYesStatus()));
                startYesStep(Step.DOWN, 0);
            }
            @Override public void onFailed(String s) { Log.w(TAG, "enableYesMove ON failed: " + s); running = false; }
        });
    }

    private static void startYesStep(Step step, int retries) {
        if (emergencyStopped) { running = false; return; }
        switch (step) {
            case DOWN : issueYesMove(SPEED, DOWN_ANGLE, step, retries); break;
            case UP   : issueYesMove(SPEED,   UP_ANGLE, step, retries); break;
            case HOME : issueYesMove(SPEED, HOME_ANGLE, step, retries); break;
            case DONE : running = false;
                Log.d(TAG, String.format("%s YES nod sequence complete.", TAG));
                disableYesMove(false); // Disable the motor after to save power
                break;
        }
    }

    /** Single command -> waits for its own onSuccess, then schedules the next step. */
    private static void issueYesMove(float speed, float angle, Step step, int retries) {
        BuddySDK.USB.buddySayYes(speed, angle, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String success) { Log.i(TAG, String.format("%s IDK yes response: %s", TAG, success));
                // When it succeeds, we should give the controller moment to settle, then move on to next step
                if (success.equals("YES_MOVE_FINISHED")) {
                    Log.i(TAG, String.format("%s buddySayYes Success (step: %s, angle: %.1f)", TAG, step, angle));
                    motor.postDelayed(() -> startYesStep(next(step), 0), SETTLE_MS);
                }
            }
            @Override public void onFailed(String err) { Log.w(TAG, String.format("%s buddySayYes failed (step: %s, angle: %.1f): %s", TAG, step, angle, err));
                // Retry this step once/twice before aborting
                if (retries < MAX_RETRIES && !emergencyStopped) { motor.postDelayed(() -> startYesStep(step, retries + 1), SETTLE_MS); }
                else                                            { running = false; disableYesMove(true);                             }
            }
        });
    }

    // Disable the Yes motor after a success or failure
    private static void disableYesMove(boolean onFailure) {
        final String caller = onFailure ? "failure" : "success";
        BuddySDK.USB.enableYesMove(false, new IUsbCommadRsp.Stub() {
            @Override public void onSuccess(String s) { Log.i(TAG, String.format("%s YES motor disabled after %s",             TAG, caller   )); }
            @Override public void onFailed (String s) { Log.w(TAG, String.format("%s YES motor disable (after %s) failed: %s", TAG, caller, s)); }
        });
    }

    // Get the next step to do
    private static Step next(Step s) {
        switch (s) {
            case DOWN: return Step.UP;
            case UP  : return Step.HOME;
            default  : return Step.DONE;
        }
    }






}

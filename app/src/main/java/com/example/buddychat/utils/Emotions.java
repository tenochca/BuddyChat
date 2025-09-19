package com.example.buddychat.utils;

import android.os.RemoteException;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bfr.buddy.ui.shared.IUIFaceAnimationCallback;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.ui.shared.FacialExpression;

// ====================================================================
// Facial Emotions
// ====================================================================
public class Emotions {
    private static final String TAG = "[DPU_Emotions]";

    private static final Handler  MAIN = new Handler(Looper.getMainLooper());
    private static       Runnable pendingReset;   // to cancel if a new mood comes in
    private static       int      version = 0;

    // --------------------------------------------------------------------
    // Valence & Arousal
    // --------------------------------------------------------------------
    // ToDo: Not sure if I have to set Buddy's face to NEUTRAL before calling these methods...
    /** Set the Positivity & Energy levels of Buddy's face (only works if face is NEUTRAL). */
    public static void setPositivityEnergy(float iPositivity, float iEnergy) {
        Log.d(TAG, String.format("%s Positivity: %.3f, Energy: %.3f", TAG, iPositivity, iEnergy));
        BuddySDK.UI.setFacePositivity(iPositivity);
        BuddySDK.UI.setFaceEnergy    (iEnergy    );
    }

    // --------------------------------------------------------------------
    // Mood-related methods
    // --------------------------------------------------------------------
    /** Set the Mood directly (changes Buddy's facial expression & LED colors). Overloaded. */
    public static void setMood(FacialExpression iExpression) { BuddySDK.UI.setMood(iExpression); }

    /** Overloaded method to allow calls with raw strings for the desired mood. */
    public static void setMood(String moodString) { setMood(parseExpression(moodString)); }

    // --------------------------------------------------------------------
    // Self-resetting mood (i.e. set to angry -> back to NEUTRAL after 2s)
    // --------------------------------------------------------------------
    public static void setMood(String moodString, long resetMS) { setMood(parseExpression(moodString), resetMS); }

    /** Overload method for resetting the mood to 'NEUTRAL' after a delay. */
    public static void setMood(FacialExpression iExpression, long resetMs) {
        // No need to do the callback if we are already setting it to neutral.
        if (iExpression == FacialExpression.NEUTRAL ) { setMood(iExpression); return; }

        // Set mood with a reset callback
        final int myVersion = ++version;
        BuddySDK.UI.setMood(iExpression, new IUIFaceAnimationCallback.Stub() {
            @Override public void onAnimationEnd(String s, String s1) throws RemoteException {
                Log.d(TAG, String.format("%s Face animation end: %s (%s)", TAG, s, s1)); if (myVersion != version) return;  // stale
                pendingReset = () -> { if (myVersion == version) { BuddySDK.UI.setMood(FacialExpression.NEUTRAL); }};
                MAIN.postDelayed(pendingReset, resetMs);
            }
        });
    }

    // --------------------------------------------------------------------
    // Facial Expression Helpers
    // --------------------------------------------------------------------
    /** Get the Mood object from the input string (fallback to 'NEUTRAL' expression). */
    private static FacialExpression parseExpression(String rawExpression) {
        // Log and invalidate current running callbacks
        Log.d(TAG, String.format("%s Setting expression to: %s", TAG, rawExpression));
        version++; cancelPending();

        // Get the actual mood object
        String key = rawExpression.trim().toUpperCase();
        try                                { return FacialExpression.valueOf(key); }
        catch (IllegalArgumentException e) { return FacialExpression.NEUTRAL;      }
    }

    /** Cancel pending callbacks to reset the facial animation. */
    private static void cancelPending() {
        if (pendingReset != null) { MAIN.removeCallbacks(pendingReset); pendingReset = null; }
    }

}

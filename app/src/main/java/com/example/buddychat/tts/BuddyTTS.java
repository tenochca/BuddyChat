package com.example.buddychat.tts;

import android.content.Context;
import android.util.Log;

import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddysdk.BuddySDK;


// ====================================================================
// Wrapper class around BuddySDK.Speech for Text-to-Speech
// ====================================================================
public class BuddyTTS {

    private static final String  TAG     = "BuddyTTS";
    private static       boolean loaded  = false;
    private static       boolean enabled = false;

    private BuddyTTS() {}  // static-only class

    // --------------------------------------------------------------------
    // Initialization & Utility
    // --------------------------------------------------------------------
    /** Called once in MainActivity.onCreate */
    public static void init(Context ctx) {
        if (loaded) return;
        BuddySDK.Speech.loadReadSpeaker();          // async, but cheap to call again
        BuddySDK.Speech.setSpeakerVoice("kate");    // English voice
        loaded = true;
        Log.d(TAG, "ReadSpeaker loaded");
    }

    /** Stop current utterance if there is one */
    public static void toggle() {
        enabled = !enabled;
        String logMessage = enabled ? "TTS Enabled." : "TTS Disabled";
        Log.w(TAG, logMessage);
    }

    /** Check if the class is present (it won't be on simulators, only on the BuddyRobot itself) */
    public static boolean isAvailable() {
        // Need to wrap it in try-except to avoid crashing locally
        // If the SDK class itself is missing, or the service isn't bound yet, return false
        try                         { return BuddySDK.Speech != null && BuddySDK.Speech.isReadyToSpeak();          }
        catch (RuntimeException ex) { Log.w(TAG, "Buddy speech not ready: " + ex.getMessage()); return false; }
    }

    // --------------------------------------------------------------------
    // Text-to-Speech Usage
    // --------------------------------------------------------------------
    /** Fire-and-forget speech */
    public static void speak(String text) {
        // Ready checks
        if (!isAvailable()) { Log.w(TAG, "TTS not ready. Call BuddyTTS.init() first."); return; }
        if (!enabled      ) { Log.w(TAG, "TTS not enabled."                          ); return; }

        // Use BuddySDK Speech
        BuddySDK.Speech.startSpeaking(
                text,
                new ITTSCallback.Stub() {
                    @Override public void onSuccess(String s) { Log.d(TAG, s); }
                    @Override public void onPause  ()         {                }
                    @Override public void onResume ()         {                }
                    @Override public void onError  (String s) { Log.e(TAG, s); }
                });
    }

    /** Stop current utterance if there is one */
    public static void stop() {
        if (!isAvailable()) { Log.w(TAG, "'stop()' call failed -- TTS not ready."); return; }
        BuddySDK.Speech.stopSpeaking();
    }

    /** Settings for the speech, not using right now */
    public static void setPitch (int pitch ) { BuddySDK.Speech.setSpeakerPitch (pitch ); }
    public static void setSpeed (int speed ) { BuddySDK.Speech.setSpeakerSpeed (speed ); }
    public static void setVolume(int volume) { BuddySDK.Speech.setSpeakerVolume(volume); }

}

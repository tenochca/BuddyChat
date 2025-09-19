package com.example.buddychat.stt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.STTResult;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;

import java.util.Locale;

// =======================================================================
// Wrapper class around BuddySDK.Speech for Speech-to-Text
// =======================================================================
public class BuddySTT {
    // Public choices
    public enum Engine { GOOGLE, CERENCE_FREE, CERENCE_FCF }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------
    private static final String   TAG       = "[BuddySTT]";
    private static final int      REQ_PERM  = 9001;
    private static final String[] MIC_PERMS = { Manifest.permission.RECORD_AUDIO };

    private static boolean available;
    private static STTTask task;
    private static boolean continuous;

    private BuddySTT() {} // Static-only class

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------
    /** Called once in MainActivity.onCreate */
    public static void init(Context context, Locale locale, Engine engine, boolean listenContinuous) {
        // Check microphone permission
        if (notMicPermission(context)) { requestMicPermission(context); }
        continuous = listenContinuous;

        // Guard for BuddyRobot hardware
        try {
            // We can delete the French optional
            switch (engine) {
                case GOOGLE       : task = BuddySDK.Speech.createGoogleSTTTask(locale); break;
                case CERENCE_FREE : task = BuddySDK.Speech.createCerenceFreeSpeechTask(locale); break;
                case CERENCE_FCF  :
                    String fcf = locale == Locale.ENGLISH ? "audio_en.fcf" : "audio_fr.fcf";
                    task = BuddySDK.Speech.createCerenceTaskFromAssets(locale, fcf, context.getAssets());
                    break;
            }

            // Success, finish initializing
            task.initialize(); available = true;
            Log.i(TAG, String.format("%s Buddy STT initialised with: %s", TAG, engine));
        }

        // Not on a Buddy robot / some other failure
        catch (Throwable t) { available = false; Log.w(TAG, String.format("%s Buddy STT unavailable: %s", TAG, t)); }
    }

    // -----------------------------------------------------------------------
    // Speech-to-Text Usage
    // -----------------------------------------------------------------------
    /** Start listening (no-op on devices without Buddy speech service). */
    public static void start(@NonNull STTListener cb) {
        if (!available) { Log.d(TAG, "STT ignored (not available)"); return; }

        // Start listening
        Log.w(TAG, "STT started");

        task.start(continuous, new ISTTCallback.Stub() {
            @Override public void onSuccess(STTResultsData res) {
                if (!res.getResults().isEmpty()) {
                    STTResult r = res.getResults().get(0);
                    cb.onText(r.getUtterance(), r.getConfidence(), r.getRule());
                }
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    /** Stop helper (currently never used) */
    public static void stop() { if (available) task.stop(); }

    /** Start/Pause Wrapper */
    public static void toggle (@NonNull STTListener cb) {
        if (task.isRunning()) { task.pause(); return; }
        start(cb);
    }

    // -----------------------------------------------------------------------
    // Permission Helpers -- used once during initialization
    // -----------------------------------------------------------------------
    private static boolean notMicPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
    }

    private static void requestMicPermission(Context context) {
        Log.i(TAG, String.format("%s Requesting microphone permission...", TAG));
        if (!(context instanceof Activity)) return;  // caller must be an Activity
        ActivityCompat.requestPermissions((Activity) context, MIC_PERMS, REQ_PERM);
    }

}

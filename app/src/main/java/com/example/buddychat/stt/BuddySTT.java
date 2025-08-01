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

// ====================================================================
// Wrapper class around BuddySDK.Speech for Speech-to-Text
// ====================================================================
public class BuddySTT {
    // Public choices
    public enum Engine { GOOGLE, CERENCE_FREE, CERENCE_FCF }

    // --------------------------------------------------------------------
    // Internal
    // --------------------------------------------------------------------
    private static final String   TAG       = "BuddySTT";
    private static final int      REQ_PERM  = 9001;
    private static final String[] MIC_PERMS = { Manifest.permission.RECORD_AUDIO };

    private static boolean available;
    private static STTTask task;
    private static boolean continuous;
    private static Context ctx; // ---- Memory leak... Idk how to fix ----

    private BuddySTT() {}

    // --------------------------------------------------------------------
    // Initialization
    // --------------------------------------------------------------------
    /** Called once in MainActivity.onCreate */
    public static void init(Context context, Locale locale, Engine engine, boolean listenContinuous) {
        ctx = context.getApplicationContext();
        continuous = listenContinuous;

        // Guard for BuddyRobot hardware
        try {
            // We can delete the French optional
            switch (engine) {
                case GOOGLE       : task = BuddySDK.Speech.createGoogleSTTTask(locale); break;
                case CERENCE_FREE : task = BuddySDK.Speech.createCerenceFreeSpeechTask(locale); break;
                case CERENCE_FCF  :
                    String fcf = locale == Locale.ENGLISH ? "audio_en.fcf" : "audio_fr.fcf";
                    task = BuddySDK.Speech.createCerenceTaskFromAssets(locale, fcf, ctx.getAssets());
                    break;
            }

            // Success, finish initializing
            task.initialize();
            available = true;
            Log.d(TAG, "Buddy STT initialised with " + engine);
        }

        // Not on a Buddy robot / some other failure
        catch (Throwable t) { available = false; Log.w(TAG, "Buddy STT unavailable: " + t); }
    }

    // --------------------------------------------------------------------
    // Speech-to-Text Usage
    // --------------------------------------------------------------------
    /** Start listening (no-op on devices without Buddy speech service). */
    public static void start(@NonNull STTListener cb) {
        if (!available) { Log.d(TAG, "STT ignored (not available)"); return; }

        if (!hasMicPermission()) requestMicPermission();
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

    /** Pause / resume helpers. */
    public static void pause() { if (available) task.pause(); }
    public static void stop () { if (available) task.stop (); }

    // --------------------------------------------------------------------
    // Permission Helpers
    // --------------------------------------------------------------------
    private static boolean hasMicPermission() {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private static void requestMicPermission() {
        if (!(ctx instanceof Activity)) return;  // caller must be an Activity
        ActivityCompat.requestPermissions((Activity) ctx, MIC_PERMS, REQ_PERM);
    }

}




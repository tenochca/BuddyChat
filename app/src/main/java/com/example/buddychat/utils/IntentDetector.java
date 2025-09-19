package com.example.buddychat.utils;

import java.util.regex.Pattern;

import android.util.Log;

// =======================================================================
// Check an utterance for "intent" (yes/no)
// =======================================================================
// Allows us to trigger behaviors in Buddy depending on the results
public class IntentDetector {
    private static final String TAG = "[DPU_IntentDetector]";

    // Types of results possible
    public enum Intent { AFFIRM, NEGATE, UNKNOWN }

    // Define "intent" phrases (use \s+ inside multi-word phrases)
    private static final String AFFIRM_SRC = "yes|y(?:ep|ea?h)|sure|of\\s+course|absolutely|affirmative|correct|indeed|right";
    private static final String NEGATE_SRC = "no|nope|nah|negative|never|incorrect|wrong";

    // Allow leading spaces, then a match, then any punctuation/spaces
    private static final Pattern AFFIRM = Pattern.compile("^\\s*" + AFFIRM_SRC + "\\b[\\p{Punct}\\s]*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NEGATE = Pattern.compile("^\\s*" + NEGATE_SRC + "\\b[\\p{Punct}\\s]*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Check an utterances intent
    public static Intent classify(String s) {
        if (s == null) return Intent.UNKNOWN;
        if (AFFIRM.matcher(s).find()) return Intent.AFFIRM;
        if (NEGATE.matcher(s).find()) return Intent.NEGATE;
        return Intent.UNKNOWN;
    }

    // -----------------------------------------------------------------------
    // Specific Yes/No Helpers (so callers don't need the Intent enum)
    // -----------------------------------------------------------------------
    public static boolean isYes(String s) {
        if (s == null) { return false;                      }
        else           { return (AFFIRM.matcher(s).find()); }
    }
    public static boolean isNo(String s) {
        if (s == null) { return false;                      }
        else           { return (NEGATE.matcher(s).find()); }
    }

    // =======================================================================
    // Buddy-specific behavior controls
    // =======================================================================
    /** Buddy-specific behavior controls
     * <br>
     * Two modes: <ol>
     *     <li> Tell the robot to nod its head yes or no. </li>
     *     <li> Change the valence and arousal (assuming the face is set to "NEUTRAL"). </li>
     * </ol>
     * We will start off with the second mode until the nodding is functional.
     */
    public static void IntentDetection(String s) {
        final Intent intent = classify(s);
        Log.d(TAG, String.format("%s Detected intent: %s", TAG, intent));

        // Mode #2: Change the valence (positivity) & arousal (energy)
        if      (intent == Intent.AFFIRM) { Emotions.setPositivityEnergy(0.9F, 0.9F); }
        else if (intent == Intent.NEGATE) { Emotions.setPositivityEnergy(0.1F, 0.1F); }
        else                              { Emotions.setPositivityEnergy(0.5F, 0.5F); }
    }

}

package com.example.buddychat.utils;

import android.util.Log;

import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.ui.shared.FacialExpression;

// ====================================================================
// Facial Emotions
// ====================================================================
// ToDo: Figure out an actual way to handle the setMood results. Probably map moods to integer values?
public class Emotions {
    private static final String TAG = "DPU_Emotions";

    /** Set the Positivity & Energy levels of Buddy's face */
    public static void setPositivityEnergy(float iPositivity, float iEnergy) {
        Log.d(TAG, String.format("[Emotions] Positivity: %.3f, Energy: %.3f", iPositivity, iEnergy));
        BuddySDK.UI.setFacePositivity(iPositivity);
        BuddySDK.UI.setFaceEnergy    (iEnergy    );
    }

    /** Set the Mood directly (changes Buddy's facial expression & LED colors). */
    public static void setMood(boolean test) {
        if (test) { BuddySDK.UI.setMood(FacialExpression.GRUMPY  ); }
        else      { BuddySDK.UI.setMood(FacialExpression.NEUTRAL); }
    }

}

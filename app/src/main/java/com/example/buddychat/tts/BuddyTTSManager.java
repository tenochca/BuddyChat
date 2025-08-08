package com.example.buddychat.tts;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.speech.shared.ITTSCallback;

import java.util.Locale;

public class BuddyTTSManager {
    private static final String TAG = "BuddyTTSManager";
    private Context appContext;

    public BuddyTTSManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void initializeTTS() {
        Log.i(TAG, "Initializing TTS...");
        BuddySDK.Speech.loadReadSpeaker();
        BuddySDK.Speech.setSpeakerVoice("kate"); // English American voice
    }

    public void speak(String textToSpeak, Locale locale) {
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) {
            Log.w(TAG, "speak called with empty or null text.");
            return;
        }

        if (BuddySDK.Speech.isReadyToSpeak()) {
            Log.i(TAG, "TTS is ready. Attempting to speak: " + textToSpeak);

            BuddySDK.Speech.startSpeaking(textToSpeak, new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String messageId) {
                    Log.i(TAG, "TTS onSuccess: " + messageId + " for text: " + textToSpeak.substring(0, Math.min(textToSpeak.length(), 30)));
                }

                @Override
                public void onPause() {
                    Log.i(TAG, "TTS onPause");
                }

                @Override
                public void onResume() {
                    Log.i(TAG, "TTS onResume");
                }

                @Override
                public void onError(String errorDetails) {
                    Log.e(TAG, "TTS onError: " + errorDetails);
                    if (appContext != null) {
                        Toast.makeText(appContext, "TTS Error: " + errorDetails, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Log.w(TAG, "TTS not ready to speak. Attempting to load ReadSpeaker again.");
            if (appContext != null) {
                Toast.makeText(appContext, "TTS Not initialized or still loading. Please wait.", Toast.LENGTH_SHORT).show();
            }
            BuddySDK.Speech.loadReadSpeaker(); // Attempt to re-load
        }
    }
}

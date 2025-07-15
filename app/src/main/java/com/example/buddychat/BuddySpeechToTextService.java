package com.example.buddychat;
import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.STTResult;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import com.example.buddychat.SpeechToTextService;

import android.content.res.AssetManager; // Required for Cerence local FCF
import android.os.RemoteException;
import android.util.Log;

import java.util.Locale;

public class BuddySpeechToTextService implements SpeechToTextService {


    @Override
    public void prepareSTTEngine(SttEngineType engineType, Locale locale) {

    }

    @Override
    public void initializeListening(SttListener listener) {

    }

    @Override
    public void startListening(boolean listenContinuously) {

    }

    @Override
    public void pauseListening() {

    }

    @Override
    public void stopListening() {

    }

    @Override
    public void releaseService() {

    }
}

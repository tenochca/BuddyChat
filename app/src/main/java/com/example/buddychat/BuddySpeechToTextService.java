package com.example.buddychat;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.STTResult;
import com.bfr.buddy.speech.shared.STTResultsData;

import android.content.res.AssetManager; // Required for Cerence local FCF
import android.os.RemoteException;
import android.util.Log;

import java.util.Locale;

public class BuddySpeechToTextService implements SpeechToTextService {

    private static final String TAG = "BuddySttService";

    private SttListener appSttListener;
    private STTTask currentSttTask;
    private AssetManager assetManager; //for Cerence local FCF
    private boolean isInitialized = false;
    private boolean isListening = false;

    public BuddySpeechToTextService(AssetManager assets) {
        this.assetManager = assets;
    }

    @Override
    public void prepareSTTEngine(SttEngineType engineType, Locale locale) {
        if (currentSttTask != null) {
            Log.w(TAG, "An STT task already exists. Stopping the old one before creating a new one.");
            stopListening(); // Stop and release the previous task
        }

        Log.i(TAG, "Preparing STT engine: " + engineType + " for locale: " + locale.toString());
        try {
            switch (engineType) {
                case GOOGLE:
                    currentSttTask = BuddySDK.Speech.createGoogleSTTTask(locale);
                    break;
                case CERENCE_FREE_SPEECH:
                    currentSttTask = BuddySDK.Speech.createCerenceFreeSpeechTask(locale);
                    break;
                case CERENCE_LOCAL_FCF:
                    String fcfFilename;
                    // Example logic for fcf filename from MainActivity
                    if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) { // More robust locale check
                        fcfFilename = "audio_en.fcf"; // Ensure this file exists in your assets
                    } else if (locale.getLanguage().equals(Locale.FRENCH.getLanguage())) {
                        fcfFilename = "audio_fr.fcf"; // Ensure this file exists in your assets
                    } else {
                        Log.e(TAG, "Cerence local FCF not supported for locale: " + locale);
                        if (appSttListener != null) {
                            appSttListener.onError("Cerence local FCF not supported for locale: " + locale);
                        }
                        currentSttTask = null; // Ensure task is null
                        return;
                    }
                    Log.d(TAG, "Using Cerence local FCF file: " + fcfFilename);
                    currentSttTask = BuddySDK.Speech.createCerenceTaskFromAssets(locale, fcfFilename, this.assetManager);
                    break;
                default:
                    Log.e(TAG, engineType + " is not a supported STT engine type.");
                    currentSttTask = null; // Ensure task is null
                    if (appSttListener != null) {
                        appSttListener.onError(engineType + " is not a supported STT engine type.");
                    }
                    return;
            }
            isInitialized = false;
            isListening = false;
            Log.i(TAG, "STT task created for " + engineType);
        } catch (Exception e) {
            Log.e(TAG, "Error creating STT task" + e.getMessage(), e);
            currentSttTask = null; // Ensure task is null
            if (appSttListener != null) {
                appSttListener.onError("Error creating STT task: " + e.getMessage());
            }
        }
    }

    @Override
    public void initializeListening(SttListener listener) {
        this.appSttListener = listener;
        if (currentSttTask == null) {
            Log.e(TAG, "STT task is null. Call prepareSTTEngine first.");

            if (this.appSttListener != null) {
                this.appSttListener.onError("STT engine not prepared. ");
            }
            return;
        }
        if (isInitialized) {
            Log.w(TAG, "STT task already initialized.");
            if (this.appSttListener != null) {
                this.appSttListener.onSttReady();
            }
            return;
        }
        Log.d(TAG, "Initializing STT task");
        try {
            currentSttTask.initialize();
            isInitialized = true;
            Log.i(TAG, "STT Task Initialized");
            if (this.appSttListener != null) {
                this.appSttListener.onSttReady();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing STT task" + e.getMessage(), e);
            if (this.appSttListener != null) {
                this.appSttListener.onError("Error initializing STT: " + e.getMessage());
            }
        }
    }


    @Override
    public void startListening(boolean listenContinuously) {
        if (currentSttTask == null || !isInitialized) {
            Log.e(TAG, "STT task is null or not initialized, Call prepareSTTEngine and initializeListening first");
            if (this.appSttListener != null) {
                this.appSttListener.onError("STT not ready to start. Prepare and initialize first");
            }
            return;
        }
        if (isListening) {
            Log.w(TAG, "STT is already listening");
            return;
        }

        Log.d(TAG, "Starting STT task, continuous: " + listenContinuously);
        try {
            currentSttTask.start(listenContinuously, ); //TODO implement callback
        }

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

    private final ISTTCallback.Stub serviceSttCallback = new ISTTCallback.Stub() {
        @Override
        public void onSuccess(STTResultsData iResults) throws RemoteException {
            Log.i(TAG, "STT onSuccess received data.");
            if (appSttListener == null) {
                Log.w(TAG, "appSttListener is null in onSuccess, cannot propagate result.");
                return;
            }

            if (iResults != null && iResults.getResults() != null && !iResults.getResults().isEmpty()) {
                // first result as most relevant
                STTResult topResult = iResults.getResults().get(0);
                String utterance = topResult.getUtterance();
                float confidence = topResult.getConfidence(); // Assuming getConfidence() returns a float
                // String rule = topResult.getRule();

                Log.i(TAG, "Utterance: " + utterance + ", Confidence: " + confidence);
                appSttListener.onSpeechResult(utterance, confidence);

                // If not continuous, the task might stop itself or need explicit stopping/re-initialization.
                // Our service's isListening flag would be managed by start/stop/pause calls.
            } else {
                Log.w(TAG, "STT onSuccess but results are empty or null.");
                // For clarity, we can call onError.
                appSttListener.onError("No speech recognized or empty results.");
            }
        }

        @Override
        public void onError(String iError) throws RemoteException {
            Log.e(TAG, "STT onError: " + iError);
            if (appSttListener == null) {
                Log.w(TAG, "appSttListener is null in onError, cannot propagate error.");
                return;
            }
            appSttListener.onError(iError);
            isListening = false; // Assume listening stops on error
        }
}

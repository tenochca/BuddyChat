package com.example.buddychat.network;
import com.example.buddychat.BuildConfig;

import androidx.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import okhttp3.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.example.buddychat.network.model.Profile;
import com.example.buddychat.network.model.AuthResponse;


// =======================================================================
// API call utility
// =======================================================================
// Right now just logging in & health check
public class NetworkUtils {
    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------
    public static final OkHttpClient CLIENT = new OkHttpClient(); // Re-used client
    private static final Gson GSON = new Gson();
    private static final String TAG  = "[DPU_NetworkUtils]";
    //private static final String BASE = "https://sandbox.cognibot.org/api";
    //private static final String BASE = "http://10.0.2.2:8000/api";
    private static final String BASE =
            "1".equals(BuildConfig.TEST_LOCAL)
                    ? "http://10.0.2.2:8000/api"             // local docker container
                    : "https://cognibot.org/api";            // cloud server
                    //: "https://sandbox.cognibot.org/api";  // cloud server


    // -----------------------------------------------------------------------
    // Callbacks
    // -----------------------------------------------------------------------
    public interface AuthCallback {
        void onSuccess(String accessToken);
        void onError(Throwable t);
    }
    public interface ProfileCallback {
        void onSuccess(Profile p);
        void onError(Throwable t);
    }

    // -----------------------------------------------------------------------
    // Pings the /health endpoint and logs the HTTP status + body
    // -----------------------------------------------------------------------
    public static void pingHealth() {
        Request request = new Request.Builder().url(BASE + "/health/").get().build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override public void onFailure (@NonNull Call call, @NonNull IOException e) {Log.e(TAG, String.format("%s Ping failed: %s", TAG, e));}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String text = body != null ? body.string() : "<empty>";
                    Log.d(TAG, String.format("%s Status: %s - %s", TAG, response.code(), text));
                }
            }
        });
    }


    // -----------------------------------------------------------------------
    // Login
    // -----------------------------------------------------------------------
    public static void login(AuthCallback cb) {
        // Setup the request payload
        JsonObject payload = new JsonObject();
        payload.addProperty("username", BuildConfig.API_USER);
        payload.addProperty("password", BuildConfig.API_PASS);

        String endpoint = String.format("%s/token/", BASE);
        RequestBody body = RequestBody.create(GSON.toJson(payload), MediaType.get("application/json"));
        Request     req  = new Request.Builder().url(endpoint).post(body).build();

        // Logging
        Log.d(TAG, String.format("%s Calling %s with username: %s, password: %s", TAG, endpoint, BuildConfig.API_USER, BuildConfig.API_PASS));

        // Make the API call
        CLIENT.newCall(req).enqueue(new Callback() {
            @Override public void onFailure (@NonNull Call c, @NonNull IOException e) { cb.onError(e); }
            @Override public void onResponse(@NonNull Call c, @NonNull Response    r) {
                try (ResponseBody b = r.body()) {
                    if (!r.isSuccessful()) throw new IOException("HTTP " + r.code());
                    String raw = b.string();
                    Log.d(TAG, String.format("%s Token JSON: %s", TAG, raw));

                    AuthResponse ar = GSON.fromJson(raw, AuthResponse.class);
                    cb.onSuccess(ar.access);
                    Log.i(TAG, String.format("%s Login success", TAG));

                } catch (Exception ex) { cb.onError(ex); }
            }
        });
    }

    // --------------------------------------------------------------------
    // Use tokens to get profile information
    // --------------------------------------------------------------------
    public static void fetchProfile(String accessToken, ProfileCallback cb) {
        Request req = new Request.Builder()
                .url(BASE + "/profile/")
                .header("Authorization", "Bearer " + accessToken)
                .get().build();

        CLIENT.newCall(req).enqueue(new Callback() {
            @Override public void onFailure (@NonNull Call c, @NonNull IOException e) { cb.onError(e); }
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) {
                try (ResponseBody b = r.body()) {
                    if (!r.isSuccessful()) { throw new IOException("HTTP " + r.code());                  }
                    if (b == null        ) { Log.e(TAG, String.format("%s body was null", TAG)); return; }

                    String raw = b.string();
                    Log.i(TAG, String.format("%s PROFILE JSON: %s", TAG, raw));

                    Profile p = GSON.fromJson(raw, Profile.class);
                    cb.onSuccess(p);

                } catch (Exception ex) { cb.onError(ex); }
            }
        });
    }

    private NetworkUtils() {}   // no instances

}

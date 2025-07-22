package com.example.buddychat.network;
import com.example.buddychat.BuildConfig;

import androidx.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import okhttp3.*;
import com.google.gson.Gson;



// ====================================================================
// API call utility
// ====================================================================
public class NetworkUtils {
    // --------------------------------------------------------------------
    // Constants
    // --------------------------------------------------------------------
    private static final OkHttpClient CLIENT = new OkHttpClient(); // Re-used client
    private static final Gson GSON = new Gson();
    private static final String TAG  = "HTTP";
    private static final String BASE = "https://cognibot.org";

    // --------------------------------------------------------------------
    // Caller passes a callback that receives a parsed object (or error)
    // --------------------------------------------------------------------
    public interface HealthCallback {
        void onSuccess(ApiResponse resp);
        void onError(Throwable t);
    }

    // --------------------------------------------------------------------
    // Pings the /health endpoint and logs the HTTP status + body
    // --------------------------------------------------------------------
    public static void pingHealth() {
        Request request = new Request.Builder().url(BASE + "/health").get().build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Ping failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String text = body != null ? body.string() : "<empty>";
                    Log.d(TAG, "Status " + response.code() + " – " + text);
                }
            }
        });
    }

    // --------------------------------------------------------------------
    // Sign in / token acquisition
    // --------------------------------------------------------------------
    public static void getTokens() {
        Log.d("NETWORK", "getTokens not yet implemented.");

        String apiUser = BuildConfig.API_USER;
        String apiPass = BuildConfig.API_PASS;
        String message = String.format("Username: %s, Password: %s", apiUser, apiPass);

        Log.d("TEST", message);


        RequestBody body = FormBody.create(
                "username=" + apiUser + "&password=" + apiPass,
                MediaType.get("application/x-www-form-urlencoded")
        );

    }
}

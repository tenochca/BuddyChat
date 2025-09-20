package com.example.buddychat.network.model;

public interface AuthListener {
    void onSuccess(String    token);
    void onError  (Throwable t    );
}

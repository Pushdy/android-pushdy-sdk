package com.pushdy.example;

import android.app.Application;
import android.app.Notification;

import com.pushdy.Pushdy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class JavaExampleApplication extends Application implements Pushdy.PushdyDelegate {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public boolean readyForHandlingNotification() {
        return false;
    }

    @Override
    public void onNotificationReceived(@NotNull Map<String, ?> notification, @NotNull String fromState) {

    }

    @Override
    public void onNotificationOpened(@NotNull Map<String, ?> notification, @NotNull String fromState) {

    }

    @Override
    public void onRemoteNotificationRegistered(@NotNull String deviceToken) {

    }

    @Override
    public void onRemoteNotificationFailedToRegister(@NotNull Exception error) {

    }

    @Nullable
    @Override
    public Notification customNotification(@NotNull String title, @NotNull String body, @NotNull Map<String, ?> data) {
        return null;
    }
}

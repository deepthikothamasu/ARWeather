package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final int PERMISSION_CODE = 1001; // Define your request code here
    private PermissionHandler permissionHandler;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        getFirebaseMessage(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
    }

    public void setPermissionHandler(PermissionHandler handler) {
        this.permissionHandler = handler;
    }

    private void requestPermissions() {
        if (permissionHandler != null) {
            // Pass the permission request to the handler
            permissionHandler.onRequestPermissions(new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED}, PERMISSION_CODE);
        }
    }

    public void getFirebaseMessage(String title, String msg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "myFirebasechannel")
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle(title)
                .setContentText(msg)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                requestPermissions();
                return;
            }
        }

        manager.notify(101, builder.build());
    }

    public interface PermissionHandler {
        void onRequestPermissions(String[] permissions, int requestCode);
    }
}

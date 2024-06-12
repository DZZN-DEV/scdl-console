package com.chaquo.python.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import java.util.List;
import java.util.ArrayList;

public class Permissions {

    private static final int REQUEST_CODE = 100; // Define a constant for permission requests

    public static void checkAndRequestPermissions(final Activity activity) {
        String[] allPermissions = {
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.QUERY_ALL_PACKAGES,
            android.Manifest.permission.REQUEST_INSTALL_PACKAGES,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : allPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsNeeded.toArray(new String[0]), REQUEST_CODE);
        }
    }

    public static void handlePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, final Activity activity) {
        if (requestCode == REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                // If any permission is not granted, show an AlertDialog to open settings
                new AlertDialog.Builder(activity)
                        .setTitle("Permission Required")
                        .setMessage("Permissions are necessary for all features to function properly. Please go to settings and allow all required permissions.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            openSettings(activity);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> activity.finish())
                        .create()
                        .show();
            }
        }
    }

    private static void openSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}

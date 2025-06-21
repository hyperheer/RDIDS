package com.example.rdds.permission;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;

public class PermissionManager {
    private static final String PREF_NAME = "AppPermissions";
    private static final String KEY_LOCATION_REQUESTED = "location_requested";
    private static final String KEY_CAMERA_REQUESTED = "camera_requested";
    private static PermissionManager instance;
    private final SharedPreferences prefs;

    private PermissionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }

    // 检查是否已请求过定位权限
    public boolean hasRequestedLocationPermission() {
        return prefs.getBoolean(KEY_LOCATION_REQUESTED, false);
    }

    // 标记已请求过定位权限
    public void markLocationPermissionRequested() {
        prefs.edit().putBoolean(KEY_LOCATION_REQUESTED, true).apply();
    }

    // 检查是否有定位权限
    public boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // 检查是否已请求过拍照权限
    public boolean hasRequestedCameraPermission() {
        return prefs.getBoolean(KEY_CAMERA_REQUESTED, false);
    }

    // 标记已请求过拍照权限
    public void markCameraPermissionRequested() {
        prefs.edit().putBoolean(KEY_CAMERA_REQUESTED, true).apply();
    }

    // 检查是否有拍照权限
    public boolean hasCameraPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
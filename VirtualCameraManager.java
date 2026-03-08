package com.virtualcamera.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/**
 * VirtualCameraManager - Manages the lifecycle of the virtual camera service.
 */
public class VirtualCameraManager {

    private final Context context;
    private boolean active = false;

    public VirtualCameraManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean activate(Uri imageUri) {
        if (imageUri == null) return false;

        Intent intent = new Intent(context, VirtualCameraService.class);
        intent.setAction(VirtualCameraService.ACTION_START);
        intent.putExtra(VirtualCameraService.EXTRA_IMAGE_URI, imageUri.toString());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            active = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void updateImage(Uri imageUri) {
        if (!active || imageUri == null) return;

        Intent intent = new Intent(context, VirtualCameraService.class);
        intent.setAction(VirtualCameraService.ACTION_UPDATE_IMAGE);
        intent.putExtra(VirtualCameraService.EXTRA_IMAGE_URI, imageUri.toString());
        context.startService(intent);
    }

    public void deactivate() {
        Intent intent = new Intent(context, VirtualCameraService.class);
        intent.setAction(VirtualCameraService.ACTION_STOP);
        context.startService(intent);
        active = false;
    }

    public boolean isActive() {
        return active;
    }
}

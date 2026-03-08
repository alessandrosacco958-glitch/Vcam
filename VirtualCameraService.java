package com.virtualcamera.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.IBinder;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * VirtualCameraService - Provides a virtual camera feed using selected images.
 * Uses Android's Camera2 API virtual camera capabilities.
 * Note: Full virtual camera injection requires system-level permissions (root/system app).
 * This service demonstrates the architecture and provides the streaming pipeline.
 */
public class VirtualCameraService extends Service {

    private static final String TAG = "VirtualCameraService";
    private static final String CHANNEL_ID = "VirtualCameraChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 720;
    private static final int FRAME_RATE = 30;

    public static final String ACTION_START = "com.virtualcamera.app.START";
    public static final String ACTION_STOP = "com.virtualcamera.app.STOP";
    public static final String ACTION_UPDATE_IMAGE = "com.virtualcamera.app.UPDATE_IMAGE";
    public static final String EXTRA_IMAGE_URI = "image_uri";

    private volatile boolean isRunning = false;
    private Uri currentImageUri;
    private Bitmap currentBitmap;
    private Thread frameThread;

    // Callback interface for image data
    public interface FrameCallback {
        void onFrame(byte[] yuvData, int width, int height);
    }

    private static FrameCallback frameCallback;

    public static void setFrameCallback(FrameCallback callback) {
        frameCallback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (action == null) return START_NOT_STICKY;

        switch (action) {
            case ACTION_START:
                String uriStr = intent.getStringExtra(EXTRA_IMAGE_URI);
                if (uriStr != null) {
                    currentImageUri = Uri.parse(uriStr);
                    startVirtualCamera();
                }
                break;
            case ACTION_STOP:
                stopVirtualCamera();
                break;
            case ACTION_UPDATE_IMAGE:
                String newUri = intent.getStringExtra(EXTRA_IMAGE_URI);
                if (newUri != null) {
                    currentImageUri = Uri.parse(newUri);
                    loadBitmap();
                }
                break;
        }

        return START_STICKY;
    }

    private void startVirtualCamera() {
        startForeground(NOTIFICATION_ID, buildNotification());
        loadBitmap();
        startFrameLoop();
        isRunning = true;
        Log.d(TAG, "Virtual camera started");
    }

    private void stopVirtualCamera() {
        isRunning = false;
        if (frameThread != null) {
            frameThread.interrupt();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }
        stopForeground(true);
        stopSelf();
        Log.d(TAG, "Virtual camera stopped");
    }

    private void loadBitmap() {
        try {
            if (currentImageUri == null) return;
            InputStream is = getContentResolver().openInputStream(currentImageUri);
            if (is == null) return;

            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();

            if (raw != null) {
                // Scale and crop to target dimensions
                currentBitmap = scaleBitmapCenterCrop(raw, TARGET_WIDTH, TARGET_HEIGHT);
                if (raw != currentBitmap) raw.recycle();
                Log.d(TAG, "Bitmap loaded: " + currentBitmap.getWidth() + "x" + currentBitmap.getHeight());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap", e);
        }
    }

    private void startFrameLoop() {
        frameThread = new Thread(() -> {
            long frameDuration = 1000L / FRAME_RATE;
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                long start = System.currentTimeMillis();
                if (currentBitmap != null && !currentBitmap.isRecycled() && frameCallback != null) {
                    byte[] yuvData = bitmapToYuv420(currentBitmap);
                    frameCallback.onFrame(yuvData, TARGET_WIDTH, TARGET_HEIGHT);
                }
                long elapsed = System.currentTimeMillis() - start;
                long sleep = frameDuration - elapsed;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        frameThread.setName("VirtualCameraFrameThread");
        frameThread.setDaemon(true);
        frameThread.start();
    }

    /**
     * Convert Bitmap to YUV420 byte array (NV21 format).
     * Used for feeding into Camera2 virtual streams.
     */
    public static byte[] bitmapToYuv420(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);

        byte[] yuv = new byte[width * height * 3 / 2];
        int yIndex = 0;
        int uvIndex = width * height;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int pixel = argb[j * width + i];
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // RGB to YUV conversion
                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                yuv[yIndex++] = (byte) Math.max(0, Math.min(255, y));

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = (byte) Math.max(0, Math.min(255, v));
                    yuv[uvIndex++] = (byte) Math.max(0, Math.min(255, u));
                }
            }
        }
        return yuv;
    }

    private Bitmap scaleBitmapCenterCrop(Bitmap source, int targetW, int targetH) {
        float srcRatio = (float) source.getWidth() / source.getHeight();
        float dstRatio = (float) targetW / targetH;

        int newW, newH, offsetX, offsetY;
        if (srcRatio > dstRatio) {
            newH = targetH;
            newW = (int) (source.getWidth() * ((float) targetH / source.getHeight()));
            offsetX = (newW - targetW) / 2;
            offsetY = 0;
        } else {
            newW = targetW;
            newH = (int) (source.getHeight() * ((float) targetW / source.getWidth()));
            offsetX = 0;
            offsetY = (newH - targetH) / 2;
        }

        Bitmap scaled = Bitmap.createScaledBitmap(source, newW, newH, true);
        Bitmap cropped = Bitmap.createBitmap(scaled, offsetX, offsetY, targetW, targetH);
        if (scaled != cropped) scaled.recycle();
        return cropped;
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, VirtualCameraService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPending = PendingIntent.getActivity(this, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.camera_running))
                .setSmallIcon(R.drawable.ic_camera)
                .setContentIntent(mainPending)
                .addAction(R.drawable.ic_camera_off, getString(R.string.stop), stopPending)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.channel_desc));

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

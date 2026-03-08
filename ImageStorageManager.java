package com.virtualcamera.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ImageStorageManager {

    private static final String PREFS_NAME = "VirtualCameraPrefs";
    private static final String KEY_IMAGES = "saved_images";

    private final SharedPreferences prefs;

    public ImageStorageManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveImages(List<ImageItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (ImageItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("uri", item.getUri());
                obj.put("name", item.getName());
                array.put(obj);
            }
            prefs.edit().putString(KEY_IMAGES, array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<ImageItem> loadImages() {
        List<ImageItem> result = new ArrayList<>();
        String json = prefs.getString(KEY_IMAGES, null);
        if (json == null) return result;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new ImageItem(
                        obj.getString("uri"),
                        obj.getString("name")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}

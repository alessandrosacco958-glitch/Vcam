package com.virtualcamera.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnImageSelectedListener {

    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;
    private MaterialCardView cardPreview;
    private ImageView ivSelectedImage;
    private TextView tvSelectedName;
    private MaterialButton btnActivate;
    private MaterialButton btnAddImages;
    private FloatingActionButton fabAdd;
    private View emptyState;

    private Uri selectedImageUri = null;
    private List<ImageItem> imageList = new ArrayList<>();
    private VirtualCameraManager cameraManager;
    private boolean isCameraActive = false;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) { allGranted = false; break; }
                }
                if (allGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            addImageToList(uri);
                        }
                    } else if (data.getData() != null) {
                        addImageToList(data.getData());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraManager = new VirtualCameraManager(this);
        initViews();
        setupRecyclerView();
        loadSampleImages();
    }

    private void initViews() {
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        cardPreview = findViewById(R.id.cardPreview);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        tvSelectedName = findViewById(R.id.tvSelectedName);
        btnActivate = findViewById(R.id.btnActivate);
        btnAddImages = findViewById(R.id.btnAddImages);
        fabAdd = findViewById(R.id.fabAdd);
        emptyState = findViewById(R.id.emptyState);

        btnActivate.setOnClickListener(v -> toggleVirtualCamera());
        btnAddImages.setOnClickListener(v -> checkPermissionsAndPick());
        fabAdd.setOnClickListener(v -> checkPermissionsAndPick());

        cardPreview.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(this, imageList, this);
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewImages.setAdapter(imageAdapter);
    }

    private void loadSampleImages() {
        // Load previously saved images from SharedPreferences
        ImageStorageManager storage = new ImageStorageManager(this);
        List<ImageItem> saved = storage.loadImages();
        if (!saved.isEmpty()) {
            imageList.addAll(saved);
            imageAdapter.notifyDataSetChanged();
            updateEmptyState();
        }
    }

    private void addImageToList(Uri uri) {
        // Persist URI permission
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            // Permission already taken or not persistable
        }

        String name = UriHelper.getFileName(this, uri);
        ImageItem item = new ImageItem(uri.toString(), name);

        if (!imageList.contains(item)) {
            imageList.add(item);
            imageAdapter.notifyItemInserted(imageList.size() - 1);
            updateEmptyState();

            // Save to storage
            ImageStorageManager storage = new ImageStorageManager(this);
            storage.saveImages(imageList);

            Snackbar.make(recyclerViewImages, getString(R.string.image_added, name), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageSelected(ImageItem item, int position) {
        selectedImageUri = Uri.parse(item.getUri());
        imageAdapter.setSelectedPosition(position);

        cardPreview.setVisibility(View.VISIBLE);
        tvSelectedName.setText(item.getName());
        Glide.with(this).load(selectedImageUri).centerCrop().into(ivSelectedImage);

        // Update virtual camera with selected image
        if (isCameraActive) {
            cameraManager.updateImage(selectedImageUri);
        }

        btnActivate.setEnabled(true);
    }

    @Override
    public void onImageDelete(ImageItem item, int position) {
        imageList.remove(position);
        imageAdapter.notifyItemRemoved(position);

        if (imageList.isEmpty()) {
            cardPreview.setVisibility(View.GONE);
            selectedImageUri = null;
            btnActivate.setEnabled(false);
        }

        updateEmptyState();

        ImageStorageManager storage = new ImageStorageManager(this);
        storage.saveImages(imageList);

        Toast.makeText(this, R.string.image_removed, Toast.LENGTH_SHORT).show();
    }

    private void toggleVirtualCamera() {
        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.select_image_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isCameraActive) {
            activateVirtualCamera();
        } else {
            deactivateVirtualCamera();
        }
    }

    private void activateVirtualCamera() {
        boolean success = cameraManager.activate(selectedImageUri);
        if (success) {
            isCameraActive = true;
            btnActivate.setText(R.string.deactivate_camera);
            btnActivate.setIconResource(R.drawable.ic_camera_off);
            btnActivate.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.red_500));

            Snackbar.make(recyclerViewImages, R.string.camera_activated, Snackbar.LENGTH_LONG)
                    .setAction(R.string.how_to_use, v -> showHowToUse())
                    .show();
        } else {
            showVirtualCameraInstructions();
        }
    }

    private void deactivateVirtualCamera() {
        cameraManager.deactivate();
        isCameraActive = false;
        btnActivate.setText(R.string.activate_camera);
        btnActivate.setIconResource(R.drawable.ic_camera);
        btnActivate.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.primary));
        Toast.makeText(this, R.string.camera_deactivated, Toast.LENGTH_SHORT).show();
    }

    private void showHowToUse() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.how_to_use_title)
                .setMessage(R.string.how_to_use_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showVirtualCameraInstructions() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.setup_required)
                .setMessage(R.string.setup_message)
                .setPositiveButton(R.string.open_settings, (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void checkPermissionsAndPick() {
        List<String> permsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (permsNeeded.isEmpty()) {
            openImagePicker();
        } else {
            permissionLauncher.launch(permsNeeded.toArray(new String[0]));
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(intent);
    }

    private void updateEmptyState() {
        if (imageList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewImages.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewImages.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isCameraActive) {
            cameraManager.deactivate();
        }
    }
}

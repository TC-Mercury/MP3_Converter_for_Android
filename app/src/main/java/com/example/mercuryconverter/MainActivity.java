//This code was made by TC__Mercury and is for educational purposes and personal use.
package com.example.mercuryconverter;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.media.MediaScannerConnection;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.DownloadProgressCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
/*
 * This is the primary entry point of the application.
 * It handles User Interface interactions, permission requests,
 * and the core logic for downloading and converting YouTube videos to MP3.
 */

public class MainActivity extends AppCompatActivity {
    DataBase db;
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private EditText editTextLink;
    private Button btnDownload;
    private ProgressBar progressBar;
    private TextView tvStatus;

    // Files will be saved in this directory
    private File mercuryDir;
    private DownloadService downloadService;
    private boolean isBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadService.LocalBinder binder = (DownloadService.LocalBinder) service;
            downloadService = binder.getService();
            isBound = true;
            downloadService.setProgressListener(new DownloadService.ProgressListener() {
                @Override
                public void onProgress(int progress, String status) {
                    runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                        tvStatus.setText(status);
                    });
                }
                @Override
                public void onCompleted(String title, String path) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(ProgressBar.GONE);
                        tvStatus.setText(R.string.msg_completed);
                        btnDownload.setEnabled(true);
                        editTextLink.setEnabled(true);
                        editTextLink.setText("");
                        Toast.makeText(MainActivity.this, getString(R.string.msg_saved_to) + " Download/MercuryFile", Toast.LENGTH_LONG).show();
                    });
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(ProgressBar.GONE);
                        tvStatus.setText(getString(R.string.error_prefix) + error);
                        btnDownload.setEnabled(true);
                        editTextLink.setEnabled(true);
                    });
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        syncUiWithServiceState();
        Intent serviceIntent = new Intent(this, DownloadService.class);
        bindService(serviceIntent, connection, Context.BIND_ABOVE_CLIENT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DataBase(this);

        editTextLink = findViewById(R.id.editTextLink);
        btnDownload = findViewById(R.id.btnDownload);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    String extractedUrl = extractUrl(sharedText);
                    if (extractedUrl != null) {
                        editTextLink.setText(extractedUrl);
                    }
                }
            }
        }

        btnDownload.setEnabled(false);
        btnDownload.setText(R.string.msg_checking_updates);
        Button btnOpenHistory = findViewById(R.id.btnOpenHistory);
        btnOpenHistory.setOnClickListener(v -> {
            showBottomSheetHistory();
        });

        // Try to initialize libraries immediately upon app launch
        try {
            initLibrary();
            checkPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Initialization Error", e);
            tvStatus.setText(R.string.init_error);
        }
        btnDownload.setOnClickListener(v -> startDownload());
    }

    private void syncUiWithServiceState() {
        if (DownloadStateHolder.isDownloading) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            progressBar.setProgress(DownloadStateHolder.lastProgress);
            tvStatus.setText(DownloadStateHolder.lastStatus);
            btnDownload.setEnabled(false);
            editTextLink.setEnabled(false);
        } else if ("completed".equals(DownloadStateHolder.pendingResult)) {
            progressBar.setVisibility(ProgressBar.GONE);
            tvStatus.setText(R.string.msg_completed);
            btnDownload.setEnabled(true);
            editTextLink.setEnabled(true);
            editTextLink.setText("");
            DownloadStateHolder.pendingResult = null; // tüketildi
        } else if (DownloadStateHolder.pendingResult != null && DownloadStateHolder.pendingResult.startsWith("error:")) {
            progressBar.setVisibility(ProgressBar.GONE);
            tvStatus.setText(getString(R.string.error_prefix) + DownloadStateHolder.pendingResult.substring(6));
            btnDownload.setEnabled(true);
            editTextLink.setEnabled(true);
            DownloadStateHolder.pendingResult = null;
        }
    }

    private String extractUrl(String text) {
        String urlRegex = "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(urlRegex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    private void showBottomSheetHistory() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(MainActivity.this);

        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_history, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        androidx.recyclerview.widget.RecyclerView recyclerView =
                bottomSheetView.findViewById(R.id.recyclerViewHistory);

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        android.database.Cursor cursor = db.getAllHistory();

        HistoryAdapter adapter = new HistoryAdapter(this, cursor, new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onLinkSelected(String url) {
                editTextLink.setText(url);
                bottomSheetDialog.dismiss();
            }
        });
        recyclerView.setAdapter(adapter);
        bottomSheetDialog.show();
    }


    /*
     * Initializes the underlying YoutubeDL and FFmpeg libraries.
     * It also triggers the update process to ensure the latest yt-dlp engine is used.
     */
    private void initLibrary() {
        try {
            // Initialize main library
            YoutubeDL.getInstance().init(getApplicationContext());
            // Initialize FFmpeg library
            FFmpeg.getInstance().init(getApplicationContext());
            Log.d(TAG, "YoutubeDL initialized successfully");

            File ffmpegFile = new File(getApplicationContext().getApplicationInfo().nativeLibraryDir, "libffmpeg.so");
            if (ffmpegFile.exists()) {
                Log.d(TAG, "FFmpeg found: " + ffmpegFile.getAbsolutePath());
            }
            // Start engine update
            updateYoutubeDL();

        } catch (YoutubeDLException e) {
            Log.e(TAG, "Initialization Error", e);
            Toast.makeText(this, getString(R.string.library_init_failed), Toast.LENGTH_LONG).show();
        }
    }
    /*
     * Checks for necessary storage permissions based on Android version.
     * - Android 13+: READ_MEDIA_AUDIO
     * - Android 10-12: READ/WRITE_EXTERNAL_STORAGE
     */
    private void checkPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ handles storage permissions differently (Scoped Storage).
            // Usually no extra permission needed for Downloads folder, but keeping valid check.
        } else {
            // For Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            prepareDirectories();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            prepareDirectories();
        }
    }

     // Sets up the target directory for downloads.
    private void prepareDirectories() {
        mercuryDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MercuryFile");
        if (!mercuryDir.exists()) {
            mercuryDir.mkdirs();
        }
        tvStatus.setText(R.string.status_ready);
        // Trigger update check again to be safe
        updateYoutubeDL();
    }

    /*
     * Updates the internal yt-dlp binary to the latest version.
     * IMPORTANT: Used {@code UpdateChannel._NIGHTLY} because the STABLE version
     * was often outdated against YouTube's anti-bot measures.
     */
    private void updateYoutubeDL() {
        runOnUiThread(() -> {
            tvStatus.setText(R.string.msg_checking_updates);
            Toast.makeText(MainActivity.this, getString(R.string.msg_checking_updates), Toast.LENGTH_SHORT).show();
        });

        new Thread(() -> {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(getApplicationContext(), YoutubeDL.UpdateChannel._NIGHTLY);
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.msg_engine_updated);
                    Toast.makeText(MainActivity.this, getString(R.string.msg_engine_updated), Toast.LENGTH_LONG).show();
                    btnDownload.setEnabled(true);
                    btnDownload.setText(R.string.action_download);
                });
            } catch (Exception e) {
                Log.e(TAG, "Update Error: " + e.getMessage());
                e.printStackTrace();
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.msg_update_failed);
                    Toast.makeText(MainActivity.this, getString(R.string.msg_update_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show();
                    btnDownload.setEnabled(true);
                    btnDownload.setText(R.string.action_download);
                });
            }
        }).start();
    }
    private void ensureMercuryDir() {
        if (mercuryDir == null) {
            mercuryDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MercuryFile");
        }
        if (!mercuryDir.exists()) {
            mercuryDir.mkdirs();
        }
    }

    //Executes the download and conversion process.
    private void startDownload() {
        String url = editTextLink.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_link), Toast.LENGTH_SHORT).show();
            return;
        }
        String youtubeRegex = "^(https?\\:\\/\\/)?(www\\.)?(youtube\\.com|youtu\\.?be|music\\.youtube\\.com)\\/.+$";
        if (!url.matches(youtubeRegex)) {
            Toast.makeText(this, R.string.error_wrong_link, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        tvStatus.setText(R.string.status_downloading);
        btnDownload.setEnabled(false);
        editTextLink.setEnabled(false);

        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.putExtra(DownloadService.EXTRA_URL, url);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }
}
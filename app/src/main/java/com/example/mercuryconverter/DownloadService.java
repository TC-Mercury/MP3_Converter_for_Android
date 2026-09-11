package com.example.mercuryconverter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import android.media.MediaScannerConnection;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.PowerManager;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    public static final String EXTRA_URL = "extra_url";

    private final IBinder binder = new LocalBinder();
    private ExecutorService executor;
    private NotificationCompat.Builder notifBuilder;
    private NotificationManager notificationManager;
    private File mercuryDir;
    private PowerManager.WakeLock wakeLock;
    private PendingIntent pendingIntent;

    public interface ProgressListener {
        void onProgress(int progress, String status);
        void onCompleted(String title, String path);
        void onError(String error);
    }
    private ProgressListener listener;

    public class LocalBinder extends Binder {
        DownloadService getService() { return DownloadService.this; }
    }

    public void setProgressListener(ProgressListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationHelper.createChannel(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra(EXTRA_URL);

        Intent notifIntent = new Intent(this, MainActivity.class);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(
                this, 0, notifIntent,
                PendingIntent.FLAG_IMMUTABLE);

        notifBuilder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notif_downloading_title))
                .setContentText(getString(R.string.notif_preparing))
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        startForeground(NotificationHelper.NOTIFICATION_ID, notifBuilder.build());
        executor.execute(() -> runDownload(url));
        return START_NOT_STICKY;
    }

    private void runDownload(String url) {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MercuryConverter:DownloadWakeLock");
        wakeLock.acquire(30 * 60 * 1000L);
        try {
            ensureMercuryDir();
            DownloadStateHolder.isDownloading = true;
            com.yausername.youtubedl_android.mapper.VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(url);
            String videoTitle = streamInfo.getTitle() != null ? streamInfo.getTitle() : getString(R.string.unknown_song);

            YoutubeDLRequest request = new YoutubeDLRequest(url);

            String ffmpegPath = "";
            File libJunkfood = new File(getApplicationInfo().dataDir, "libffmpeg.so");
            File libFiles = new File(getFilesDir(), "libffmpeg.so");
            File libNative = new File(getApplicationInfo().nativeLibraryDir, "libffmpeg.so");
            if (libJunkfood.exists()) ffmpegPath = libJunkfood.getAbsolutePath();
            else if (libFiles.exists()) ffmpegPath = libFiles.getAbsolutePath();
            else if (libNative.exists()) ffmpegPath = libNative.getAbsolutePath();
            if (!ffmpegPath.isEmpty()) request.addOption("--ffmpeg-location", ffmpegPath);

            request.addOption("-x");
            request.addOption("--audio-format", "mp3");
            request.addOption("--audio-quality", "0");
            request.addOption("--embed-metadata");
            request.addOption("--embed-thumbnail");
            request.addOption("--add-metadata");
            request.addOption("--recode-video", "mp3");
            request.addOption("--metadata-from-title", "%(artist)s - %(title)s");
            request.addOption("-o", mercuryDir.getAbsolutePath() + "/%(artist)s - %(title)s.%(ext)s");
            request.addOption("--extractor-args", "youtube:player_client=android");
            request.addOption("--force-ipv4");
            request.addOption("--no-playlist");
            request.addOption("--format", "bestaudio/best");
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                request.addOption("--no-check-certificate");
            }

            YoutubeDL.getInstance().execute(request, "mercury_process", (progress, eta, line) -> {
                int prog = (progress != null) ? progress.intValue() : 0;
                updateNotification(prog, line);
                DownloadStateHolder.lastProgress = prog;
                DownloadStateHolder.lastStatus = line;
                if (listener != null) listener.onProgress(prog, line);
                return null;
            });

            DataBase db = new DataBase(this);
            db.addDownload(videoTitle, url, mercuryDir.getAbsolutePath());

            MediaScannerConnection.scanFile(
                    getApplicationContext(),
                    new String[]{mercuryDir.getAbsolutePath()},
                    null,
                    (path, uri) -> Log.i(TAG, "Media Scan completed: " + path));

            showCompletedNotification(videoTitle);
            DownloadStateHolder.pendingResult = "completed";
            if (listener != null) listener.onCompleted(videoTitle, mercuryDir.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Download error", e);
            DownloadStateHolder.pendingResult = "error:" + e.getMessage();
            showErrorNotification(e.getMessage());
            if (listener != null) listener.onError(e.getMessage());
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            DownloadStateHolder.isDownloading = false;
            stopSelf();
        }
    }

    private void updateNotification(int progress, String status) {
        notifBuilder.setProgress(100, progress, false);
        notifBuilder.setContentText(status);
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notifBuilder.build());
    }

    private void showCompletedNotification(String title) {
        Notification done = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notif_completed_title))
                .setContentText(title)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, done);
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
    }

    private void showErrorNotification(String error) {
        Notification err = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notif_failed_title))
                .setContentText(error != null ? error : getString(R.string.error_unknown))
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, err);
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
    }

    private void ensureMercuryDir() {
        mercuryDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MercuryFile");
        if (!mercuryDir.exists()) mercuryDir.mkdirs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
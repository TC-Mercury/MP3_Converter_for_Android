package com.example.mercuryconverter;

public class DownloadStateHolder {
    public static volatile boolean isDownloading = false;
    public static volatile int lastProgress = 0;
    public static volatile String lastStatus = "";
    public static volatile String pendingResult = null;
}
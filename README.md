#  Mercury Converter for Android

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![YT-DLP](https://img.shields.io/badge/yt--dlp-Nightly-red?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Completed-success?style=for-the-badge)

Mercury Converter is a powerful, native Android application designed to download and convert YouTube videos to MP3 format directly on your device.

It uses **yt-dlp** (Nightly builds) and **FFmpeg** to bypass modern YouTube restrictions (HTTP 403 / PO Token errors) and ensure high-quality audio extraction.

---

##  Features:

* ** MP3 Conversion:** Automatically extracts audio and converts it to high-quality MP3 using a native FFmpeg binary.
* ** Anti-Bot Bypass:** Uses the `_NIGHTLY` update channel and client emulation (`android_testsuite`) to bypass YouTube's latest "PO Token" and bot protections.
* ** Multi-Language:** Full support for **English** 🇺🇸 and **Turkish** 🇹🇷 (Auto-detects system language).
* ** Easy Access:** Saves files directly to the public `Downloads/MercuryFile` directory.
* ** Auto-Update:** Checks and updates the internal yt-dlp engine on launch to keep up with YouTube changes.

---

##  Download & Installation:

You can download the latest APK from the Releases page:

[** Download Latest APK**][https://github.com/TC-Mercury/Mercury_Converter_for_Android/releases/download/v1.0.2/MercuryConverter.apk)

1.  Download `MercuryConverter.apk`.
2.  Allow installation from unknown sources.
3.  Enjoy your music!

---

##  Technical Details:

This project solves several complex challenges in Android development:

* **Manual FFmpeg Detection:** Implements a custom detective logic to find the `libffmpeg.so` binary across `nativeLibraryDir`, `filesDir`, and `dataDir` to prevent "FFmpeg not found" errors on different Android versions.
* **Engine Management:** Updates `yt-dlp` to the latest nightly build dynamically.
* **Scoped Storage:** Compliant with Android 11+ storage policies, writing to the public Downloads collection.

### Libraries Used:
* [youtubedl-android](https://github.com/JunkFood02/youtubedl-android) (JunkFood02 Fork)
* [FFmpeg-Android](https://github.com/JunkFood02/youtubedl-android)

---

## ⚠️ Disclaimer

This project is for **educational and personal use only**.
The developer does not endorse the downloading of copyrighted materials without permission. Please respect YouTube's Terms of Service.

---

**Developed with determination by [TC__Mercury]**

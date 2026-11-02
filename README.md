# ShortSave Android starter

Dark Kotlin + Jetpack Compose app with an Apify-backed Node.js API.

## What is implemented
- Android UI for entering the ReelShort `bookId` and `chapterId`
- Calls a backend `/api/download` endpoint
- Backend runs `spider_studio/reelshort-m3u8-downloader`
- Backend proxies the private Apify Key-Value Store MP4 without exposing the Apify token
- Android DownloadManager saves the MP4 into the device Downloads folder

The Actor accepts IDs, not a normal ReelShort share URL. This starter intentionally asks for both IDs; URL-to-ID resolution is a separate integration and must be verified against ReelShort's permitted interfaces.

## 1. Configure and run backend
Install Node.js 20+ on a server you control.

Windows PowerShell:
```powershell
cd backend
npm install
$env:APIFY_TOKEN="YOUR_APIFY_API_TOKEN"
npm start
```
Deploy it behind HTTPS before using it from the Android app. Do not put the Apify token in the Android project or APK.

Check:
`https://YOUR-BACKEND-DOMAIN/health`

## 2. Open Android project
1. Install Android Studio (current stable) and Android SDK Platform 36.
2. Extract this ZIP.
3. Android Studio > Open > select the `ShortSave` folder (the one containing `settings.gradle.kts`).
4. Let Gradle sync finish. Use JDK 17 if Android Studio asks.
5. Open `app/src/main/java/com/shortsave/app/MainActivity.kt`.
6. The Backend URL field is editable in the app. Enter your HTTPS backend URL.
7. Build > Build Bundle(s) / APK(s) > Build APK(s).
8. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## 3. Apify
Create an Apify account and copy an API token from Apify Console > Settings / API & Integrations. Set it only as the backend environment variable `APIFY_TOKEN`.

The Actor's input:
```json
{
  "chapters": [
    { "bookId": "YOUR_BOOK_ID", "chapterId": "YOUR_CHAPTER_ID" }
  ]
}
```

## Notes
- Actor selects the highest quality available; it does not promise HDR or a fixed resolution.
- Apify charges per successful result and platform compute/storage may also apply. Check current pricing in Apify Console.
- Locked, inaccessible, or restricted episodes may fail. Use only media you have permission to download and retain.
- This is a starter project, not a signed release APK. A production release should add authentication/rate limits to the backend, request validation, logging, privacy policy, and secure hosting.

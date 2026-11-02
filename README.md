ShortSave

ShortSave is an Android app for organizing and saving ReelShort episodes when a downloadable source is available and you have permission to save it.

«Unofficial project. ShortSave is not affiliated with, endorsed by, or sponsored by ReelShort. Only download content you own or are explicitly authorized to download. Do not use the app to bypass DRM, subscriptions, paywalls, access controls, or episode restrictions.»

Features

- Paste a ReelShort share link and request episode information.
- View available episodes and select an accessible episode.
- Request an available video file from the backend.
- Save downloads to Android's Downloads folder.
- Track download progress.
- View app-managed downloads in the Downloads library.
- Play saved videos offline with the built-in player.

Quality note: ShortSave requests the best source quality made available by its backend. Resolution and HDR depend on the source; 1080p or HDR is not guaranteed.

How it works

- Android: Kotlin, Jetpack Compose, Android DownloadManager and AndroidX Media3.
- Backend: Node.js/Express service in "backend/".
- Processing: Apify actors configured in "backend/server.js".
- Security: The Apify API token must remain server-side as "APIFY_TOKEN". Never put it in the Android app or public source code.

Requirements

- Android SDK 36 and JDK 17.
- Node.js 20 or later for the backend.
- An Apify account and API token.
- HTTPS hosting for the backend.

Build the Android app

1. Clone or download this repository.
2. Open the project root (the folder containing "settings.gradle.kts") in Android Studio or a compatible Android IDE.
3. Allow Gradle to sync. Use JDK 17.
4. Build the debug APK, or run:

./gradlew :app:assembleDebug

5. Find the APK at:

"app/build/outputs/apk/debug/app-debug.apk"

A debug APK is for testing. Public distribution requires a signed release APK.

Run the backend

cd backend
npm install
npm start

Before starting, configure "APIFY_TOKEN" in your hosting provider's environment settings. Never commit real credentials or a ".env" file containing secrets.

Health check:

"https://YOUR-BACKEND-DOMAIN/health"

Responsible use and limitations

- Only download videos you own or have explicit permission to save.
- Respect ReelShort's terms, copyright and creators' rights.
- Do not attempt to access locked, paid, private or restricted episodes.
- Video resolution, HDR and availability depend on the source.
- Third-party services may change, become unavailable or incur charges.
- Do not redistribute downloaded videos without permission.

Privacy and security

The app sends ReelShort links and selected episode identifiers to the configured backend for processing. The backend communicates with Apify using its server-side token. Download requests and resulting files may be processed or temporarily stored by the backend or provider.

A separate production privacy policy should explain actual server logs, retention periods, hosting providers, data processing and contact details.

Recommended production safeguards include authentication, rate limiting, input validation, safe error messages, monitoring and a documented data-retention policy.

Contributing

Issues and pull requests are welcome. Never include credentials, private download URLs, copyrighted media or personal information in issues.

License

No license has been added yet. Public visibility does not automatically grant permission to reuse, modify or redistribute this source code.

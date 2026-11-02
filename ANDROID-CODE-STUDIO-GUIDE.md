# ShortSave — Android Code Studio (phone build)

This edition is adjusted for Android Code Studio environments using Gradle 7.4.2. It uses Android Gradle Plugin 7.2.2, Kotlin 1.7.10, and compile SDK 33.

## Import
1. Extract this ZIP.
2. In Android Code Studio choose Open Project / Open Folder.
3. Select the extracted `ShortSave` folder (the one containing `settings.gradle.kts`).
4. If asked which Gradle distribution to use, choose **Gradle Wrapper**. The wrapper is set to Gradle 7.4.2.
5. Set the JDK to 11 or 17 if the IDE asks.
6. Allow Gradle sync to finish; install Android SDK Platform 33 if prompted.

## Build
Use the IDE Build menu and build the `app` module / debug APK. The output is normally `app/build/outputs/apk/debug/app-debug.apk`.

## Important
This project creates the Android app UI. The backend must be deployed separately and configured in the app before downloads work. Keep the Apify token on the backend; never put it in the APK. Only download media you are authorized to save.

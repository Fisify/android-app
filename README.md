# Fisify Android app

The password for signing the App bundle with `key.jks` is `Fisify2025!` for both questions, the key alias is `fisify`.

🚨 IMPORTANT 🚨

Keep in mind that Google Play's SHA-1 signatures can only exist in one Firebase project, so if they are set in production, Google Sign-In on Android will stop working.

## Load localhost from Webview:
- https://stackoverflow.com/questions/52492970/android-webview-not-loading-for-localhost-server

## Authentication with Firebase SDK
- https://firebase.google.com/docs/auth/android/google-signin?hl=es
- https://stackoverflow.com/questions/56915557/firebase-google-authentication-in-web-does-not-work-on-android-app-webview

## Get SHA-1 key for Firebase
- https://stackoverflow.com/questions/15727912/sha-1-fingerprint-of-keystore-certificate
- https://stackoverflow.com/questions/27609442/how-to-get-the-sha-1-fingerprint-certificate-in-android-studio-for-debug-mode
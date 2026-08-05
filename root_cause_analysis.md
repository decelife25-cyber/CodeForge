# Root Cause Analysis

## Issue 1: Debug APK - OAuth Callback Failing

### Symptom:
After authorizing in GitHub, the user is redirected back to the app but ends up on the login screen instead of entering the application.

### Exact Failing Step (Root Cause):
1. **GitHub OAuth Token Endpoint Failure**: The app initiates a PKCE flow and requests a token from `https://github.com/login/oauth/access_token`. However, the token exchange fails because the app does not provide a `client_secret`, which is required for traditional GitHub OAuth Apps (even when using PKCE, unless the app is specifically a GitHub App configured without a client secret or if GitHub rejects the specific client ID used for public PKCE without a secret). This results in a `401 Unauthorized` HTTP response (`incorrect_client_credentials`).
2. **Missing Error Handling in Network Call**: In `AuthRedirectActivity.kt`, the code executes the network call:
   ```kotlin
   val resp = client.newCall(request).execute()
   val body = resp.body?.string()
   if (!resp.isSuccessful || body.isNullOrEmpty()) return@withContext null
   ```
   Because `resp.isSuccessful` is false, it returns `null`.
3. **Silent Failure and UI Reset**: When `exchangeCodeForToken` returns `null`, the calling coroutine evaluates `if (!token.isNullOrEmpty())` as false, skips launching `MainActivity`, and simply calls `finish()`.
4. **Login Screen Re-composition**: Since `AuthRedirectActivity` finishes without launching the app, the back stack returns the user to the existing `MainActivity`, which is still rendering the `LoginScreen`.

**Conclusion**: The exact failing step is the network call to `https://github.com/login/oauth/access_token`, which returns an HTTP 401 error. This happens because the OAuth request is missing the required `client_secret` (or fails due to incorrect client credentials). When the request fails, the activity simply finishes without navigating to the app.

---

## Issue 2: Release APK - Installation Failure

### Symptom:
The Release APK cannot be installed on Android after uninstalling previous versions.

### Exact Failing Step (Root Cause):
1. **Missing Signing Configuration**: In `app/build.gradle.kts`, the `release` build type is defined but does not include any `signingConfig`:
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = false
           proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
       }
   }
   ```
2. **Unsigned APK Generation**: Because no signing configuration is specified (like a Keystore file, key alias, and passwords), Gradle outputs an unsigned APK: `app-release-unsigned.apk`.
3. **Android Package Manager Rejection**: Android requires all APKs to be digitally signed with a certificate before they can be installed. When the user attempts to install the unsigned `app-release-unsigned.apk`, the Android package manager completely rejects it.

**Conclusion**: The release APK is not installable because it is strictly unsigned. The release build type lacks a `signingConfig` instruction.

---

## What Must Be Changed (Before Modifying Code)
1. **For OAuth Failure**:
   - We must add detailed logging to `AuthRedirectActivity.kt` to capture the exact HTTP status code, response body, and error messages during the token exchange to confirm the API rejection.
   - We need to determine if we should embed a `client_secret` for the token exchange, or if the OAuth App configuration on GitHub needs adjusting (which is outside the codebase). If a `client_secret` is required by the current GitHub configuration, it must be added to `AuthConfig.kt` and sent in the `FormBody`.
2. **For Release APK**:
   - A `signingConfig` block must be created in `app/build.gradle.kts`.
   - A keystore (e.g., a debug keystore or a generated release keystore) must be specified and assigned to the `release` build type so that Gradle outputs a signed APK (`app-release.apk`) rather than an unsigned one.

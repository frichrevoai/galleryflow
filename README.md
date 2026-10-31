# GalleryFlow

GalleryFlow is an Android image and video gallery app.

## Build v0.18.0 on GitHub — upload ONE ZIP

1. Download the complete **GalleryFlow-v0.18.0-GitHub-Actions.zip** source archive from ChatGPT.
2. Open https://github.com/frichrevoai/galleryflow/upload/main
3. Upload the **ZIP file itself** directly into the repository root. **Do not extract it.**
4. Click **Commit changes**.
5. GitHub Actions automatically extracts the source ZIP and builds the Android app.
6. Open https://github.com/frichrevoai/galleryflow/actions, select the latest successful **Build GalleryFlow APK** run, and download the artifact **GalleryFlow-v0.18.0-debug-APK**.
7. Extract that artifact and transfer `app-debug.apk` to your Android device for testing.

The workflow supports either an uploaded source ZIP at the repository root or an already checked-in `app/` source directory.

### Installing on a phone

The debug APK is signed by GitHub's new temporary debug signing certificate. It will **not** update an existing GalleryFlow app signed with a different certificate. **Do not uninstall the existing app until you've backed up its data.** To create an in-place update, the APK must be signed with the original v0.17.0 signing key. Do not upload keystores/passwords to this public repository.

### Build dependencies

GitHub Actions installs Java 17, Android SDK 35, build tools 35.0.0, and Gradle 8.9. No local Android SDK or PowerShell execution-policy change is necessary.

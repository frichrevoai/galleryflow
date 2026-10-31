# GalleryFlow

Android gallery application. The next planned build is **v0.18.0**.

## Upload the source

1. Extract `GalleryFlow-v0.18.0-GitHub-Actions.zip` obtained from the previous ChatGPT download.
2. Open this repository, then choose **Add file → Upload files**.
3. Upload the *contents* of the extracted folder to this repository (especially `app/`, `build.gradle`, and `settings.gradle`). Do not upload just the ZIP.
4. Commit the files to `main`.
5. Go to **Actions → Build GalleryFlow APK**, open the successful run and download the APK artifact.

The GitHub Actions workflow lives at `.github/workflows/build-apk.yml` and will run when Android source/build files are pushed.

**Signing:** The default debug APK may not update an existing installation signed with a different certificate. Keep your installed app and its data until a matching-key release APK is available. Do not commit keystores or passwords.

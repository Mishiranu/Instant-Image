# Instant Image

Instant Image is tool that allows you to find image in Google search and attach it in two clicks.

Choose this app in attachment dialog, enter search query and choose the desired image.

# Building Guide

1. Install JDK 7 or higher
2. Install Android SDK, define `ANDROID_HOME` environment variable or set `sdk.dir` in `local.properties`
4. Install Gradle
5. Run `gradle assembleRelease`

The resulting APK file will appear in `build/outputs/apk` directory.

### Build Signed Binary

You can create `keystore.properties` in the source code directory with the following properties:

```properties
store.file=%PATH_TO_KEYSTORE_FILE%
store.password=%KEYSTORE_PASSWORD%
key.alias=%KEY_ALIAS%
key.password=%KEY_PASSWORD%
```

# License

Instant Image is licensed under the [MIT License](LICENSE).

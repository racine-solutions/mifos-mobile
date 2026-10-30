# Publishing to Google Play

## Application ID

The Android application id is `androidPackageNamespace` in `gradle/libs.versions.toml`
(`com.racinesolutions.pay`). It is permanent once the first build is uploaded to Play, so confirm it
before the first upload. The demo flavor and debug builds add `.demo` / `.debug` suffixes.

## Signing

No keystore or password lives in the repository. Create an upload key once and keep it somewhere safe:

```bash
keytool -genkeypair -v -keystore ~/racinepay-upload.keystore -alias racinepay-upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Provide it to Gradle with environment variables (CI):

`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD`

or in `~/.gradle/gradle.properties` (local):

```properties
RELEASE_KEYSTORE_PATH=/home/you/racinepay-upload.keystore
RELEASE_KEYSTORE_PASSWORD=...
RELEASE_KEY_ALIAS=racinepay-upload
RELEASE_KEY_PASSWORD=...
```

If none of these is set, `release` builds are produced unsigned. Enrol the app in Play App Signing when
creating it in the Play Console, so Google holds the final app signing key.

## Build the bundle

```bash
VERSION=1.0.0 VERSION_CODE=1 ./gradlew :cmp-android:bundleProdRelease
```

Upload `cmp-android/build/outputs/bundle/prodRelease/*.aab`. `VERSION_CODE` must increase with every upload.

## Firebase

Analytics/Crashlytics are not wired in for the prod flavor (a stub is used) because the repository no
longer ships Mifos's `google-services.json`. To enable Firebase, create a Firebase project for the
application id and add its `google-services.json` to `cmp-android/` (git-ignored). The Firebase Gradle
plugins are applied automatically when that file exists. Then restore the Firebase module in
`core-base/analytics/src/androidProd` so analytics are actually collected.

## Before submitting

- Production backend URL set as the default (`AppSettings.DEFAULT.baseUrl` and `BaseURL.API_ENDPOINT`).
- Privacy policy URL, Data safety form, financial-features declaration, content rating.
- Test credentials and a reachable test backend in the Play Console "App access" section.
- An account-deletion path (in-app and web) if users can register.

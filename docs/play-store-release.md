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

## SMS auto-read hash (sign-up and password reset codes)

The app reads the 6-digit code from the SMS by itself using Google's SMS Retriever, which needs no
SMS permission. For this to work, the last line of the SMS must carry an **app hash** computed from
the package name and the certificate that signed the installed APK. The bank adds the hash(es) to
the backend setting `self-service-sms-app-hash`; the setting accepts several hashes separated by
commas. If the hash is missing or wrong, nothing breaks: the member just types the code.

### Debug builds

Signed with `~/.android/debug.keystore`. The hashes below are already set on the local backend:

| Package | Hash |
|---|---|
| `com.racinesolutions.pay.demo.debug` | `x/MrSJkY/JS` |
| `com.racinesolutions.pay.debug` | `3esZTVbW2qB` |

### Release build

The hash must come from the key that signs the APK **on the member's phone**:

- **With Play App Signing (the normal case):** Google re-signs the app with its own *app signing
  key*, so use that certificate, **not** your upload keystore. In Play Console open
  **Setup → App integrity → App signing key certificate** and copy the **SHA-256** fingerprint, or
  download the certificate (`.der`) from the same page.
- **Without Play App Signing (APK handed out directly):** use your own release keystore.

Compute the hash from the certificate (Google's documented method):

```bash
# From a keystore (replace the package, alias and keystore path):
keytool -exportcert -alias <alias> -keystore <keystore.jks> \
  | xxd -p | tr -d "[:space:]" \
  | xargs printf 'com.racinesolutions.pay %s' \
  | shasum -a 256 | tr -d "[:space:]-" | xxd -r -p | base64 | cut -c1-11

# From the .der certificate downloaded from Play Console:
xxd -p -c 1000 app-signing-cert.der \
  | xargs printf 'com.racinesolutions.pay %s' \
  | shasum -a 256 | tr -d "[:space:]-" | xxd -r -p | base64 | cut -c1-11
```

The package name in the command must be the installed application id (`com.racinesolutions.pay`
for the production flavor). The output is 11 characters, for example `3esZTVbW2qB`.

Give the bank the hash for the **app signing key** (and, if internal test builds are signed with a
different key, one hash per signing key). Re-check it whenever the signing key changes.

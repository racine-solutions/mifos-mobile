# RPAY-136 — Sign-up, forgot password and login: mobile app requirements

Backend: fineract branch `RPAY-136` (latest `73393aa74`), live-tested against localhost.
Mobile: implement on this repo's `RPAY-136` branch.

## Goals (approved by the user)

- A member signs up with a **6-digit code** instead of the long UUID token.
- The code goes by **SMS by default**; if the SMS fails it goes by **email**.
- On Android the app **reads the code from the SMS by itself**.
- If the member leaves the code screen or closes the app, they can **resume**. Sign-up no longer fails just because the code screen was lost.
- The member's **email becomes their username**. Members can also **log in with their phone number**.
- Forgot password works the same way (6-digit code) and also **unlocks** an account locked by wrong passwords.
- Secure by design:
  - Codes go only to the phone or email on the SACCO's records.
  - A code works only with its own request reference.
  - No screen reveals whether an account exists.

## Common rules

- All endpoints are `POST`, JSON, public (no login), under `/v1/self/...`. They use the same base URL and `Fineract-Platform-TenantId` header as login.
- Errors use the normal Fineract body: `{httpStatusCode, userMessageGlobalisationCode, defaultUserMessage, errors, ...extra}`.
  - Show `defaultUserMessage` to the member.
  - Branch on `userMessageGlobalisationCode`.
- Never store, log or send to analytics a code or password. Keep the typed password in memory only until the automatic login after sign-up.
- If the bank turns the app off, every endpoint returns 503 `error.msg.selfservice.disabled`. Reuse the existing "app unavailable" screen.

## 1. Sign-up

### Screens (from the approved mockup)

| Screen | Content |
|---|---|
| **A. Sign up** | Sacco/MFI code, customer account no., first name, last name, phone, email, password, repeat password. Buttons: **Send my code**, link **I already have a code**. Note under email: "Your email will be your username." |
| **B. Verify by SMS** | "We sent a 6-digit code by SMS to `{destination}`". 6 code boxes. "Code read from SMS" chip when auto-filled. **Verify**. "Resend code in 0:42" countdown, then **Resend code**. **Send by email instead**. "Wrong number? Call `{support}`". |
| **C. Email fallback** | Shown when `smsFailed=true` or `channel="EMAIL"`: "We couldn't send an SMS, so we emailed your code to `{destination}`". 6 boxes, **Verify**, **Try SMS again**, "Code valid for 10 minutes". |
| **D. Wrong / expired** | Error under the boxes: "That code is wrong. 2 tries left." or the expired message with **Send a new code**. |

### Endpoints

**`POST /v1/self/signup/start`**
```json
{"accountNumber":"000000005","firstName":"Joseph","lastName":"Kony","mobileNumber":"0785154434",
 "email":"member@example.com","password":"…","repeatPassword":"…","channel":"SMS"}
```
`channel` is optional ("SMS" default, or "EMAIL"). Response 200:
```json
{"registrationId":"c797c3ed-…","channel":"SMS","destination":"0785•••434","smsFailed":false,
 "codeLength":6,"codeExpiresInSeconds":600,"resendAfterSeconds":60}
```
- **Save `registrationId` persistently right away** (DataStore/settings, per tenant). This is what makes resume possible.
- Show B, or C when `channel=EMAIL` or `smsFailed=true`. Start a 10-minute expiry timer and a 60-second resend countdown.
- Starting again replaces any earlier sign-up for the same member. Always overwrite the stored `registrationId`.

**`POST /v1/self/signup/resend`** — `{"registrationId":"…","channel":"SMS"|"EMAIL"}`, same response as start.
- **Send by email instead** sends `channel:"EMAIL"`; **Try SMS again** sends `channel:"SMS"`.
- The previous code stops working, so clear the boxes.
- Show whatever `channel` and `destination` come back. The server may fall back to the other channel.

**`POST /v1/self/signup/resume`** — `{"registrationId":"…"}`
```json
{"registrationId":"…","channel":"SMS","destination":"0785•••434","codeLength":6,"codeActive":true,
 "codeExpiresInSeconds":412,"attemptsLeft":5,"resendAfterSeconds":0}
```
- Call it when the app starts or the sign-up screen opens while a `registrationId` is stored, and from **I already have a code**.
- Restore B or C with the remaining countdowns.
- If `codeActive=false`, show the boxes disabled with **Send a new code**.

**`POST /v1/self/signup/verify`** — `{"registrationId":"…","code":"482913"}` → `{"userId":3,"username":"member@example.com","clientId":5}`
- On success:
  1. Clear the stored `registrationId`.
  2. Log in immediately with the returned `username` and the in-memory password.
  3. Continue to the existing set-PIN flow.

### Sign-up errors

| HTTP | `userMessageGlobalisationCode` | App behaviour |
|---|---|---|
| 400 | `error.msg.signup.details.mismatch` | Stay on A and show the message. It is deliberately generic and does not say which field is wrong. |
| 400 | `error.msg.signup.client.not.active` | Show the message on A. |
| 400 | `error.msg.invalid.request` (password rules etc.) | Show the message under the password field. |
| 409 | `error.msg.signup.already.registered` | Dialog with **Log in** and **Forgot password** buttons. |
| 409 | `error.msg.signup.username.taken` | Error on the email field. |
| 400 | `error.msg.code.invalid` + `attemptsLeft` | Screen D: "That code is wrong. N tries left." |
| 410 | `error.msg.code.expired` | Clear the boxes and show **Send a new code** (resend). |
| 404 | `error.msg.signup.not.found` | The sign-up expired (24 h) or was replaced. Clear `registrationId` and go back to A with the message. |
| 429 | `error.msg.code.resend.too.soon` / `error.msg.code.send.limit` + `retryAfterSeconds` | Disable resend and show a countdown. |
| 502 | `error.msg.code.delivery.failed` + `registrationId`, `supportPhoneNumbers` | Store `registrationId`. Show the message, a **Try again** button (resend) and call buttons (reuse the existing call-button component). |

Notes:
- Codes are sent only to the phone and email on the client record, never to what the member typed. The member's email still becomes the username.
- Phone numbers match in any format (`0785…`, `256785…`, `+256 785 …`).
- Names are matched ignoring case.

## 2. SMS auto-read (Android)

- Use the SMS Retriever API: `com.google.android.gms:play-services-auth-api-phone`.
  - It needs **no SMS permission**.
  - Call `SmsRetriever.getClient(context).startSmsRetriever()` right before signup `start`/`resend` and password-reset `start`/`resend`.
  - Listen for `SmsRetriever.SMS_RETRIEVED_ACTION`. Take the first `\d{6}` in the message and fill the boxes.
  - **Do not auto-submit.** Show "Code read from SMS" and let the member tap **Verify**.
  - On timeout (5 min) or any error, fall back silently to manual entry.
- The SMS ends with a last line holding the app hash(es), space separated, e.g.:
  ```
  Your mobile app sign-up code is 482913. It expires in 10 minutes. Never share it with anyone, including staff.
  x/MrSJkY/JS 3esZTVbW2qB
  ```
  The forgot-password SMS starts "Your password reset code is …".
- Debug hashes (default `~/.android/debug.keystore`), already set in the local backend setting `self-service-sms-app-hash`:
  - `com.racinesolutions.pay.demo.debug` → `x/MrSJkY/JS`
  - `com.racinesolutions.pay.debug` → `3esZTVbW2qB`
- **Release:** the hash must come from the key that signs the installed APK. With Play App Signing that is Google's *app signing key* (Play Console → App integrity → App signing key certificate), not the upload keystore. Please document how to compute it in `docs/play-store-release.md` (keytool method, or a debug-only `AppSignatureHelper` log), so the bank can add it to the setting. The setting accepts several hashes, comma separated.
- Other platforms: manual entry (use the one-time-code autofill hint where available).

## 3. Forgot password

### Screens

1. **Forgot password:** one field, "Email, username or phone", and **Send code**. This replaces the current phone + email form.
2. **Enter code and new password:** 6 boxes (with SMS auto-read), new password, repeat password, **Reset password**, **Resend code** (60-second countdown).

### Endpoints

**`POST /v1/self/password-reset/start`** — `{"identifier":"member@example.com"}`
```json
{"resetId":"b4e9…","codeLength":6,"codeExpiresInSeconds":600,"resendAfterSeconds":60,
 "message":"If an account matches, we sent a 6-digit code to its registered phone or email. If nothing arrives in a few minutes, call +256 774 067 967."}
```
- The answer is **always the same**, whether or not an account exists, a code was sent, or a limit was hit. This is deliberate, so the screen cannot be used to discover accounts.
- Show `message` and go to screen 2. There is no destination in this response.

**`POST /v1/self/password-reset/resend`** — `{"resetId":"…"}`, same response shape.

**`POST /v1/self/password-reset/verify`** — `{"resetId":"…","code":"783334","password":"…","repeatPassword":"…"}` → `{"username":"member@example.com"}`
- On success:
  1. Go to login with the username prefilled.
  2. Show "Password changed. You can log in now."
  3. All earlier sessions are revoked, so clear any stored tokens or passcode session.
- A successful reset also **unlocks** an account locked by wrong passwords.

### Errors

| HTTP | Code | App behaviour |
|---|---|---|
| 400 | `error.msg.code.invalid` | "That code is wrong or has expired…" There is **no** `attemptsLeft` by design. Offer **Resend code**. |
| 400 | `error.msg.invalid.request` / password messages | Show under the password fields. |
| 400 | `error.msg.password.reset.not.allowed` | Show the message (it includes the support instruction). |

## 4. Login

- Relabel the username field **"Email or phone"**. The server also still accepts an old-style username.
- `POST /v1/self/authentication` is unchanged otherwise. Store the **`username` from the response**, which is the canonical username, not what was typed.
- New refusal: 401 `error.msg.login.identifier.ambiguous` ("This phone number is linked to more than one account. Log in with your username instead."). Show it as a normal login error.
- On the **locked** refusal (401 `error.msg.selfservice.user.locked`), add a **Reset my password** button next to the call buttons. It opens Forgot password with the typed identifier prefilled.

## 5. Shared phone or email: blocked with a reference

If a member's phone number (any format) or email is also on another client's record, or used by another app user, the app is **blocked** for that member until SACCO staff correct the records. This prevents codes or data reaching the wrong member.

- **Where it applies:** signup start, signup verify, login, token refresh, forgot-password verify, and every request made with an open session. For an open session, the server ends the session and further calls get 401; the clear message appears on the next login.
- **Response:** HTTP **409**, `userMessageGlobalisationCode` = **`error.msg.selfservice.contact.conflict`**, with extra fields:
  ```json
  {"defaultUserMessage":"Your phone number or email is also on another member's record, so the mobile app is blocked to protect your account. Please contact your SACCO and give them reference DUP-000000005. Call +256 774 067 967.",
   "reference":"DUP-000000005","supportPhoneNumbers":["+256 774 067 967"]}
  ```
- **App behaviour:** show a dedicated dialog or screen, not a toast:
  - Title "Mobile app blocked for your safety".
  - The `defaultUserMessage`.
  - The **reference** in large, selectable text, with a **Copy reference** button.
  - Call buttons for `supportPhoneNumbers` (reuse the call-button component).
  - No retry button. Do not log the member in, and clear any stored tokens.
- **Forgot password:** start/resend stay neutral (no code is sent to a blocked member). If verify returns this 409, show the same blocked screen.

## 6. Old endpoints

`/self/registration`, `/self/registration/user` and `/self/password/request|renew` still work for older app versions. The new app should use only the endpoints above.

## 7. Testing

- Local backend: `https://localhost:8443` (emulator: `adb reverse tcp:8443 tcp:8443`).
- Test account: `kony.test@example.invalid` (client 5, phone 0785154434). Ask the backend session for the password.
- Codes go by **real SMS to the user's phone**, so agree on live tests with the user.
- When the app is ready, tell the backend session. It will run the end-to-end test on the emulator: sign-up, auto-read, resume after closing the app, wrong code, email fallback, forgot password from the locked screen, and login by email and phone.

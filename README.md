# OctoHub — GitHub for Android (unofficial WebView client)

GitHub-এর আসল ওয়েবসাইট, লেআউট ও কনটেন্ট Android অ্যাপের ভেতর। আলাদা GitHub API client বা নকল UI নয়।

**Architecture:** a native Java Android Activity hosts the live `https://github.com/` site in Android System WebView. The toolbar, navigation, file chooser and download integration are native; the GitHub UI itself is web content, not native Android widgets. No bundled website, backend, API token or OAuth app registration is needed. Not affiliated with or endorsed by GitHub, Inc.

## Features

- GitHub's own responsive interface and sign-in flow, including its normal password/2FA screens.
- Browse repositories, code, issues, pull requests, notifications, settings and other pages available to your account on GitHub's mobile website.
- Native back / forward / home / reload controls; Android back-button history.
- Native file picker for supported GitHub uploads, with no broad storage permission.
- Confirmed HTTPS downloads through Android Download Manager, including correctly scoped session cookies for private downloads.
- Open external links in a browser, share current page, switch mobile/desktop user agent.
- Session cookies persist in WebView's private app storage. An explicit **Clear session & local data** action clears website cookies/storage/cache. Android backups are disabled.
- Network error view, progress bar, state restoration and keyboard/system-bar insets.

## Requirements and limitations

- **Android 10+ (API 29)** and an up-to-date Android System WebView / Chrome. Internet is required; this is not an offline GitHub clone.
- GitHub controls all web functionality. The app cannot guarantee every github.com feature works in an embedded browser.
- Passkeys, third-party identity providers/SSO, security-key flows, some pop-ups, camera capture and `blob:` downloads may need **Open in browser**. Browser and WebView sessions are separate; signing in externally does not automatically sign you into the app.
- File uploads use the system document picker; direct camera capture is not implemented.
- This is not a background push-notification service, Git command-line client, or Android port of GitHub's official app. GitHub's notification page remains accessible.
- Download redirects are handled by Android Download Manager; unusual authentication/redirect flows may require the external browser.
- No JavaScript bridge, password interception, injected website styling or native access is exposed to GitHub pages. JavaScript and DOM storage are enabled because GitHub requires them. Plain HTTP, mixed content, third-party cookies, arbitrary URI intents and WebView file access are blocked; TLS errors are never bypassed.
- Only github.com, www.github.com and gist.github.com navigate inside the app. Other HTTPS destinations open externally. These restrictions apply to top-level navigation, not GitHub's necessary CDN resources.

## APK বানানো / Download

The **Build Android APK** GitHub Actions workflow runs tests, lint and assembles a debug-signed APK on pushes to `main` / `arena/**`, pull requests, or manual dispatch.

1. Open this repository on GitHub → **Actions → Build Android APK**.
2. Select a successful run → **Artifacts → OctoHub-Android-APK**.
3. Extract the ZIP and install **OctoHub-debug.apk** on your phone. Android may ask you to allow installation from that source.

The debug build is installable for testing, not a Play Store production release. CI runners use ephemeral debug signing keys, so a later APK may need the previous test app uninstalled first (clears its session). A production release requires your own stable, privately held signing key. Never commit keystores or passwords.

## Build locally

Install **JDK 17** and **Android SDK platform 35 / build-tools 35.0.0**. Set `ANDROID_HOME` or an untracked `local.properties` with `sdk.dir=...`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Alternatively open the project in a compatible Android Studio (Meerkat or newer). No Node.js or npm dependencies are used.

## Manual device checklist

1. Launch on Android 10 and Android 15+ with current WebView; check status/navigation bars, keyboard, portrait/landscape and Back.
2. Sign in through GitHub using a test account, complete 2FA, relaunch and check session persistence. Do not share credentials in chat.
3. Open public/private repositories, navigate files and issues, and test an account-authorized comment/edit.
4. Upload an attachment via the file picker; cancel the picker and retry.
5. Download public release and private repository files; verify their content in Downloads.
6. Follow external links, share pages, switch desktop mode and test target-blank links.
7. Disable networking, reload, re-enable networking and retry.
8. Clear the local session; verify sign-out and that no repository/account was modified.
9. Test a passkey/SSO account separately and use the external browser where the provider disallows WebView.

Automated unit tests cover URL trust boundaries, spoofed domains, unsafe schemes, invalid input and download host restrictions. Live GitHub login, uploads, downloads and device behavior require manual testing; unit tests do not establish their compatibility.

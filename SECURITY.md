# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 1.0.x | Yes |

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting:
**Security → Report a vulnerability** on
[github.com/vijaykumarbs/matrix-nothing-iconpack](https://github.com/vijaykumarbs/matrix-nothing-iconpack/security/advisories/new).

Do not open a public issue for a security problem. Reports are acknowledged within
72 hours. This is a small, offline app, so the realistic scope is limited to build,
signing and release-process issues.

## Security model

Matrix is deliberately boring:

- **No permissions.** The manifest contains zero `uses-permission` entries. There is
  no Internet, storage, location or package-list permission to abuse.
- **No network code.** The app never opens a socket. Links open in the user's browser.
- **No background components.** No services, broadcast receivers, content providers,
  alarms or jobs. The process lives only while a screen is open.
- **No third-party dependencies.** Java + Android framework only; no AndroidX, no
  Kotlin runtime, no analytics SDK, no WebView.
- **Narrow package visibility.** A single `<queries>` entry for `MAIN`/`LAUNCHER`
  activities lets the icon-request screen list the user's launchable apps locally.
  Nothing is uploaded; the list is only copied to the clipboard when the user taps
  *Copy list*.
- **No backups.** `allowBackup="false"`.
- **Cleartext blocked.** `usesCleartextTraffic="false"` on top of no network permission.

The launcher-facing `ThemeActivity` is exported because the icon-pack protocol
requires it. It accepts no data, performs no privileged action and simply opens the
main screen. `MainActivity` is the normal launcher entry point.

## Release integrity

- Release APKs are signed with a **4096-bit RSA** key using APK Signature Scheme
  **v2 and v3**, with a 10,000-day validity.
- The private key exists only in the maintainer's local keystore and in encrypted
  GitHub Actions secrets. It is git-ignored and never uploaded as an artifact.
- Every release publishes `SHA256SUMS.txt`. Verify with:

  ```sh
  shasum -a 256 -c SHA256SUMS.txt
  ```

- CI verifies signatures and fails the release if any APK declares a permission.
- Builds are reproducible from source with JDK 21, Android SDK 36 and Gradle 8.11.1.

## Release signing fingerprint

```
SHA-256: 60:77:D7:5E:B5:A7:18:8E:99:BB:9C:61:BC:F4:A7:49:3B:C2:36:AA:2D:30:57:54:8C:A5:2C:39:57:5E:DB:EB
```

Compare against an installed APK with:

```sh
apksigner verify --print-certs matrix-phosphor-glass.apk
```

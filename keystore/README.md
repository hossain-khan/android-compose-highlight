# Android Signing Keystore

This directory contains the debug keystore used to sign the sample app release builds in CI.

## Debug Keystore

The `debug.keystore` file is the standard Android debug keystore with the following credentials:

- Store Password: `android`
- Key Alias: `androiddebugkey`
- Key Password: `android`

This is the same debug keystore as the one used in
[android-compose-app-template](https://github.com/hossain-khan/android-compose-app-template/tree/main/keystore).

**Note:** Using a debug keystore is acceptable here because this is a sample app, not a
production release. Never use a debug keystore for production app distribution.

See [Android debug signing](https://developer.android.com/studio/publish/app-signing#debug-mode)
for more details.

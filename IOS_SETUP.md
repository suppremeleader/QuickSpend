# QuickSpend iOS foundation

Android V1 is accepted at build #52 (version 1.0.0.52 / code 1052). iOS work starts from that ledger/UI baseline on the `ios/v1-foundation` branch.

## Current checkpoint

The iOS Foundation Simulator workflow installs the Capacitor 6 iOS runtime, stages the shared app, generates a native Xcode project and opaque branded icons, and builds an **unsigned iOS Simulator `.app`** on a GitHub macOS runner. Its output is not an IPA, cannot be installed on an iPhone, and is not a TestFlight release.

Shared functionality: manual entry, user-pasted bank SMS, import/JSON backup, budgets, dated EMI, lending balances, Insights, English/Hindi and dark/light preference. Android SMS, floating overlay, Android updater and unavailable native account actions are hidden on iOS. Native Share remains enabled. Google/Firebase cloud sign-in is not implemented on iOS at this checkpoint.

## Reproduce on a Mac

```sh
npm install
node scripts/ios/stage-web.mjs
npx cap add ios
python3 scripts/ios/configure.py
swift scripts/ios/icons.swift assets/quickspend-icon.png ios/App/App/Assets.xcassets/AppIcon.appiconset
npx cap sync ios
npx cap open ios
```

In Xcode open `ios/App/App.xcworkspace` and choose an iPhone Simulator. Generated `ios`, `www`, Pods and DerivedData are disposable outputs at this foundation checkpoint; changes to native setup belong in the preparation scripts until the native project is adopted for the release phase.

## Before a signed TestFlight build

1. Identify the Apple Developer Program team that will own QuickSpend. Confirm the bundle ID (`com.quickspend.app` proposed; do not overwrite any existing app). Create the matching App Store Connect app record.
2. Configure signing/provisioning and upload authentication through Xcode/secure CI credentials. Do not put passwords, signing private keys or API private keys in this repository or chat.
3. Register an iOS app in the existing Firebase project. Supply the iOS client configuration (`GoogleService-Info.plist`) through the project setup route, then implement native Google sign-in and the existing revision-checked Firestore backup contract. Do not confuse this file with Android `google-services.json` or a service-account key.
4. Plan Sign in with Apple alongside Google login, and implement account deletion and account linking/migration without losing existing Google-owned backups. Review App Review Guideline 4.8; do not assume Google-only login is release-ready.
5. Audit privacy labels, privacy manifests and required-reason API use for the actual included SDKs; confirm encryption/export declarations and the current App Store Xcode/SDK requirements. A successful simulator compile does not establish App Store eligibility.
6. Validate real iPhone safe areas, date pickers, keyboards, file import/share backup, restore, local persistence/reinstall behavior, authentication and both themes/languages. Decide iPad support explicitly.
7. Create a signed archive/IPA, upload to App Store Connect, and invite internal TestFlight testers. External testing may require Apple review. Publish publicly only after beta acceptance and an explicit rollout request.

## Capture differences

iOS does not expose the Android SMS receiver and system-overlay service architecture. The foundation uses manual entry and user-pasted messages/import. A later user-initiated Share extension or Shortcuts flow can be investigated. Apple's SMS filtering extension has its own sandbox restrictions and cannot be treated as unrestricted access to the ledger or message inbox.

## Sources

- Capacitor 6 iOS/Xcode setup: https://capacitorjs.com/docs/v6/ios
- Apple distribution preparation: https://developer.apple.com/documentation/xcode/preparing-your-app-for-distribution
- TestFlight workflow: https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/
- App Review Guideline 4.8: https://developer.apple.com/app-store/review/guidelines/
- SMS filtering constraints: https://developer.apple.com/documentation/identitylookup/sms-and-mms-message-filtering

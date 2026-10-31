# Activate Google sign-in and cloud backup

The layout fixes work without cloud configuration. Google sign-in remains unavailable until this setup is complete. No Firebase project is configured in this repository yet.

1. Open https://console.firebase.google.com/ and create/select the QuickSpend project.
2. Add an Android app with package **com.quickspend.app**.
3. In Play Console, open App integrity / App signing and copy the **app signing certificate** SHA-1 and SHA-256 fingerprints. Add these in Firebase Project settings → Your apps. Play-installed builds use Google's app signing certificate, which differs from the upload key.
4. In Firebase Authentication, enable Google under Sign-in method and choose the project support email.
5. Create a Cloud Firestore database in **Production mode**. Choose the database location in the console. Paste `firebase/firestore.rules` into its Rules tab and publish those rules. They limit each backup to its signed-in owner.
6. Download the updated Android `google-services.json` after enabling Google authentication. Add that **client configuration** to this repository at `config/google-services.json`. Alternatively, attach it in this conversation for integration. Do not send a service-account JSON, private key, password, or OAuth client secret.
7. Rebuild and upload the new AAB to Internal testing. Test actual Google login and cloud restore using the Play-installed app.

## What this version saves

Transactions, pending parsed payment records, budgets, accounts, lending balances, recurring records, category memories and custom categories. Raw SMS bodies, SMS senders, Google tokens, account-session preferences and signing keys are excluded.

This is automatic cloud backup and restore, not realtime multi-device editing. The app stores one current backup per Google account. A new device checks for an existing backup before uploading. Restore merges records with the phone's current records; existing local budget plans and category choices win when the existing import logic finds the same record. Selecting “Use this phone’s records” explicitly replaces the previous backup after confirmation.

Revision checks prevent silent overwrites if another device has changed the backup. On a save/network conflict automatic saving pauses and the UI reports it; use Restore or Save to cloud now to resolve/retry. The latest backup survives a local Reset; Reset pauses automatic cloud saving so an empty phone cannot overwrite that backup. Signing out keeps local records on the phone.

Payloads are capped at 800,000 UTF-8 bytes. Larger histories must use the existing file-backup option. Data is protected by Firebase authentication and owner-only Firestore rules; this implementation does not provide end-to-end encryption.

## Validation still required after configuration

Google account selection/cancellation, owner-only access with two test accounts, clean-install restore, offline save/retry, change-of-account behaviour, simultaneous-device conflict handling, and sign-out. No live Google/Firebase test has been possible without the project's configuration and deployed rules.

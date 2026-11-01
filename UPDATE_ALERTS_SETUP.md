# Update alerts — prepared for the next app build

This feature is prepared on a separate branch. It has not been merged, built, installed, enabled on the server, or used to send notifications.

## Behaviour

- On opening QuickSpend, eligible Play-installed users can see an optional Update now / Later banner. Automatic prompts respect a daily per-version cooldown and stay away from onboarding and open entry sheets.
- Google Play owns update availability and the download/installation consent flow. Flexible updates download while the app remains usable; Restart and update is offered after the download completes.
- Settings → App updates provides a manual check, Google Play link, and update-alert preference.
- Firebase Cloud Messaging delivers discrete update alerts while the app is backgrounded or normally closed. No additional persistent foreground service is used.
- Alerts contain only message type, package name, and version code. The client rejects other packages, installed/older versions, duplicate alerts, and locally disabled notifications. No financial records, SMS, Google sign-in tokens, or device registration tokens are exposed to JavaScript or the release watcher.
- The new Firebase default-app resources support messaging while the existing named Firebase authentication app is retained, preserving existing sign-in sessions.
- This version intentionally targets **internal testing** only. Before a public release, separate client topics and watcher configuration must be introduced for each release channel.

## Important limits

The updated client must be installed and opened at least once to subscribe. Build #36 and older cannot gain this feature remotely. Google Play must recognise the installed app/account as eligible for an update. Test account ownership and signing must match the Play release.

Notifications require Android notification permission, enabled App updates notifications, network delivery and Google Play services. Android force-stop, device restrictions and Doze can delay/block delivery. Notification delivery is not instantaneous or guaranteed. A notification tap opens the fixed QuickSpend Google Play listing; clients never follow arbitrary server-provided URLs.

## Release detection

`.github/workflows/release-alerts.yml` polls the internal Play track every 15 minutes, subject to GitHub scheduling delays. It reads an uncommitted Play edit snapshot and always deletes that snapshot; it never commits an edit, uploads a bundle or changes a release.

Only `completed` releases are considered. Draft, halted and staged/in-progress releases are excluded. A new version must be observed for at least 10 minutes before announcement, allowing time for Play propagation. The first run records the existing release without broadcasting it. Later higher versions can be announced.

State is recorded on a dedicated `release-notification-state` branch at `release-state/internal.json`, never on main. Writes there do not match the Android build workflow's `push: branches: [main]` trigger. Sends are at-least-once: if a job fails after FCM accepts a message but before state is recorded, a later run may retry; client version deduplication prevents repeat notifications on the same device.

## Required authorization — not configured yet

Use a dedicated Google Cloud service account in the same Firebase project. **Do not create or upload a service-account private key.** The workflow uses keyless Workload Identity Federation (WIF).

1. Enable the Firebase Cloud Messaging HTTP v1 API and Google Play Android Developer API for the project.
2. Grant that service account the FCM sending permission `cloudmessaging.messages.create` (a narrowly scoped custom role, or `roles/firebasecloudmessaging.admin` if appropriate). It does not need access to users' Firestore financial records.
3. In Play Console → Users and permissions, grant this service account read access to the QuickSpend app for inspecting release tracks. Validate that the uncommitted edit read is authorised; do not grant Owner/Admin just to bypass a permission error.
4. Configure WIF through that service account. Restrict the identity to this repository's immutable repository/owner IDs, `refs/heads/main`, and this exact workflow path. Grant the matching principal `roles/iam.workloadIdentityUser` on the dedicated service account.
5. Add repository variables `UPDATE_ALERTS_WIF_PROVIDER` and `UPDATE_ALERTS_SERVICE_ACCOUNT`. Leave `UPDATE_ALERTS_ENABLED` unset until authorisation and a device test are complete.
6. After the client feature is included in a future app release and installed, set `UPDATE_ALERTS_ENABLED=true` and run the workflow once to initialise the current-version baseline. Then a later published release can trigger the first update alert.

Only the service identity and WIF provider resource names belong in these variables. The Firebase project's existing Android client configuration remains at `config/google-services.json`. The release watcher obtains temporary Google credentials through WIF and uses the repository-scoped GitHub Actions token for its dedicated state branch.

## Verification before activation

Local JS and Python checks cover optional prompt behaviour, blocked UI states, cancellation, restart, opt-out, release filtering, baseline behaviour, propagation delay, failed sends and deduplication. Java source syntax is checked locally; pure Java policy tests are wired into the next normal Android build. No new Android build or live push was started for this preparation.

Before enabling the watcher, test the client from Google Play with a higher version on the same eligible internal track. Verify notification delivery with the app closed, notification permissions off, update alerts off, the same notification delivered twice, and an already-updated device. Validate Play access and FCM authorization using the dedicated identity. Backend setup and end-to-end Play/FCM behaviour remain unverified.

## Authorization check without delivery

After this workflow is merged into main, open GitHub Actions → Notify published internal updates → Run workflow. Keep `validate_only` checked (the default) and select main. This mode works while `UPDATE_ALERTS_ENABLED` remains unset. It reads and deletes an uncommitted Play edit snapshot and asks FCM to validate a message with `validate_only: true`. It never delivers a notification, creates notification state, or changes the baseline. It checks API authorization, not phone delivery. A 403 from Play requires investigating the app permission; do not grant account-wide release access.

The WIF provider intentionally trusts only this exact workflow on main. A feature-branch run is blocked. Merging the app feature into main also triggers the existing Android build; obtain approval for that build before merging. The Google setup reported by the operator must still pass the authorization check; no live authorization result is claimed here.

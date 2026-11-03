# Build 69 — version code 1069

Includes the current merged PR #13 and PR #14 changes:
- One EMI heading in the app's new-entry form, with active existing plans below it.
- Text and picture notes, including picture preview, removal, saved-entry indicators and backup support.
- Microphone recording and microphone permissions remain removed as requested. Existing saved voice attachments are preserved; future recording is in Ideas / QUI-28 alongside GenZ mode QUI-12.

The build now checks the packaged HTML for PR #13/#14 controls, and browser checks verify the rendered EMI heading, an active plan and picture-note flow. The signed artifact includes build-evidence.json with the exact version, commit and these release checks.

To test on the phone: upload the new quickspend-build-69.aab to the Play testing track, publish the test release, then install the update from the tester link. Downloading the AAB alone does not install it. In QuickSpend Settings, the footer must show Android build 1069. In the app, tap + to open New entry: Add picture is under New note, and existing active plans appear under the single EMI heading. These note controls are in the app's entry form; the Android floating popup has its own capture form.

PR #12 remains separate and unmerged. No Play rollout is performed by the GitHub build.

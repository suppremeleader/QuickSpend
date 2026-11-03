# PR #12 integrated with the latest main

- Android system Back/button and completed Back gestures now use the native AndroidX dispatcher and the app's own route handler. Sheets close before history/secondary sections, and Home remains the exit point. Predictive Back is enabled in the packaged manifest.
- Restored transaction initials in history, with a small immutable message/person badge for automatic/manual entry source. Picture and legacy voice attachment indicators remain visible.
- New-entry EMI section keeps PR #13's single heading and active-plan list, with PR #12's Add new EMI action. Saving or cancelling a plan returns to the unfinished entry; amount, merchant/account, note, picture and captured source are preserved. Saving a plan does not automatically save the payment.
- PR #14 picture notes and the explicit removal of microphone recording/permissions are retained. Future recording stays in QUI-28 / Ideas alongside GenZ mode QUI-12.
- Removed the temporary PR-branch build trigger; builds run from main and retain uniquely named quickspend-build-N.aab files and build-evidence.json.

Validation: 19 JavaScript suites and 11 Python tests pass locally. CI tests the same JavaScript Back function invoked by Android, including returning from a new EMI form while retaining a picture draft. Signed-bundle checks verify the native handler, predictive-Back manifest, PR #13/#14 UI and absent microphone permissions.

Phone acceptance: test system Back/button/swipe from editor, history, settings and other tabs; verify Home is the exit point. Check small source badges. Add/cancel an EMI from a captured payment with a picture and confirm the draft is retained. Upload the new AAB to the testing track and install the update; Settings must show the new Android build code. Physical Android Back gestures remain a phone acceptance check.

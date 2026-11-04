# PR #16 — EMI controls when editing expenses

Base: Build 70, commit de822d4df4d617799c8eba764b54583668713de4. The original PR was one commit ahead of that exact base; no Build 70 changes were missing.

- Expense editors now show the single EMI heading, active existing plans and Add new EMI, including expenses opened from saved Review.
- Selecting, changing or deselecting an EMI persists when the existing transaction is saved. Switching to an income/debt type clears EMI linkage.
- Linking a plan while editing preserves the original amount, merchant, account, timestamp, captured source, note and picture. It updates the EMI association/category without creating another transaction or plan. New blank manual entries still use their existing plan-prefill behavior.
- A failed save restores the original saved record and leaves the draft available for retry.
- Build 70's native Android Back routing, small source badges, Add new EMI draft-return flow, PR #13 single heading, PR #14 picture notes, floater capture/recovery/appearance sound, themes/languages and microphone removal are retained.

Validation: 19 JavaScript suites and 11 Python tests pass locally. CI additionally checks linking/unlinking in the real browser editor, native policies, Android compilation/signing and packaged HTML equality with the reviewed source. Physical phone checks remain pending.

Phone acceptance: open an existing expense from history and saved Review; select/deselect an EMI and save, then reopen. Confirm unchanged amount/account/source and picture, one transaction and no duplicate plan. Confirm system Back and the existing Add new EMI return flow. Upload the new named AAB to the Play testing track, install the update and confirm its Android build code in Settings.

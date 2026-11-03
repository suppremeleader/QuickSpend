# PR #11 — Back navigation and EMI payment capture

- System Back/swipe closes transaction sheets and settings before leaving the current screen. From history or a secondary tab, Back returns to Home. UI close actions consume their own history step without closing the parent screen or leaving redundant exit steps.
- Transaction history shows a message icon for automatically captured records and a person icon for manual records. The source indicator is derived from original capture metadata and cannot be changed in the editor.
- New expense entries show active existing EMI plans. Selecting a plan links the payment without creating another plan. Blank manual amounts are prefilled; captured amounts and merchant/account information are preserved.
- Recorded EMI payments remain actual expenses in balances, budgets, history and Insights. Planned totals retain their existing treatment. Plan-name metadata survives backups.
- Added Hindi labels and regression coverage for asynchronous UI/system Back and EMI accounting. Existing EMI calendar scrolling, floater gestures and appearance sound are retained.

Validation: all 18 JavaScript suites and 10 Python tests pass locally. The Android workflow also runs real Chromium system-Back checks, Java capture/gesture/sound policies, signed release compilation and bundle verification. Physical Android Back/swipe verification remains part of phone acceptance.

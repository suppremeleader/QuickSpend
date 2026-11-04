# Subscriptions, savings and Android widgets

Base: Build 75, de03135dfe6a9183a33c0071c9f5367e145e3330. Existing changes retained; a new version code is required for an installable update.

- Four Home sections: Home, Budget, Savings, EMI. Existing monthly planner moves to Budget. Savings has a separate total and editable balance records.
- Budget -> Subscriptions: search 17 name-only presets or add custom; enter your own price, monthly/quarterly/yearly cycle, renewal date, notes and optional 1/3/7-day reminder.
- Pause/resume, mark cancelled and delete change tracking only. No provider logos, payments, mandates, automatic expenses, balance/budget deductions or assumed paid renewals.
- Collapsed subscription pickers in New entry and native floater link an explicitly saved payment. Native floater also offers existing EMI selection. Payment details stay intact; EMI/subscription links are mutually exclusive.
- Approximate offline local reminders preserve calendar anchors, reschedule after reboot/time changes, deduplicate, and cancel on edits/pause/delete. Notification permissions, channel blocks, Doze, force-stop and shutdown can delay/prevent delivery. Reopen after force-stop. No permanent reminder service notification or exact-alarm permission.
- Three Android widgets: Payment Review (latest pending payment SMS), Monthly Summary (recorded income/spending), Quick Add (new entry). Widgets do not create transactions. Payment details become visible on the home screen when installed.
- Savings/subscriptions/deletion markers are in local/file/cloud backup and reset. Old backups preserve existing planning records; repeated imports deduplicate. Older app builds may drop these new fields on re-export.

Validation: 20 JavaScript suites and 13 Python tests locally, native Java syntax checks. CI adds Chromium flows/layouts, native calendar/gesture tests, Android compilation/signing, merged-manifest widget/reminder checks and exact packaged source verification. Physical widget/reminder acceptance remains pending. iOS widgets/reminders are not included in this Android build.

Phone test:
1. Update from 75; check existing transactions, notes, EMI and budgets.
2. Add/edit/delete savings and confirm totals with no ledger/budget change.
3. Create/edit/pause/resume/cancel a subscription, confirm estimates and no extra expense. Link/unlink from New entry and the floater; check original amount/merchant/account/note and one saved payment.
4. Long-press Android Home -> Widgets -> QuickSpend. Install Payment Review, Monthly Summary and Quick Add. Receive a real supported payment SMS while the app is closed, tap its widget, save, verify it clears. Test cold/warm starts and summary refresh after floater saves.
5. Test reminder permission/channel blocks, reboot, edits/pause/delete and timezone changes. Reminders never confirm a provider payment.
6. Test export/import/cloud restore, repeated imports, stale deleted records, both themes/languages.

Linear QUI-31 and QUI-32; phone acceptance pending.

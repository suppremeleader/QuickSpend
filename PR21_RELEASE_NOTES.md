# PR21 — Separate budgets and Quick Access settings

- Monthly budgets have their own planning balance. Creating or editing a budget never creates a transaction or changes account balances.
- Only expenses dated on or after budget creation, in positively allocated categories, reduce the budget. Historical expenses and unselected categories are excluded. Editing preserves the original boundary.
- Existing explicit budgets retain their amounts. Legacy budgets that followed recorded income freeze that amount once during migration, using their own saved/backup records. Legacy creation dates are unknown and use the budget month start; newly created budgets start now.
- Quick Access settings show four independent pinned controls and the actual unique rail of up to ten categories. Duplicate pins no longer remap controls; short income lists show their real count. Preview controls fit narrow screens.
- Reinforced circular Settings geometry and added Hindi labels for the new budget and rail screens.
- All Build79 features retained, including native category vector badges, stationary hold/slide Quick Access, Later/Discard, savings, subscriptions, widgets and backups.

## Validation

20 JavaScript suites and 14 Python tests pass locally. Added migration, backup/restore, creation boundary, failed-save rollback and duplicate/short-catalog checks. Local Chromium launch is blocked by the execution environment; CI runs actual phone browser flows, geometry, screenshots, budget setup/edit and category isolation, alongside native policies and signed Android/bundle verification.

## Phone acceptance

Create a budget with selected category limits, verify account history/balances stay unchanged, then add one selected and one unselected expense. Only the selected one should reduce the budget. Edit and restore to confirm the same starting boundary. Check dark/light/Hindi settings, four editable pins, ten-tag preview (or actual smaller catalog), and retained floater icons/gesture/save behaviour.

Physical-phone and Play upload acceptance pending.

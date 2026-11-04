# Build 72 — linked EMI instalment completion

Base: Build 71, f329396128f3233482252fd536423c7842756a22. All existing changes retained.

- Linking a saved expense to a dated EMI completes the instalment matching the payment month.
- Outstanding balance and instalment count update immediately; unlinking restores that month.
- Multiple linked payments in one month count once.
- Outstanding totals use exact scheduled instalment amounts, including the rounded final instalment.
- Fully paid plans show Completed rather than future-payment encouragement.

Validation: 19 JavaScript suites and 11 Python tests pass locally. CI additionally checks a real Chromium expense link/unlink flow, native policies, signed Android package and exact packaged source equality. Physical phone acceptance remains pending.

Phone test: link a current-month expense, inspect the green Completed month and reduced outstanding balance, then unlink and confirm Pending and the original balance return. Reopen the app to confirm persistence. Test rounded final instalments and preserve the existing payment details.

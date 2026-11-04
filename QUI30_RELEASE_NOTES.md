# Entry icon polish and collapsed EMI picker — QUI-30

Base: Build 72 / 7e3c4c31448b25ad20ac1dc0dd207502bb87e8b2. Existing features and saved data retained.

- Replace emoji category decorations with consistent vector symbols on coloured circular badges: shopping bag, cutlery, car, receipt and more. Custom categories use a matching tag symbol.
- Use the same category icons in budget/Savings and matching vector account, picture, history-source and Settings controls.
- Remove the EMI symbol from the entry area. EMI starts collapsed on every new/opened editor, with the selected plan name visible in its summary.
- Expand the EMI row to link/unlink an existing plan or create a new one. Returning from the plan form retains the draft and expanded state; a different entry starts collapsed.
- Preserve payment details, notes, pictures, EMI calculation fixes, English/Hindi and dark/light themes. Icon rendering does not alter saved category/account values.

Validation: 19 JavaScript suites and 11 Python tests pass locally. Chromium CI checks keyboard disclosure, real link/unlink and nested form return, dark/light and 320px Hindi layouts, plus no entry emoji/overflow. Existing date-sensitive account test fixtures now mark their current month's recurring record handled, avoiding unrelated ledger creation on the fifth day of the month.

Physical phone check: open New entry, confirm icon clarity and collapsed EMI; expand, link, save and reopen; verify selected plan stays visible when collapsed. Add/cancel a new plan and check draft amount/note/picture. Test both languages/themes.

# Build 79 — PR20 and native category icons

- Quick Access now reveals up to ten category tags while the bubble stays in place. Hold, slide vertically and release on the highlighted tag.
- Four preferred tags remain configurable; remaining slots use distinct ranked categories, with separate income choices.
- Floating New entry and carousel tags now use the same coloured vector badges as the in-app editor, including a generic badge for custom categories.
- Corrected highlighted/saved category alignment, froze choices during a gesture and kept rail rows visible and separated on smaller screens. Horizontal movement alone cannot save.
- Added Hindi labels for the carousel settings and instructions.
- All Build 78 features retained, including per-payment Later, red Discard, budgets, savings, subscriptions, widgets and backup compatibility.

## Validation

20 JavaScript regression suites and 14 Python tests pass locally. Native Java/vector syntax checked. CI must pass Chromium phone flows, native gesture/calendar policies, Android compilation/signing, bundle validation and universal APK generation before delivery.

## Phone acceptance

- Compare floating New entry category badges with the in-app editor in dark/light mode and Hindi.
- Enable Quick Access, hold to reveal tags without saving, then slide up/down and confirm the saved category matches the highlight. Test a bubble near each screen edge and all ten choices.
- Normal drag to bottom X keeps payment in Review; a horizontal hold-drag alone does not save. Income categories remain separate.
- Confirm multiple pending payments survive Later/Discard correctly. Quick Access remains opt-in.

Physical phone and Google Play upload acceptance remain pending.

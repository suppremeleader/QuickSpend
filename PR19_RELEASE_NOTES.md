# PR 19 — budget setup and settings polish, including Build 77

Base: Build 76 / 254b764452f0d5615a7952fa20db5e21b8047530. PR18 head 423ffeff88193530f2e6f503ffee969ddf4f7abb is integrated as a parent so all Build 77 changes are included in main via PR19. The only difference from the verified Build77 workflow is removal of its temporary branch trigger, already cleaned up in PR18.

- Unset monthly budget shows ₹0 and one Set budget action. Spending, savings and category rows appear after a monthly budget has been configured.
- Subscriptions has a visible section heading and shortcut on the Budget page.
- Settings button stays circular at 40px, with a centred 20px icon, even alongside a long profile name.
- Retains Build77 per-payment floater Later, red manual Discard, Hindi Discard translation and Play-style universal APK generation checks.
- Retains Build75/76 icon polish, savings, subscriptions, widgets, EMI, notes, budget, Review and capture behaviour.

Validation: combined 20 JavaScript suites and 13 Python tests pass locally. Chromium CI exercises real budget setup, unchanged transaction ledger, empty/configured state, dark/light screenshots, settings geometry, Hindi/discard and the existing planning/navigation flows. Native policies/calendar tests, signed Android compilation, bundle validation, universal APK generation and exact packaged-source checks remain enabled. Phone acceptance and Google Play upload acceptance remain pending.

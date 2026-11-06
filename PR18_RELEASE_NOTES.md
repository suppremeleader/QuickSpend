# PR 18 — per-payment Later and manual Discard

Base: Build 76 / 254b764452f0d5615a7952fa20db5e21b8047530. All Build 75/76 changes retained.

- Floater Later defers the current real payment, keeps it in Review and leaves other waiting payments available. Deferrals are scoped to the running capture service; a service restart can offer the payment again. Demo Later still removes only the preview.
- Manual New entry has a red Discard action. Closing/discarding creates no expense or pending payment. Existing entries keep Delete; captured payments keep Later.
- EMI and Subscriptions remain available as collapsed expense pickers.
- Discard is translated in Hindi.

Verification: full JavaScript/Python suites locally; CI browser checks actual discard and Hindi labels, native calendar/gesture tests, Android compilation/signing, bundletool validation and universal APK generation. APKs generated for validation use a disposable test key; the AAB keeps the existing production upload certificate. They are not distributed.

Previous Play upload error is generic. Build 76 had a valid archive/signature, same upload certificate as Build 75 and version code 1076. These checks cannot prove Play server acceptance or establish its rejection reason. This update adds a stronger package-generation check; no upload-error fix is claimed without a successful Play upload.

Phone acceptance: with two real pending payments, Later one and verify the next is offered while the first remains in Review; save the next once. Check discard leaves no transaction. Install widgets/savings/subscriptions from retained Build 76 and verify no regressions. Physical testing pending.

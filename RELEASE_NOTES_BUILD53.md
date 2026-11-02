# Quick Access experiment — Android

Quick Access is OFF by default. Enable it in Settings → Quick Access. iOS remains deferred.

- Hold a payment bubble for 400 ms to peek at its signed amount and payment type.
- Keep holding and drag to one of four glowing edges. Release there to save with that category; release in the centre to cancel. A normal tap opens the existing popup.
- Four configurable shortcuts for expenses and four for income, including custom categories. Bottom defaults to Other, with a soft coral glow.
- Rounded labels, selected-target checks, haptics and a short release animation. The bubble keeps its shape. Hindi, light/dark themes and reduced-motion preference are supported.
- Saves use the existing atomic payment source-key path; duplicate completion cannot create another entry. Failed saves leave the source in Review. Preview payments never create real financial entries.
- When enabled, dragging before holding moves the bubble. The old pull-to-hide gesture remains available when the experiment is disabled.

## Validation

The JavaScript regression suite and nine Python tests passed locally. Generated native Java syntax was checked. CI compiles the pure gesture tests and the Android release, then verifies signing and packaged native classes. Physical phone testing is required for overlay positioning, edge targeting and haptics.

## Phone checks

1. Enable Floating quick capture, then Quick Access. Assign the four expense/income shortcuts and reopen Settings to confirm persistence.
2. Use Floating quick capture's preview, switch to another app and hold its bubble. Confirm the amount appears below, with no targets until dragging.
3. Try all four edges; check selection and haptics. Preview must add no transaction. Repeat with a real pending payment; confirm exactly one correct category/amount/type in Home and its removal from Review.
4. Hold/release without dragging, release in the centre, cancel, lock the phone and switch back to QuickSpend. None should save.
5. Queue two payments; save the held one and confirm the next remains available. A new incoming payment during a hold must not replace the held payment.
6. Disable the experiment; normal popup, moving and pull-to-hide should behave as before. Check Hindi, Light mode, large text, gesture navigation and animations disabled.

FTUE polishing follows this experiment's validation.

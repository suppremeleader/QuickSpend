# Android V1 follow-ups — QUI-23

- Setup now uses large headings, a text QUICKSPEND wordmark and clear actions. Removed decorative wallet artwork and badges from language, intro and account setup.
- Home shows the five latest transactions with a **See all** button. The new history screen includes search (merchant, note, category, type, account or amount), account/category/type/date filters, amount/date sorting and month headings. Records can still be opened for editing or deletion.
- Insights keeps income, expenses, net flow and spending by category. Removed Daily average and Money over time.
- Switching bottom tabs or Home/Savings/EMI resets the destination to the top. Changing an EMI calendar year still preserves its scroll and the single-open schedule behavior.
- Normal bubble drag reveals a bottom close target. Dropping there hides capture while the original payment remains in Review. It does not create an expense.
- With **Settings → Quick Access** enabled, holding the bubble arms category capture. Pull and release launches opposite the pull toward the highlighted category. Short pulls cancel; the icon keeps its shape. Income and expense shortcuts remain separate.
- English and Hindi labels included. Existing pending-payment recovery, backup and EMI calculations retained.

## Validation

17 JavaScript regression suites and 10 Python tests pass locally. Java sources and generated service code parse successfully. CI additionally runs the pure Java gesture policies, real Chromium phone-layout checks with screenshots and the signed Android bundle build.

## Phone acceptance checks

1. Open history, search a note, combine filters, change sorting and edit a record. Back returns to Home.
2. Scroll Home down, switch to Insights and back: Home starts at the top. Repeat for Review, Lent & borrowed, Savings and EMI. In an expanded EMI schedule, change the year: scroll stays put.
3. On a pending payment, drag immediately to the bottom ×. Confirm the record is still pending in Review and no transaction was fabricated.
4. Enable Quick Access. Hold until haptic feedback, pull down → top category; left → right; up → bottom; right → left. Confirm highlighted category matches the saved one. Short pull/release and cancelled gestures must leave the payment pending.
5. Repeat with an incoming payment and with reduced animations, large text and both color modes.

No physical Android gesture verification or Play Store rollout is implied by the automated build. The iOS draft remains separate.

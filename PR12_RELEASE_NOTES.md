# PR #12 — phone Back handling and entry refinements

- Android Back buttons and completed Back gestures now route directly into QuickSpend's screen navigation through AndroidX's Back dispatcher in the active release workflow. Secondary screens and sheets close before Home is allowed to background the app. This replaces the proposed legacy callback in an unused build file.
- Transaction history restores the large transaction initial. A small message/person badge identifies automatic/manual entry source; it remains derived from the original capture metadata.
- New expense entries show an explicit **Add new EMI** action and existing active plans. Saving a new plan returns to the unfinished payment with that plan selected; cancelling or pressing Back returns without losing the amount, note or captured source. Creating the plan does not save the payment automatically.
- Existing EMI accounting, capture amount preservation, calendar scrolling, floater gestures and appearance sound are retained. New labels work in English and Hindi.

Validation: all 18 JavaScript suites, 10 Python tests, generated Java syntax and workflow YAML pass locally. CI checks the native callback in the actual build source, predictive-Back manifest setting, browser calls to the same JavaScript entry point used by Android, native compilation, signing and packaged resources.

Phone acceptance: use both Back button and swipe in a transaction editor, history, settings and secondary tabs; Home should be the exit point. Check source badges. From an unfinished captured payment, add/cancel an EMI and confirm the amount, note and original payment remain intact. Physical gesture verification remains pending.

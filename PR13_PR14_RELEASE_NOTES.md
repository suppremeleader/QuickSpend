# PRs #13 and #14 — EMI cleanup and note attachments

- New entries show one EMI heading with active existing plans underneath. Plan selection and captured payment amounts keep their existing behavior.
- Notes can include one compressed picture and one voice recording, with previews, playback and remove buttons. Saved entries show attachment indicators, and attachments can be edited or removed later.
- Voice recording requests microphone permission when used, stops after 30 seconds and releases the microphone on close, backgrounding or navigation. Late permission/file callbacks cannot add media to a different entry. Save waits for recording or image preparation to finish.
- Android microphone permissions are packaged, with permission handling provided by Capacitor's existing WebView client. Pictures use the system file/photo picker. Invalid imported media URLs are not rendered.
- Attachments persist across app restarts and are included in file backups and cloud payloads. **Cloud backup retains its existing 800 KB total payload limit.** If exceeded, the app clearly reports that records and attachments remain saved on this phone and asks for a file backup; it does not report a false cloud save or discard local records.
- New controls and messages support English and Hindi.

Validation: 19 JavaScript suites and 10 Python tests pass locally. CI exercises real Chromium photo compression, voice recording, save/remove and microphone cleanup in addition to existing UI tests, native policies, signed compilation and package verification. Physical Android microphone permission, recording/playback and picture picker tests remain pending.

PR #12 remains separate and unmerged. Its native Back dispatcher and source-badge refinements are not included in these two PRs.

## Device compatibility correction

Declared microphone hardware optional while retaining on-demand audio permission. Voice notes work where a microphone is available; other app features remain available without one. Added regression and signed-manifest checks to prevent accidental microphone-based Play device filtering. Re-upload the corrected bundle and re-check Play Console’s affected-device report; the exact six-device recovery cannot be confirmed from the screenshot alone.

## Current release: voice recording deferred

Removed new voice-recording controls and microphone/audio-settings permissions at user request. Text and compressed picture notes remain available. Existing voice recordings remain playable, removable and included in backups; no saved attachments are deleted. The manifest generator removes stale microphone permissions/features, and the signed bundle is checked for their absence. Voice recording is now a low-priority future idea alongside Gen Z language mode. Use this replacement build instead of build #65 or #66.

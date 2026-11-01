# Quick Capture status notification

Floating Quick Capture uses an Android foreground service so it can keep the user-enabled overlay available while QuickSpend is not on screen. Android requires a foreground-service notification for the entire time that service runs. Dismissing or clearing the notification does not stop the service, so Android can show it again.

QuickSpend keeps this notification silent and low priority, disables launcher badges for newly created status channels and requests hidden lock-screen content, and does not bridge it to connected devices. These are the least intrusive settings compatible with a reliable foreground service; Android versions and device manufacturers may still show the notification differently.

The **Status notification** button in Quick Capture settings opens the dedicated Android notification channel, where the user can choose any additional visibility controls offered by the device. Payment alert notifications use a separate channel and are not affected. Turning off **Floating quick capture** stops the service and removes its status notification; Quick Capture cannot reliably remain active without it.

Existing Android notification channels retain their saved settings across upgrades. Badge and visibility preferences on those channels remain under Android/user control; the app does not recreate the channel to override them. Lock-screen privacy is requested on each notification with `NotificationCompat.Builder.setVisibility(VISIBILITY_SECRET)`, rather than the system-only channel visibility setter.

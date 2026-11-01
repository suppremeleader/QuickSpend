# Quick Capture status notification

Floating Quick Capture uses an Android foreground service so it can keep the user-enabled overlay available while QuickSpend is not on screen. Android requires a foreground-service notification for the entire time that service runs. Dismissing or clearing the notification does not stop the service, so Android can show it again.

QuickSpend keeps this notification silent and low priority, prevents launcher badges and lock-screen content, and does not bridge it to connected devices. These are the least intrusive settings compatible with a reliable foreground service; Android versions and device manufacturers may still show the notification differently.

The **Status notification** button in Quick Capture settings opens the dedicated Android notification channel, where the user can choose any additional visibility controls offered by the device. Payment alert notifications use a separate channel and are not affected. Turning off **Floating quick capture** stops the service and removes its status notification; Quick Capture cannot reliably remain active without it.

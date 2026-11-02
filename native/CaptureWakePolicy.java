package com.quickspend.app;

/** Shared recovery decisions, independent of Android timing and UI state. */
public final class CaptureWakePolicy {
    public enum Action { STOP, WAIT_UNLOCK, WAIT_APP, REFRESH, START }
    public static Action decide(boolean enabled, boolean overlay, boolean pending,
            boolean hidden, boolean locked, boolean interactive, boolean appVisible, boolean running) {
        if (!enabled || !overlay || !pending || hidden) return Action.STOP;
        if (locked || !interactive) return Action.WAIT_UNLOCK;
        if (appVisible) return Action.WAIT_APP;
        return running ? Action.REFRESH : Action.START;
    }
    private CaptureWakePolicy() {}
}

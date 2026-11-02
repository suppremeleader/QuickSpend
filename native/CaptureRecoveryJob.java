package com.quickspend.app;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.Looper;

public final class CaptureRecoveryJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        // Finish this one-off job first, so check can schedule the next coalesced retry.
        final android.content.Context context=getApplicationContext();
        new Handler(Looper.getMainLooper()).post(()->CaptureRecovery.check(context));
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return true; }
}

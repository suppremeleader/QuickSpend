package com.quickspend.app;

import android.app.KeyguardManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** App-owned wake listener plus a durable, coalesced process-death fallback. */
public final class CaptureRecovery {
    private static final int JOB = 61204;
    private static final String PREFS = "qs_float", HIDDEN = "capture_hidden_until_new_payment";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static BroadcastReceiver receiver;
    private static int attempts;
    private static boolean checking;
    public static boolean hidden(Context c) { return c.getSharedPreferences(PREFS,0).getBoolean(HIDDEN,false); }
    public static void hide(Context c) {
        c.getSharedPreferences(PREFS,0).edit().putBoolean(HIDDEN,true).commit();
        cancel(c);
    }
    public static void payment(Context c) {
        c.getSharedPreferences(PREFS,0).edit().putBoolean(HIDDEN,false).putInt("capture_recovery_attempts",0).commit();
        attempts=0;
        // Persist the OS fallback before the SMS broadcast returns; a posted
        // callback alone is not guaranteed to survive receiver process teardown.
        if(FloatingCaptureService.enabled(c) && Settings.canDrawOverlays(c)) schedule(c);
        changed(c);
    }
    public static void changed(Context context) {
        Context c=context.getApplicationContext();
        MAIN.post(() -> check(c));
    }
    private static boolean pending(Context c) {
        JSONArray q=SmsStore.pending(c);
        for(int i=0;i<q.length();i++) {
            JSONObject p=q.optJSONObject(i);
            if(p!=null && PaymentParser.parse(p.optString("from"),p.optString("body")).amount>0) return true;
        }
        return false;
    }
    private static void watch(Context c) {
        if(receiver!=null)return;
        receiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
            if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) FloatingCaptureService.refresh(c,false);
            check(c);
            // SCREEN_ON may arrive before keyguard/interactive state settles.
            MAIN.postDelayed(()->check(c),250);
            MAIN.postDelayed(()->check(c),1200);
            MAIN.postDelayed(()->check(c),3000);
        }};
        IntentFilter f=new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON); f.addAction(Intent.ACTION_SCREEN_OFF); f.addAction(Intent.ACTION_USER_PRESENT);
        ContextCompat.registerReceiver(c,receiver,f,ContextCompat.RECEIVER_NOT_EXPORTED);
    }
    public static void check(Context c) {
        if(checking)return;
        checking=true;
        try {
            KeyguardManager key=(KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
            PowerManager power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
            CaptureWakePolicy.Action action=CaptureWakePolicy.decide(FloatingCaptureService.enabled(c),
                Settings.canDrawOverlays(c),pending(c),hidden(c),key.isKeyguardLocked(),power.isInteractive(),
                FloatingCaptureService.appIsVisible(),FloatingCaptureService.running());
            if(action==CaptureWakePolicy.Action.STOP){cancel(c);return;}
            watch(c);
            if(action==CaptureWakePolicy.Action.WAIT_UNLOCK){schedule(c);return;}
            if(action==CaptureWakePolicy.Action.WAIT_APP){cancelJob(c);return;}
            if(action==CaptureWakePolicy.Action.REFRESH){FloatingCaptureService.recoverPending(c);return;}
            attempts=c.getSharedPreferences(PREFS,0).getInt("capture_recovery_attempts",0);
            if(attempts>=5){cancelJob(c);return;}
            c.getSharedPreferences(PREFS,0).edit().putInt("capture_recovery_attempts",++attempts).commit();
            schedule(c);
            FloatingCaptureService.recoverPending(c);
        } catch(RuntimeException e) {
            android.util.Log.w("QuickSpend","Capture recovery deferred: "+e.getClass().getSimpleName());
            schedule(c);
        } finally {checking=false;}
    }
    private static void schedule(Context c) {
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(jobs.getPendingJob(JOB)==null) jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,CaptureRecoveryJob.class))
            .setMinimumLatency(15000).setOverrideDeadline(60000).build());
    }
    private static void cancelJob(Context c) { ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(JOB); }
    public static void visible(Context c) { cancelJob(c); }
    public static void cancel(Context context) {
        Context c=context.getApplicationContext();
        cancelJob(c);
        if(receiver!=null){try{c.unregisterReceiver(receiver);}catch(RuntimeException ignored){}receiver=null;}
    }
    private CaptureRecovery() {}
}

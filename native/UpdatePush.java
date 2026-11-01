package com.quickspend.app;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public final class UpdatePush {
    static final String CHANNEL="qs_updates",TOPIC="quickspend_internal_updates",PREFS="qs_updates";
    private UpdatePush() { }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    public static boolean enabled(Context c){return prefs(c).getBoolean("alerts",true);}
    public static void setEnabled(Context c,boolean value){if(!prefs(c).edit().putBoolean("alerts",value).commit())throw new IllegalStateException("Could not save update preferences");configure(c);}
    static boolean allowed(Context c){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);return NotificationManagerCompat.from(c).areNotificationsEnabled()&&(Build.VERSION.SDK_INT<26||nm.getNotificationChannel(CHANNEL)==null||nm.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE);}
    public static boolean configured(Context c){try{return FirebaseApp.initializeApp(c)!=null;}catch(Exception e){return false;}}
    public static void configure(Context c){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,QsLanguage.tr(c,"App updates"),NotificationManager.IMPORTANCE_DEFAULT));
        if(!configured(c))return;
        if(enabled(c)&&allowed(c))FirebaseMessaging.getInstance().subscribeToTopic(TOPIC).addOnFailureListener(error->android.util.Log.w("QuickSpend","Update alert subscription will retry"));
        else FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC);
    }
    public static long installed(Context c){try{android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}catch(Exception e){return Long.MAX_VALUE;}}
    public static Intent storeIntent(Context c){Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+c.getPackageName())).setPackage("com.android.vending");if(intent.resolveActivity(c.getPackageManager())==null)intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+c.getPackageName()));return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
    public static void show(Context c,String packageName,String offered){
        long code=UpdatePolicy.version(offered);SharedPreferences p=prefs(c);
        if(!c.getPackageName().equals(packageName)||!UpdatePolicy.notify(installed(c),code,p.getLong("notified",0),enabled(c))||!allowed(c))return;
        configure(c);
        PendingIntent open=PendingIntent.getActivity(c,61210,storeIntent(c),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notice=new NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_qs_notification)
            .setContentTitle(QsLanguage.tr(c,"QuickSpend update available"))
            .setContentText(QsLanguage.tr(c,"A new version is ready. Tap to update."))
            .setAutoCancel(true).setOnlyAlertOnce(true).setContentIntent(open).setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try { NotificationManagerCompat.from(c).notify(61210,notice.build());p.edit().putLong("notified",code).apply(); }catch(SecurityException denied){ }
    }
}

package com.quickspend.app;
import android.Manifest;
import android.app.AlarmManager;
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
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CapacitorPlugin(name="PlanningBridge",permissions={@Permission(strings={Manifest.permission.POST_NOTIFICATIONS},alias="notifications")})
public class PlanningBridge extends Plugin {
 static final Object LOCK=new Object(); static final String CHANNEL="qs_subscriptions";
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("qs_planning",Context.MODE_PRIVATE);}
 static JSONObject read(Context c){try{return new JSONObject(prefs(c).getString("snapshot","{}"));}catch(Exception e){return new JSONObject();}}
 static JSONArray choices(Context c,String key){return read(c).optJSONArray(key)==null?new JSONArray():read(c).optJSONArray(key);}
 static void channel(Context c){if(Build.VERSION.SDK_INT>=26){NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);n.createNotificationChannel(new NotificationChannel(CHANNEL,QsLanguage.tr(c,"Subscription reminders"),NotificationManager.IMPORTANCE_DEFAULT));}}
 static boolean allowed(Context c){if(!NotificationManagerCompat.from(c).areNotificationsEnabled())return false;if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).getNotificationChannel(CHANNEL);if(ch!=null&&ch.getImportance()==0)return false;}return true;}
 @PluginMethod public void syncPlanning(PluginCall call){synchronized(LOCK){try{String raw=call.getString("payload","{}");if(raw.length()>600000)throw new Exception("Planning snapshot too large");JSONObject data=new JSONObject(raw);JSONArray subs=data.optJSONArray("subscriptions");if(subs==null||subs.length()>2000)throw new Exception("Invalid subscriptions");JSONObject old=read(getContext());if(!prefs(getContext()).edit().putString("snapshot",data.toString()).commit())throw new Exception("Could not save native planning");channel(getContext());int errors=reschedule(getContext(),old);QsWidgets.updateAll(getContext());JSObject out=new JSObject();out.put("saved",true);if(errors>0)out.put("reminderError",QsLanguage.tr(getContext(),"Some reminders could not be scheduled. Reopen QuickSpend to retry."));out.put("remindersAllowed",allowed(getContext()));call.resolve(out);}catch(Exception e){call.reject("Could not update reminders/widgets. Open QuickSpend and retry.",e);}}}
 @PluginMethod public void requestNotifications(PluginCall call){if(Build.VERSION.SDK_INT>=33&&getPermissionState("notifications")!=PermissionState.GRANTED)requestPermissionForAlias("notifications",call,"notificationResult");else notificationResult(call);}
 @PermissionCallback private void notificationResult(PluginCall call){channel(getContext());reschedule(getContext(),read(getContext()));JSObject out=new JSObject();out.put("allowed",allowed(getContext()));call.resolve(out);}
 @PluginMethod public void takeAction(PluginCall call){getActivity().runOnUiThread(()->{JSObject out=new JSObject();Intent i=getActivity().getIntent();if(i!=null){String action=i.getStringExtra("qsPlanningAction");if(action!=null){out.put("action",action);out.put("key",i.getStringExtra("qsPlanningKey"));out.put("draft",i.getStringExtra("qsCaptureDraft"));out.put("planning",i.getStringExtra("qsCapturePlanning"));i.removeExtra("qsPlanningAction");i.removeExtra("qsPlanningKey");i.removeExtra("qsCaptureDraft");i.removeExtra("qsCapturePlanning");}}call.resolve(out);});}
 static PendingIntent alarm(Context c,String id,long due,long stamp){Intent i=new Intent(c,QsReminderReceiver.class).setAction("com.quickspend.app.SUBSCRIPTION").setData(Uri.parse("quickspend://reminder/"+Uri.encode(id))).putExtra("id",id).putExtra("due",due).putExtra("stamp",stamp);return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static int reschedule(Context c,JSONObject old){synchronized(LOCK){int errors=0;AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);JSONArray previous=old.optJSONArray("subscriptions");if(previous!=null)for(int j=0;j<previous.length();j++){JSONObject r=previous.optJSONObject(j);if(r!=null){String id=r.optString("id");am.cancel(alarm(c,id,0,0));JSONObject current=null;JSONArray latest=choices(c,"subscriptions");for(int k=0;k<latest.length();k++)if(id.equals(latest.optJSONObject(k).optString("id")))current=latest.optJSONObject(k);if(current==null||current.optLong("updatedAt")!=r.optLong("updatedAt")||!"active".equals(current.optString("status")))NotificationManagerCompat.from(c).cancel("sub:"+id,1);}}JSONArray subs=choices(c,"subscriptions");for(int j=0;j<subs.length();j++){JSONObject r=subs.optJSONObject(j);if(r==null||!"active".equals(r.optString("status"))||r.optInt("reminder",-1)<0)continue;try{long[] next=SubscriptionDates.next(r.optString("date"),r.optInt("interval"),r.optInt("reminder"),System.currentTimeMillis());am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next[0],alarm(c,r.optString("id"),next[1],r.optLong("updatedAt")));}catch(Exception e){errors++;android.util.Log.w("QuickSpend","Could not schedule subscription reminder",e);}}return errors;}}
 static void deliver(Context c,Intent intent){synchronized(LOCK){try{String id=intent.getStringExtra("id");long due=intent.getLongExtra("due",0),stamp=intent.getLongExtra("stamp",-1);JSONArray rows=choices(c,"subscriptions");for(int j=0;j<rows.length();j++){JSONObject r=rows.getJSONObject(j);if(!r.optString("id").equals(id)||r.optLong("updatedAt")!=stamp||!"active".equals(r.optString("status"))||r.optInt("reminder",-1)<0)continue;Set<String> done=new HashSet<>(prefs(c).getStringSet("delivered",new HashSet<String>()));String key=id+":"+SubscriptionDates.format(calendar(due))+":"+r.optInt("reminder");channel(c);if(allowed(c)&&!done.contains(key)){String amount=String.format(Locale.US,"₹%,.2f",r.optLong("cents")/100.0);NotificationCompat.Builder b=new NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_qs_notification).setContentTitle(r.optString("name")).setContentText(QsLanguage.tr(c,"Expected renewal")+" · "+amount+" · "+SubscriptionDates.format(calendar(due))).setContentIntent(QsWidgets.launch(c,"subscriptions",id)).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE);NotificationManagerCompat.from(c).notify("sub:"+id,1,b.build());if(done.size()>2000)done.clear();done.add(key);prefs(c).edit().putStringSet("delivered",done).commit();}break;}}catch(Exception e){android.util.Log.w("QuickSpend","Subscription reminder unavailable",e);}reschedule(c,read(c));}}
 static Calendar calendar(long ms){Calendar c=Calendar.getInstance();c.setTimeInMillis(ms);return c;}
}

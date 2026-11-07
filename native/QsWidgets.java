package com.quickspend.app;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class QsWidgets extends AppWidgetProvider {
 public static class Review extends QsWidgets {} public static class Summary extends QsWidgets {} public static class Quick extends QsWidgets {}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c);}
 static String tr(Context c,String s){return QsLanguage.tr(c,s);}
 static String money(double n){return String.format(Locale.US,"₹%,.2f",n);}
 static PendingIntent launch(Context c,String action,String key){Intent i=new Intent(c,MainActivity.class).setAction(Intent.ACTION_VIEW).setData(new Uri.Builder().scheme("quickspend").authority("widget").appendPath(action).appendPath(key==null?"":key).build()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("qsPlanningAction",action).putExtra("qsPlanningKey",key);return PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 public static void updateAll(Context c){for(Class<?> type:new Class<?>[]{Review.class,Summary.class,Quick.class}){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,type)))try{draw(c,m,id,type);}catch(Exception e){android.util.Log.w("QuickSpend","Widget could not refresh",e);}}}
 static void draw(Context c,AppWidgetManager m,int widgetId,Class<?> type)throws Exception {
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.qs_widget);JSONObject data=PlanningBridge.read(c),w=data.optJSONObject("widget");if(w==null)w=new JSONObject();String title,value,detail,action,key="";boolean light="light".equals(data.optString("colorMode"));v.setInt(R.id.qs_widget_root,"setBackgroundResource",light?R.drawable.qs_widget_light:R.drawable.qs_widget_dark);for(int id:new int[]{R.id.qs_widget_title,R.id.qs_widget_detail,R.id.qs_widget_action})v.setTextColor(id,light?Color.rgb(70,83,65):Color.rgb(186,195,174));v.setTextColor(R.id.qs_widget_value,light?Color.rgb(35,65,26):Color.rgb(201,243,107));
  if(type==Quick.class){title="QUICKSPEND";value=tr(c,"New entry");detail=tr(c,"Tap to add an expense or income");action="add";}
  else if(type==Summary.class){title=tr(c,"Monthly summary");Calendar now=Calendar.getInstance();String month=String.format(Locale.US,"%04d-%02d",now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1);boolean current=month.equals(w.optString("month"));double expense=w.optDouble("expense"),income=w.optDouble("income");Set<String> imported=new HashSet<>();JSONArray keys=w.optJSONArray("nativeKnown");if(keys!=null)for(int j=0;j<keys.length();j++)imported.add(keys.optString(j));JSONArray nativeSaved=SmsStore.snapshot(c).optJSONArray("entries");if(current&&nativeSaved!=null)for(int j=0;j<nativeSaved.length();j++){JSONObject r=nativeSaved.getJSONObject(j);Calendar date=Calendar.getInstance();date.setTimeInMillis(r.optLong("ts"));if(imported.contains(r.optString("nativeKey"))||r.optBoolean("emi")||date.get(Calendar.YEAR)!=now.get(Calendar.YEAR)||date.get(Calendar.MONTH)!=now.get(Calendar.MONTH))continue;if("Income".equals(r.optString("t")))income+=r.optDouble("a");if("Expense".equals(r.optString("t")))expense+=r.optDouble("a");}value=current?money(expense):"—";detail=current?tr(c,"Income")+" "+money(income)+" · "+tr(c,"Spent"):tr(c,"Open QuickSpend to refresh");action="summary";}
  else{title=tr(c,"Payment Review");JSONObject latest=null;Set<String> known=new HashSet<>(),handled=new HashSet<>();JSONObject snap=SmsStore.snapshot(c);JSONArray saved=snap.optJSONArray("entries"),q=snap.optJSONArray("items"),p=w.optJSONArray("pending");JSONArray hs=new JSONArray(c.getSharedPreferences("qs_sms",0).getString("overlay_handled","[]"));for(int j=0;j<hs.length();j++)handled.add(hs.optString(j));if(saved!=null)for(int j=0;j<saved.length();j++)handled.add(saved.getJSONObject(j).optString("sourceKey"));if(q!=null)for(int j=0;j<q.length();j++){JSONObject row=q.getJSONObject(j);if(row.optBoolean("demo"))continue;String source=SmsStore.key(row);if(handled.contains(source)||!known.add(source))continue;PaymentParser payment=PaymentParser.parse(row);JSONObject x=new JSONObject().put("sourceKey",source).put("ts",row.optLong("ts")).put("amount",payment.amount).put("name",payment.merchant);if(latest==null||x.optLong("ts")>latest.optLong("ts"))latest=x;}if(p!=null)for(int j=0;j<p.length();j++){JSONObject x=p.getJSONObject(j);String source=x.optString("sourceKey");if(source.isEmpty())source=String.valueOf(x.optLong("id"));if(handled.contains(source)||!known.add(source))continue;if(latest==null||x.optLong("ts")>latest.optLong("ts"))latest=x;}
   boolean smsGranted=androidx.core.content.ContextCompat.checkSelfPermission(c,android.Manifest.permission.RECEIVE_SMS)==android.content.pm.PackageManager.PERMISSION_GRANTED;boolean upiReady=PhonePeNotificationListener.enabled(c)&&PhonePeNotificationListener.allowed(c);action="review";value=latest==null&&!smsGranted&&!upiReady?tr(c,"Allow SMS access"):latest==null?tr(c,"No payments waiting"):latest.optDouble("amount")>0?money(latest.optDouble("amount")):tr(c,"Check amount");detail=latest==null?tr(c,"New payments appear here"):latest.optString("name")+" · "+known.size()+" "+tr(c,"To review");if(latest!=null)key=latest.optString("sourceKey",String.valueOf(latest.optLong("id")));
  }
  v.setTextViewText(R.id.qs_widget_title,title);v.setTextViewText(R.id.qs_widget_value,value);v.setTextViewText(R.id.qs_widget_detail,detail);v.setTextViewText(R.id.qs_widget_action,tr(c,"Open QuickSpend"));v.setOnClickPendingIntent(R.id.qs_widget_root,launch(c,action,key));m.updateAppWidget(widgetId,v);
 }
}

package com.quickspend.app;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
/** Calendar anchors survive short months; no payment/ledger state is touched. */
public final class SubscriptionDates {
  static Calendar parse(String value) throws Exception {SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);if(value==null||!value.matches("\\d{4}-\\d{2}-\\d{2}"))throw new Exception("Invalid date");Date d=f.parse(value);Calendar c=Calendar.getInstance();c.setTime(d);return c;}
  static Calendar cycle(Calendar anchor,int months){Calendar c=(Calendar)anchor.clone();int day=c.get(Calendar.DAY_OF_MONTH);c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,months);c.set(Calendar.DAY_OF_MONTH,Math.min(day,c.getActualMaximum(Calendar.DAY_OF_MONTH)));return c;}
  static String format(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
  public static long[] next(String date,int interval,int offset,long now) throws Exception {if(!(interval==1||interval==3||interval==12)||!(offset==1||offset==3||offset==7))throw new Exception("Invalid schedule");Calendar a=parse(date),today=Calendar.getInstance();today.setTimeInMillis(now);int diff=(today.get(Calendar.YEAR)-a.get(Calendar.YEAR))*12+today.get(Calendar.MONTH)-a.get(Calendar.MONTH);int start=Math.max(0,diff/interval-1);for(int n=start;n<start+120;n++){Calendar due=cycle(a,n*interval),alarm=(Calendar)due.clone();alarm.add(Calendar.DAY_OF_MONTH,-offset);alarm.set(Calendar.HOUR_OF_DAY,9);alarm.set(Calendar.MINUTE,0);alarm.set(Calendar.SECOND,0);alarm.set(Calendar.MILLISECOND,0);if(alarm.getTimeInMillis()>now)return new long[]{alarm.getTimeInMillis(),due.getTimeInMillis()};}throw new Exception("Date out of range");}
}

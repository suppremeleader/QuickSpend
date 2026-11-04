package com.quickspend.app;
import java.util.Calendar;
import java.util.TimeZone;
public class SubscriptionDatesTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  for(String zone:new String[]{"Asia/Kolkata","America/New_York","UTC"}){TimeZone.setDefault(TimeZone.getTimeZone(zone));Calendar jan=SubscriptionDates.parse("2026-01-31");check(SubscriptionDates.format(SubscriptionDates.cycle(jan,1)).equals("2026-02-28"),"February clamp");check(SubscriptionDates.format(SubscriptionDates.cycle(jan,2)).equals("2026-03-31"),"March restores anchor");check(SubscriptionDates.format(SubscriptionDates.cycle(jan,3)).equals("2026-04-30"),"Quarter clamp");Calendar leap=SubscriptionDates.parse("2024-02-29");check(SubscriptionDates.format(SubscriptionDates.cycle(leap,12)).equals("2025-02-28"),"Year clamp");check(SubscriptionDates.format(SubscriptionDates.cycle(leap,48)).equals("2028-02-29"),"Leap recovers");long now=SubscriptionDates.parse("2026-01-01").getTimeInMillis();long[] next=SubscriptionDates.next("2026-01-31",1,3,now);Calendar reminder=Calendar.getInstance();reminder.setTimeInMillis(next[0]);check(SubscriptionDates.format(reminder).equals("2026-01-28"),"Offset");check(reminder.get(Calendar.HOUR_OF_DAY)==9,"Morning local time");long[] following=SubscriptionDates.next("2026-01-31",1,3,next[0]+1);check(following[0]>next[0],"No rearm of the delivered cycle");}
  boolean invalid=false;try{SubscriptionDates.next("2026-02-30",1,3,0);}catch(Exception e){invalid=true;}check(invalid,"Invalid date rejected");System.out.println("PASS: native subscription calendar anchors, leap/quarter/year/DST and next-cycle reminder");
 }
}

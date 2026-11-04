package com.quickspend.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class QsReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if("com.quickspend.app.SUBSCRIPTION".equals(i.getAction()))PlanningBridge.deliver(c,i);else PlanningBridge.reschedule(c,PlanningBridge.read(c));QsWidgets.updateAll(c);}
}

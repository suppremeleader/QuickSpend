package com.quickspend.app;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
public class UpdateMessagingService extends FirebaseMessagingService {
    @Override public void onMessageReceived(RemoteMessage message){
        if(!"app_update".equals(message.getData().get("type")))return;
        UpdatePush.show(this,message.getData().get("package"),message.getData().get("versionCode"));
    }
    @Override public void onNewToken(String token){UpdatePush.configure(this);}
}

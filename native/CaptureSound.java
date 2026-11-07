package com.quickspend.app;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
/** Local, once-per-visible-payment cue. Playback failure never affects capture. */
public final class CaptureSound {
    private CaptureSound() {}
    private static MediaPlayer player;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    public static synchronized void appeared(Context c,JSONArray pending,boolean newBubble){
        try {
            Set<String> current=new HashSet<>();
            for(int i=0;i<pending.length();i++){
                JSONObject item=pending.optJSONObject(i);
                if(item!=null&&PaymentParser.parse(item).amount>0)current.add(SmsStore.key(item));
            }
            SharedPreferences prefs=c.getSharedPreferences("qs_float",0);
            Set<String> seen=new HashSet<>(prefs.getStringSet("capture_sound_seen",new HashSet<>()));
            boolean announce=CaptureSoundPolicy.announce(newBubble,current,seen);
            // Mark even in silent mode: unlocking, rotation or unmuting must not replay old payments.
            if(!seen.equals(current)&&!prefs.edit().putStringSet("capture_sound_seen",current).commit())return;
            if(!announce||player!=null)return;
            AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            NotificationManager notices=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            if(audio==null||notices==null||!CaptureSoundPolicy.audible(audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL,audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION),notices.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL))return;
            AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            MediaPlayer cue=MediaPlayer.create(c.getApplicationContext(),R.raw.qs_capture_appear,attributes,0);
            if(cue==null)return;player=cue;
            cue.setOnCompletionListener(CaptureSound::release);
            cue.setOnErrorListener((p,what,extra)->{release(p);return true;});
            cue.setLooping(false);cue.start();MAIN.postDelayed(()->release(cue),3000);
        }catch(RuntimeException ignored){if(player!=null)release(player);}
    }
    private static synchronized void release(MediaPlayer cue){
        if(player!=cue)return;player=null;
        try{cue.release();}catch(RuntimeException ignored){}
    }
}

package com.quickspend.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
/** Non-interactive visual layer; the original bubble retains the touch gesture. */
public final class QuickAccessOverlay extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] labels;
    private final String amount,type,hint,release;
    private final boolean light;
    private boolean dismissMode;
    public boolean targetActive;
    private final int[] colors={0xffc9f36b,0xff68b8ff,0xffff9995,0xffba9bff};
    public int selected=-1;
    public boolean dragging;
    public float bubbleX,bubbleY;
    private float d(float n){return n*getResources().getDisplayMetrics().density;}
    public QuickAccessOverlay(Context c,String[] labels,String amount,String type,boolean light){super(c);this.labels=labels;this.amount=amount;this.type=type;this.light=light;hint=QsLanguage.tr(c,"Slide up or down to choose");release=QsLanguage.tr(c,"Release to save");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public QuickAccessOverlay(Context c,boolean light){this(c,new String[0],"","",light);dismissMode=true;}
    private void pill(Canvas c,String value,float x,float y,float maxWidth,int color,boolean active){
        p.setShader(null);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(d(14));
        String text=TextUtils.ellipsize(value,new android.text.TextPaint(p),maxWidth-d(28),TextUtils.TruncateAt.END).toString();float w=Math.min(maxWidth,p.measureText(text)+d(28)),h=d(44);
        x=Math.max(w/2+d(8),Math.min(x,getWidth()-w/2-d(8)));y=Math.max(h/2+d(8),Math.min(y,getHeight()-h/2-d(8)));
        p.setColor(active?color:(light?0xfff6f9f1:0xff20252b));c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,d(22),d(22),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(active?2:1));p.setColor(color);c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,d(22),d(22),p);p.setStyle(Paint.Style.FILL);
        p.setColor(active?0xff101714:(light?0xff182119:Color.WHITE));c.drawText(text,x-p.measureText(text)/2,y-(p.ascent()+p.descent())/2,p);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight(),band=d(80);
        if(dismissMode){
            float x=w/2,y=h-d(96);p.setShader(null);p.setColor(targetActive?0xffe24e65:(light?0xfff3e7e9:0xff302329));c.drawCircle(x,y,d(targetActive?37:32),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(2));p.setColor(targetActive?Color.WHITE:0xffffb8c4);c.drawCircle(x,y,d(targetActive?37:32),p);c.drawLine(x-d(8),y-d(8),x+d(8),y+d(8),p);c.drawLine(x-d(8),y+d(8),x+d(8),y-d(8),p);p.setStyle(Paint.Style.FILL);
            pill(c,QsLanguage.tr(getContext(),"Keep in Review"),x,y-d(65),w-d(24),0xffff9995,false);return;
        }
        if(dragging){
            float step=d(48),centre=bubbleY+d(30),radius=d(150);int middle=(labels.length-1)/2;
            for(int i=0;i<labels.length;i++){
                float y=centre+(i-middle)*step;
                float normalized=Math.min(1f,Math.abs(y-centre)/(Math.max(1,middle)*step));
                float x=bubbleX-d(76)-radius*(1f-normalized*.58f);
                pill(c,(selected==i?"✓ ":"")+labels[i],x,y,Math.min(w*.48f,d(190)),colors[i%colors.length],selected==i);
            }
            pill(c,selected>=0?release:hint,Math.max(d(110),bubbleX-d(130)),Math.min(h-d(36),centre+(middle+1.4f)*step),Math.min(w-d(24),d(245)),0xffc9f36b,false);
        }
        float y=bubbleY+d(80);if(y>h-d(165))y=bubbleY-d(28);
        pill(c,amount,bubbleX+d(30),y,Math.min(w-d(24),d(245)),"Income".equals(type)?0xffc9f36b:0xff68b8ff,true);
    }
}


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
    private final int[] colors={0xffc9f36b,0xff68b8ff,0xffff9995,0xffba9bff};
    public int selected=-1;
    public boolean dragging;
    public float bubbleX,bubbleY;
    private float d(float n){return n*getResources().getDisplayMetrics().density;}
    public QuickAccessOverlay(Context c,String[] labels,String amount,String type,boolean light){super(c);this.labels=labels;this.amount=amount;this.type=type;this.light=light;hint=QsLanguage.tr(c,"Drag to a category");release=QsLanguage.tr(c,"Release to save");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
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
        if(dragging){for(int i=0;i<4;i++){int color=colors[i];int glow=(selected==i?0xc9000000:0x55000000)|(color&0xffffff);float x0=i==1?w:i==3?0:w/2,y0=i==0?0:i==2?h:h/2,x1=i==1?w-band:i==3?band:x0,y1=i==0?band:i==2?h-band:y0;p.setShader(new LinearGradient(x0,y0,x1,y1,glow,Color.TRANSPARENT,Shader.TileMode.CLAMP));if(i==0)c.drawRect(0,0,w,band,p);if(i==1)c.drawRect(w-band,0,w,h,p);if(i==2)c.drawRect(0,h-band,w,h,p);if(i==3)c.drawRect(0,0,band,h,p);}
            float[] xs={w/2,w-d(78),w/2,d(78)},ys={d(58),h/2,h-d(58),h/2};
            for(int i=0;i<4;i++)pill(c,(selected==i?"✓ ":"")+labels[i],xs[i],ys[i],Math.min(w*.40f,d(180)),colors[i],selected==i);
            pill(c,selected>=0?release:hint,w/2,h-d(122),w-d(24),0xffc9f36b,false);
        }
        float y=bubbleY+d(80);if(y>h-d(165))y=bubbleY-d(28);
        pill(c,amount,bubbleX+d(30),y,Math.min(w-d(24),d(245)),"Income".equals(type)?0xffc9f36b:0xff68b8ff,true);
    }
}

package com.quickspend.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
        p.setColor(active?color:(light?0xfff8f8f8:0xff20252b));c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,d(22),d(22),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(active?2:1));p.setColor(color);c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,d(22),d(22),p);p.setStyle(Paint.Style.FILL);
        p.setColor(active?(light?Color.WHITE:0xff101714):(light?0xff181818:Color.WHITE));c.drawText(text,x-p.measureText(text)/2,y-(p.ascent()+p.descent())/2,p);
    }
    public int targetForSlide(float dy){if(getHeight()<=0||getWidth()<=0)return -1;float density=getResources().getDisplayMetrics().density;return QuickAccessPolicy.carouselTarget(dy,labels.length,QuickAccessPolicy.carouselStep(getHeight(),density,labels.length),QuickAccessPolicy.carouselOrigin(labels.length,getHeight(),bubbleY+d(30),density));}
    public float[] categoryPosition(int index){
        float density=getResources().getDisplayMetrics().density,w=getWidth();
        float[] rail=QuickAccessPolicy.carouselRail(labels.length,selected,getHeight(),bubbleY+d(30),density);
        float visible=rail[1],offset=index-rail[0],norm=visible<=1?0:Math.min(1,Math.abs(offset-(visible-1)/2)/Math.max(1,(visible-1)/2));
        float maxWidth=Math.min(w*.48f,d(190)),direction=bubbleX<maxWidth+d(30)?1:-1;
        float x=bubbleX+d(30)+direction*(d(125)-d(30)*norm*norm);
        x=Math.max(maxWidth/2+d(8),Math.min(x,w-maxWidth/2-d(8)));
        return new float[]{x,rail[3]+offset*rail[2]};
    }
    private void categoryPill(Canvas c,String category,float x,float y,float width,boolean active){
        float h=d(44);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(active?(light?0xff191919:0xffc9f36b):(light?0xfff8f8f8:0xff20252b));c.drawRoundRect(x-width/2,y-h/2,x+width/2,y+h/2,d(22),d(22),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(active?2:1));p.setColor(active?(light?0xff191919:0xffc9f36b):(light?0xffdedede:0xff3b4249));c.drawRoundRect(x-width/2,y-h/2,x+width/2,y+h/2,d(22),d(22),p);p.setStyle(Paint.Style.FILL);
        CategorySymbols.draw(getContext(),c,category,x-width/2+d(9),y-d(13),d(26));
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(d(13));p.setColor(active?(light?Color.WHITE:0xff181818):(light?0xff181818:Color.WHITE));
        String label=TextUtils.ellipsize(QsLanguage.tr(getContext(),category),new android.text.TextPaint(p),Math.max(d(8),width-d(52)),TextUtils.TruncateAt.END).toString();
        c.drawText(label,x-width/2+d(43),y-(p.ascent()+p.descent())/2,p);
    }
    private void drawCategoryRail(Canvas canvas,int start,int visible,float width){
        Path curve=new Path();
        float markerX=0,markerY=0;
        float direction=bubbleX<width+d(30)?-1:1;
        for(int index=start;index<start+visible;index++){
            float[] point=categoryPosition(index);
            float x=Math.max(d(4),Math.min(getWidth()-d(4),point[0]+direction*(width/2+d(5))));
            if(index==start)curve.moveTo(x,point[1]);else curve.lineTo(x,point[1]);
            if(index==selected){markerX=x;markerY=point[1];}
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(d(1.5f));
        p.setColor(light?0xff757575:0xff82916c);
        canvas.drawPath(curve,p);
        p.setStyle(Paint.Style.FILL);
        if(selected>=start&&selected<start+visible){
            p.setColor(light?0xff191919:0xffc9f36b);
            canvas.drawCircle(markerX,markerY,d(3.5f),p);
        }
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();
        if(dismissMode){
            float x=w/2,y=h-d(96);p.setShader(null);p.setColor(targetActive?0xffe24e65:(light?0xfff3e7e9:0xff302329));c.drawCircle(x,y,d(targetActive?37:32),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(2));p.setColor(targetActive?Color.WHITE:0xffffb8c4);c.drawCircle(x,y,d(targetActive?37:32),p);c.drawLine(x-d(8),y-d(8),x+d(8),y+d(8),p);c.drawLine(x-d(8),y+d(8),x+d(8),y-d(8),p);p.setStyle(Paint.Style.FILL);
            pill(c,QsLanguage.tr(getContext(),"Keep in Review"),x,y-d(65),w-d(24),0xffff9995,false);return;
        }
        if(labels.length>0){
            float[] rail=QuickAccessPolicy.carouselRail(labels.length,selected,h,bubbleY+d(30),getResources().getDisplayMetrics().density);
            int start=(int)rail[0],visible=(int)rail[1];
            float width=Math.min(w*.48f,d(190));
            c.drawColor(light?0x18000000:0x38000000);
            drawCategoryRail(c,start,visible,width);
            for(int i=start;i<start+visible;i++){
                float[] point=categoryPosition(i);
                categoryPill(c,labels[i],point[0],point[1],width,selected==i);
            }
            pill(c,selected>=0?release:hint,w/2,h-d(30),w-d(24),0xffc9f36b,false);
        }
        pill(c,amount,w/2,d(28),Math.min(w-d(24),d(290)),light?0xff191919:"Income".equals(type)?0xffc9f36b:0xff68b8ff,true);

    }
}


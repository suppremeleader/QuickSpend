package com.quickspend.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
/** Non-interactive visual layer; the original bubble retains the touch gesture. */
public final class QuickAccessOverlay extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] labels;
    private final String amount,cancel;
    private final boolean light;
    private boolean dismissMode;
    public boolean targetActive;
    public int selected=-1;
    public float bubbleX,bubbleY;
    private final float[] emphasis;
    private boolean firstDraw=true;
    private float d(float n){return n*getResources().getDisplayMetrics().density;}
    public QuickAccessOverlay(Context c,String[] labels,String amount,boolean light){super(c);this.labels=labels;this.amount=amount;this.light=light;emphasis=new float[labels.length];cancel="×  "+QsLanguage.tr(c,"Cancel");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public QuickAccessOverlay(Context c,boolean light){this(c,new String[0],"",light);dismissMode=true;}
    private void pill(Canvas c,String value,float x,float y,float maxWidth,int color,boolean active){
        p.setShader(null);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(d(14));
        String text=TextUtils.ellipsize(value,new android.text.TextPaint(p),maxWidth-d(28),TextUtils.TruncateAt.END).toString();float w=Math.min(maxWidth,p.measureText(text)+d(28)),h=d(44);
        x=Math.max(w/2+d(8),Math.min(x,getWidth()-w/2-d(8)));y=Math.max(h/2+d(8),Math.min(y,getHeight()-h/2-d(8)));
        p.setColor(active?color:(light?0xfff8f8f8:0xff20252b));c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,d(22),d(22),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(active?2:1));p.setColor(color);c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,d(22),d(22),p);p.setStyle(Paint.Style.FILL);
        p.setColor(active?(light?Color.WHITE:0xff101714):(light?0xff181818:Color.WHITE));c.drawText(text,x-p.measureText(text)/2,y-(p.ascent()+p.descent())/2,p);
    }
    public int targetForSlide(float dx,float dy,float x,float y){
        float density=getResources().getDisplayMetrics().density;
        float[] close=cancelPosition();
        targetActive=QuickAccessPolicy.cancelTarget(x,y,getWidth(),getHeight(),density,close[0],close[1])&&Math.hypot(dx,dy)>=d(18);
        if(getHeight()<=0||getWidth()<=0)return -1;
        int origin=QuickAccessPolicy.visibleOrigin(labels.length,getHeight(),bubbleY+d(30),density);
        return QuickAccessPolicy.slideTarget(dx,dy,labels.length,QuickAccessPolicy.carouselStep(getHeight(),density,labels.length),origin,d(18),targetActive);
    }
    private float railWidth(){return Math.min(getWidth()*.59f,d(220));}
    private float railX(){
        float width=railWidth(),edge=width/2+d(14),anchor=bubbleX+d(30);
        float preferred=anchor+(anchor<getWidth()/2?1:-1)*(width/2+d(40));
        return Math.max(edge,Math.min(preferred,getWidth()-edge));
    }
    private float[] rail(){
        return QuickAccessPolicy.carouselRail(labels.length,selected,getHeight(),bubbleY+d(30),getResources().getDisplayMetrics().density);
    }
    public float[] categoryPosition(int index){
        float[] rail=rail();
        return new float[]{railX(),rail[3]+(index-rail[0])*rail[2]};
    }
    private float[] cancelPosition(){
        float[] rail=rail();
        return new float[]{railX(),QuickAccessPolicy.cancelCenter(rail,getResources().getDisplayMetrics().density)};
    }
    private void drawAmount(Canvas canvas,float x,float y,float width){
        p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(light?0xfff4faeb:0xff252f22);
        canvas.drawRoundRect(x-width/2,y-d(20),x+width/2,y+d(20),d(13),d(13),p);
        p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(d(22));
        String shown=TextUtils.ellipsize(amount,new android.text.TextPaint(p),width-d(20),TextUtils.TruncateAt.END).toString();
        p.setColor(light?0xff24391a:0xffdcf8ae);
        canvas.drawText(shown,x-p.measureText(shown)/2,y-(p.ascent()+p.descent())/2,p);
    }
    private void categoryPill(Canvas c,String category,float x,float y,float width,boolean active,float focus){
        float h=d(36+10*focus),expandedWidth=width+d(10)*focus;
        width=expandedWidth;p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(active?(light?0xffd9efa9:0xffc9f36b):(light?0xfff8f8f8:0xff20252b));c.drawRoundRect(x-width/2,y-h/2,x+width/2,y+h/2,d(22),d(22),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(active?2:1));p.setColor(active?(light?0xff6c8d39:0xffc9f36b):(light?0xffdedede:0xff3b4249));c.drawRoundRect(x-width/2,y-h/2,x+width/2,y+h/2,d(22),d(22),p);p.setStyle(Paint.Style.FILL);
        float icon=d(24+11*focus);
        CategorySymbols.draw(getContext(),c,category,x-width/2+d(22)-icon/2,y-icon/2,icon);
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(d(13+2*focus));p.setColor(active?(light?0xff24391a:0xff181818):(light?0xff181818:Color.WHITE));
        String label=TextUtils.ellipsize(QsLanguage.tr(getContext(),category),new android.text.TextPaint(p),Math.max(d(8),width-d(52)),TextUtils.TruncateAt.END).toString();
        c.drawText(label,x-width/2+d(43),y-(p.ascent()+p.descent())/2,p);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();
        if(dismissMode){
            float x=w/2,y=h-d(96);p.setShader(null);p.setColor(targetActive?0xffe24e65:(light?0xfff3e7e9:0xff302329));c.drawCircle(x,y,d(targetActive?37:32),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(2));p.setColor(targetActive?Color.WHITE:0xffffb8c4);c.drawCircle(x,y,d(targetActive?37:32),p);c.drawLine(x-d(8),y-d(8),x+d(8),y+d(8),p);c.drawLine(x-d(8),y+d(8),x+d(8),y-d(8),p);p.setStyle(Paint.Style.FILL);
            pill(c,QsLanguage.tr(getContext(),"Keep in Review"),x,y-d(65),w-d(24),0xffff9995,false);return;
        }
        if(labels.length>0){
            float[] rail=rail();
            int start=(int)rail[0],visible=(int)rail[1];
            float width=railWidth(),density=getResources().getDisplayMetrics().density,x=railX();
            float amountY=QuickAccessPolicy.amountCenter(rail,density),cancelY=QuickAccessPolicy.cancelCenter(rail,density);
            c.drawColor(0x650c100d);
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(light?0xfffdfefb:0xff171c19);
            c.drawRoundRect(x-width/2-d(8),amountY-d(26),x+width/2+d(8),cancelY+d(27),d(21),d(21),p);
            drawAmount(c,x,amountY,width);
            boolean animating=false;
            for(int i=start;i<start+visible;i++){
                float target=i==selected?1f:0f;
                if(firstDraw)emphasis[i]=target;
                else emphasis[i]+=(target-emphasis[i])*.38f;
                if(Math.abs(emphasis[i]-target)>.015f)animating=true;
                if(i==selected)continue; // draw highlighted choice last, above neighbours
                float[] point=categoryPosition(i);
                categoryPill(c,labels[i],point[0],point[1],width,false,emphasis[i]);
            }
            if(selected>=start&&selected<start+visible){
                float[] point=categoryPosition(selected);
                categoryPill(c,labels[selected],point[0],point[1],width,true,emphasis[selected]);
            }
            firstDraw=false;
            if(animating)postInvalidateOnAnimation();
            float[] close=cancelPosition();
            pill(c,cancel,close[0],close[1],Math.min(w-d(24),d(138)),light?0xffb52d44:0xffffb8c4,targetActive);
        }
    }
}

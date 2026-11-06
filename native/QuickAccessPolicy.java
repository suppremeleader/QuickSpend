package com.quickspend.app;
/** Pure gesture rules: no Android APIs or financial mutations. */
public final class QuickAccessPolicy {
    private QuickAccessPolicy() {}
    public static final long HOLD_MS=400;
    public static int carouselTarget(float dy,int count,float step){
        if(count<=0||step<=0||!Float.isFinite(step)||!Float.isFinite(dy))return -1;
        int index=Math.round(dy/step)+count/2;
        return Math.max(0,Math.min(count-1,index));
    }
    public static float carouselStep(float height,float density,int count){return Math.min(48*density,Math.max(1,(height-48*density)/Math.max(1,count-1)));}
    public static int carouselOrigin(int count,float height,float anchorY,float density){if(count<=0||height<=48*density)return Math.max(0,count/2);return Math.max(0,Math.min(count-1,Math.round((anchorY-24*density)/(height-48*density)*(count-1))));}
    public static int carouselTarget(float dy,int count,float step,int origin){if(count<=0||step<=0||!Float.isFinite(step)||!Float.isFinite(dy)||origin<0||origin>=count)return -1;return Math.max(0,Math.min(count-1,Math.round(dy/step)+origin));}
    /** Rail rows move as one unit; smaller windows scroll across all ten choices. */
    public static float[] carouselRail(int count,int selected,float height,float anchorY,float density){
        if(count<=0||height<=0||density<=0)return new float[]{0,0,0,0};
        float step=48*density,available=Math.max(0,height-160*density);
        int visible=Math.min(count,Math.max(1,(int)Math.floor(available/step)+1));
        int origin=carouselOrigin(count,height,anchorY,density),current=selected>=0?Math.min(count-1,selected):origin;
        int start=Math.max(0,Math.min(count-visible,current-visible/2));
        float top=80*density,bottom=height-80*density;
        if(bottom<top){top=height/2;bottom=top;}
        float first=Math.max(top,Math.min(anchorY+(start-origin)*step,bottom-(visible-1)*step));
        return new float[]{start,visible,step,first};
    }
    public static int target(float x,float y,float width,float height,float band){
        if(width<=0||height<=0||band<=0||!Float.isFinite(x)||!Float.isFinite(y)||x<0||y<0||x>width||y>height)return -1;
        float[] distances={y,width-x,height-y,x};int best=0;
        for(int i=1;i<4;i++)if(distances[i]<distances[best])best=i;
        return distances[best]<=band?best:-1;
    }
    /** Ray from the resting bubble, travelling opposite to the elastic pull. */
    public static int launchTarget(float x,float y,float dx,float dy,float width,float height){
        if(width<=0||height<=0||!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(dx)||!Float.isFinite(dy)||x<0||y<0||x>width||y>height||Math.hypot(dx,dy)<1)return -1;
        float vx=-dx,vy=-dy,best=Float.POSITIVE_INFINITY;int slot=-1;
        float[] times={vy<0?-y/vy:Float.POSITIVE_INFINITY,vx>0?(width-x)/vx:Float.POSITIVE_INFINITY,vy>0?(height-y)/vy:Float.POSITIVE_INFINITY,vx<0?-x/vx:Float.POSITIVE_INFINITY};
        for(int i=0;i<4;i++)if(times[i]>=0&&times[i]<best){best=times[i];slot=i;}
        return slot;
    }
    public static boolean dismissTarget(float x,float y,float width,float height,float density){
        return density>0&&width>0&&height>0&&Float.isFinite(x)&&Float.isFinite(y)&&Math.hypot(x-width/2,y-(height-96*density))<=56*density;
    }
    public static boolean canSave(boolean enabled,long held,float travel,float minTravel,int slot,double amount,boolean locked,boolean cancelled){
        return enabled&&held>=HOLD_MS&&Float.isFinite(travel)&&Float.isFinite(minTravel)&&minTravel>0&&travel>=minTravel&&slot>=0&&slot<10&&Double.isFinite(amount)&&amount>0&&!locked&&!cancelled;
    }
}


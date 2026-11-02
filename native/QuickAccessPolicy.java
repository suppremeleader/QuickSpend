package com.quickspend.app;
/** Pure gesture rules: no Android APIs or financial mutations. */
public final class QuickAccessPolicy {
    private QuickAccessPolicy() {}
    public static final long HOLD_MS=400;
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
        return enabled&&held>=HOLD_MS&&travel>=minTravel&&slot>=0&&slot<4&&Double.isFinite(amount)&&amount>0&&!locked&&!cancelled;
    }
}


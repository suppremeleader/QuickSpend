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
    public static boolean canSave(boolean enabled,long held,float travel,float minTravel,int slot,double amount,boolean locked,boolean cancelled){
        return enabled&&held>=HOLD_MS&&travel>=minTravel&&slot>=0&&slot<4&&Double.isFinite(amount)&&amount>0&&!locked&&!cancelled;
    }
}

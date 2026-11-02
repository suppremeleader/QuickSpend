import com.quickspend.app.QuickAccessPolicy;
public class QuickAccessPolicyTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args){
        check(QuickAccessPolicy.target(200,20,400,800,60)==0,"top");
        check(QuickAccessPolicy.target(390,400,400,800,60)==1,"right");
        check(QuickAccessPolicy.target(200,790,400,800,60)==2,"bottom");
        check(QuickAccessPolicy.target(10,400,400,800,60)==3,"left");
        check(QuickAccessPolicy.target(200,400,400,800,60)==-1,"centre cancels");
        check(QuickAccessPolicy.target(-1,400,400,800,60)==-1,"outside cancels");
        check(QuickAccessPolicy.target(Float.NaN,400,400,800,60)==-1,"invalid coordinates cancel");
        check(QuickAccessPolicy.target(20,20,400,800,60)==0,"corner tie deterministic");
        check(QuickAccessPolicy.canSave(true,500,100,72,0,50,false,false),"deliberate hold and release");
        check(!QuickAccessPolicy.canSave(false,500,100,72,0,50,false,false),"default-off cannot save");
        check(!QuickAccessPolicy.canSave(true,399,100,72,0,50,false,false),"short tap cannot save");
        check(!QuickAccessPolicy.canSave(true,500,71,72,0,50,false,false),"peek cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,-1,50,false,false),"miss cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,4,50,false,false),"invalid slot cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,0,Double.NaN,false,false),"bad amount cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,0,50,true,false),"lock cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,0,50,false,true),"cancel cannot save");
        System.out.println("PASS: Quick Access four-edge targeting, deliberate arming, off/peek/miss/cancel/lock and invalid input guards");
    }
}

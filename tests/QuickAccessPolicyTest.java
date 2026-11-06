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
        check(QuickAccessPolicy.launchTarget(200,400,0,100,400,800)==0,"pull down launches top");
        check(QuickAccessPolicy.launchTarget(200,400,-100,0,400,800)==1,"pull left launches right");
        check(QuickAccessPolicy.launchTarget(200,400,0,-100,400,800)==2,"pull up launches bottom");
        check(QuickAccessPolicy.launchTarget(200,400,100,0,400,800)==3,"pull right launches left");
        check(QuickAccessPolicy.launchTarget(370,200,-80,-5,400,800)==1,"off-centre ray reaches correct edge");
        check(QuickAccessPolicy.launchTarget(200,400,0,0,400,800)==-1,"hold without pull cancels");
        check(QuickAccessPolicy.launchTarget(200,400,Float.NaN,5,400,800)==-1,"bad pull cancels");
        check(QuickAccessPolicy.carouselTarget(-192,10,48)==1,"upward slide reaches earlier tag");
        check(QuickAccessPolicy.carouselTarget(0,10,48)==5,"hold starts at middle tag");
        check(QuickAccessPolicy.carouselTarget(999,10,48)==9,"carousel clamps at last tag");
        check(QuickAccessPolicy.carouselTarget(0,0,48)==-1,"empty carousel cancels");
        check(QuickAccessPolicy.dismissTarget(200,704,400,800,1),"normal drag reaches bottom close");
        check(QuickAccessPolicy.dismissTarget(600,2112,1200,2400,3),"close hit testing scales with density");
        check(!QuickAccessPolicy.dismissTarget(200,400,400,800,1),"ordinary move does not dismiss");
        check(!QuickAccessPolicy.dismissTarget(200,704,400,800,0),"invalid screen cannot dismiss");
        check(QuickAccessPolicy.canSave(true,500,100,72,0,50,false,false),"deliberate hold and release");
        check(!QuickAccessPolicy.canSave(false,500,100,72,0,50,false,false),"default-off cannot save");
        check(!QuickAccessPolicy.canSave(true,399,100,72,0,50,false,false),"short tap cannot save");
        check(!QuickAccessPolicy.canSave(true,500,71,72,0,50,false,false),"peek cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,-1,50,false,false),"miss cannot save");
        check(QuickAccessPolicy.canSave(true,500,100,72,9,50,false,false),"tenth carousel tag can save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,10,50,false,false),"invalid slot cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,0,Double.NaN,false,false),"bad amount cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,0,50,true,false),"lock cannot save");
        check(!QuickAccessPolicy.canSave(true,500,100,72,0,50,false,true),"cancel cannot save");
        System.out.println("PASS: Quick Access four-edge targeting, deliberate arming, off/peek/miss/cancel/lock and invalid input guards");
    }
}


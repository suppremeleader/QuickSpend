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
        check(QuickAccessPolicy.carouselTarget(0,10,Float.NaN)==-1,"invalid step cancels");
        for(float height:new float[]{300,480,800})for(float density:new float[]{1,3})for(int count:new int[]{1,5,10})for(float fraction:new float[]{.05f,.5f,.95f}){
            float h=height*density,anchorY=h*fraction,step=QuickAccessPolicy.carouselStep(h,density,count);int origin=QuickAccessPolicy.carouselOrigin(count,h,anchorY,density);
            check(QuickAccessPolicy.carouselTarget(-10000,count,step,origin)==0,"first category reachable");check(QuickAccessPolicy.carouselTarget(10000,count,step,origin)==count-1,"last category reachable");
            for(int chosen=0;chosen<count;chosen++){check(QuickAccessPolicy.carouselTarget((chosen-origin)*step,count,step,origin)==chosen,"display and save indices match");float[] rail=QuickAccessPolicy.carouselRail(count,chosen,h,anchorY,density);int start=(int)rail[0],visible=(int)rail[1];check(chosen>=start&&chosen<start+visible,"selected tag remains visible");check(rail[3]>=22*density&&rail[3]+(visible-1)*rail[2]<=h-22*density,"whole rail stays on screen");check(visible==1||rail[2]>=44*density,"visible tags do not overlap");}
        }

        check(QuickAccessPolicy.dismissTarget(200,704,400,800,1),"normal drag reaches bottom close");
        check(QuickAccessPolicy.dismissTarget(600,2112,1200,2400,3),"close hit testing scales with density");
        check(!QuickAccessPolicy.dismissTarget(200,400,400,800,1),"ordinary move does not dismiss");
        check(!QuickAccessPolicy.dismissTarget(200,704,400,800,0),"invalid screen cannot dismiss");
        int level=QuickAccessPolicy.visibleOrigin(10,800,300,1);
        check(QuickAccessPolicy.slideTarget(-60,0,10,48,level,18,false)==level,"straight inward slide selects the aligned category");
        check(QuickAccessPolicy.slideTarget(-60,12,10,48,level,18,false)==level,"small vertical drift still selects the aligned category");
        check(QuickAccessPolicy.slideTarget(0,48,10,48,level,18,false)==level+1,"vertical slide still changes the category");
        check(QuickAccessPolicy.slideTarget(0,0,10,48,level,18,false)==-1,"hold alone cannot select");
        check(QuickAccessPolicy.cancelTarget(200,766,400,800,1),"cancel is under the bottom target");
        check(QuickAccessPolicy.slideTarget(-60,48,10,48,level,18,true)==-1,"cancel overrides a selected category");
        check(!QuickAccessPolicy.cancelTarget(200,700,400,800,1),"ordinary rail position cannot cancel");
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


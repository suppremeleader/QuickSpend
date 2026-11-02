import com.quickspend.app.CaptureWakePolicy;
import static com.quickspend.app.CaptureWakePolicy.Action.*;
public class CaptureWakePolicyTest {
    static void check(CaptureWakePolicy.Action actual,CaptureWakePolicy.Action expected){
        if(actual!=expected)throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args){
        // Payment while asleep; screen-on still locked; unlock with dead/alive service.
        check(CaptureWakePolicy.decide(true,true,true,false,true,false,false,false),WAIT_UNLOCK);
        check(CaptureWakePolicy.decide(true,true,true,false,true,true,false,false),WAIT_UNLOCK);
        check(CaptureWakePolicy.decide(true,true,true,false,false,true,false,false),START);
        check(CaptureWakePolicy.decide(true,true,true,false,false,true,false,true),REFRESH);
        check(CaptureWakePolicy.decide(true,true,true,false,false,true,true,true),WAIT_APP);
        check(CaptureWakePolicy.decide(true,true,true,true,false,true,false,false),STOP);
        check(CaptureWakePolicy.decide(false,true,true,false,false,true,false,false),STOP);
        check(CaptureWakePolicy.decide(true,false,true,false,false,true,false,false),STOP);
        check(CaptureWakePolicy.decide(true,true,false,false,false,true,false,false),STOP);
        System.out.println("Capture wake/unlock policy scenarios passed");
    }
}

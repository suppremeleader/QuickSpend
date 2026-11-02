import com.quickspend.app.CaptureSoundPolicy;
import java.util.Set;
public class CaptureSoundPolicyTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        Set<String> payment=Set.of("bank:1");
        check(CaptureSoundPolicy.announce(true,payment,Set.of()),"first visible payment announces");
        check(!CaptureSoundPolicy.announce(false,payment,Set.of()),"badge refresh does not announce");
        check(!CaptureSoundPolicy.announce(true,payment,payment),"recovery, rotation and popup close do not replay");
        check(CaptureSoundPolicy.announce(true,Set.of("bank:1","bank:2"),payment),"new payment appearance announces once");
        check(!CaptureSoundPolicy.announce(true,Set.of(),payment),"empty queue silent");
        check(CaptureSoundPolicy.audible(true,3,true),"normal notification volume permits cue");
        check(!CaptureSoundPolicy.audible(false,3,true),"silent and vibrate suppress cue");
        check(!CaptureSoundPolicy.audible(true,0,true),"muted notification stream suppresses cue");
        check(!CaptureSoundPolicy.audible(true,3,false),"DND suppresses cue");
        System.out.println("PASS: floater appearance/new-payment dedupe, refresh/recovery suppression and silent/volume/DND gates");
    }
}

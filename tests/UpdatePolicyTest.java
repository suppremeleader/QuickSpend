import com.quickspend.app.UpdatePolicy;
public class UpdatePolicyTest {
    private static void expect(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        expect(UpdatePolicy.notify(1036,1037,0,true));
        expect(!UpdatePolicy.notify(1037,1037,0,true));
        expect(!UpdatePolicy.notify(1038,1037,0,true));
        expect(!UpdatePolicy.notify(1036,1037,1037,true));
        expect(!UpdatePolicy.notify(1036,1037,0,false));
        expect(UpdatePolicy.version("1037")==1037);
        expect(UpdatePolicy.version("-1")==0&&UpdatePolicy.version("1.5")==0&&UpdatePolicy.version("bad")==0&&UpdatePolicy.version("2100000001")==0&&UpdatePolicy.version(null)==0);
        expect(!UpdatePolicy.prompt(1037,1037,10000,5000,false));
        expect(UpdatePolicy.prompt(1037,1037,86410000,5000,false));
        expect(UpdatePolicy.prompt(1038,1037,10000,5000,false));
        expect(UpdatePolicy.prompt(1037,1037,10000,5000,true));
        System.out.println("PASS: update versions, installed/duplicate/opt-out suppression, daily prompt cooldown and manual checks");
    }
}

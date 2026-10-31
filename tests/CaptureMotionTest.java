import com.quickspend.app.CaptureMotion;
public class CaptureMotionTest {
    public static void main(String[] args) {
        for (float[] direction : new float[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}}) {
            float x=200,y=400;
            float[] end=CaptureMotion.exit(x,y,direction[0]*200,direction[1]*200,400,800,70);
            if (!(end[0] <= -69 || end[0] >= 469 || end[1] <= -69 || end[1] >= 869)) throw new AssertionError("Did not leave screen");
            float cross=(end[0]-x)*direction[1]-(end[1]-y)*direction[0];
            if (Math.abs(cross)>.01) throw new AssertionError("Exit changed pull direction");
            if ((end[0]-x)*direction[0]+(end[1]-y)*direction[1]>=0) throw new AssertionError("Exit must oppose pull direction");
        }
        System.out.println("PASS: all eight pull directions exit the screen opposite the pull vector");
    }
}

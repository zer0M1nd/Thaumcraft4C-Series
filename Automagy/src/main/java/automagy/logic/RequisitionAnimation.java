package automagy.logic;

import java.util.function.IntUnaryOperator;

/** Legacy tick-based lift, opening and occasional page turn (not continuous flutter). */
public class RequisitionAnimation {
    public float open,page;
    public double bookY;
    private boolean goingUp=true;
    public boolean tick(boolean powered,IntUnaryOperator random){
        if(powered){
            if(open<1){open=Math.min(1,open+.02f);if(bookY<.1){bookY=retreat(bookY,.1,0,.18);goingUp=true;}}
            else if(goingUp){if(bookY>=.1)goingUp=false;else bookY=retreat(bookY,.1,0,.007);}
            else if(bookY<=0)goingUp=true;
            else bookY=retreat(bookY,0,.1,.007);
        }else if(open>0)open=Math.max(0,open-.02f);
        if(page>0){page+=.1f;if(page>1)page=powered&&open>=1&&random.applyAsInt(4)==0?.1f:0;}
        else if(open>=1&&random.applyAsInt(200)==0){page=.1f;return random.applyAsInt(16)==0;}
        return false;
    }
    private static double retreat(double value,double target,double origin,double ratio){
        return value+Math.signum(target-value)*Math.min(Math.abs(target-value),(Math.abs(origin-value)+.025)*ratio);
    }
}

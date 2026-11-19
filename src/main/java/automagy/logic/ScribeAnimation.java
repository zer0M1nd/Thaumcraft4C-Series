package automagy.logic;

import java.util.function.IntUnaryOperator;

/** Tick-based legacy quill travel, bobbing and occasional page turns. */
public class ScribeAnimation {
    public double quillY,quillHoriz,quillR,bookY=.005;
    public float page;
    private boolean quillUp,bookUp;
    public boolean tick(boolean powered,IntUnaryOperator random) {
        if(!powered) {
            if(quillHoriz<0) {quillHoriz=approach(quillHoriz,0,.01);if(quillY>=.1)quillY+=(.1-quillY)*.1;}
            else if(quillUp) {if(quillY>=.1)quillUp=false;else quillY=retreat(quillY,.1,.05,.01);}
            else if(quillY<=.05)quillY=retreat(quillY,0,.05,.02);
            else quillY=retreat(quillY,.05,.1,.01);
            if(quillR<0)quillR=approach(quillR,0,1);
        } else if(quillHoriz>-.5) {
            if(quillY<.15) {quillUp=true;quillY=approach(quillY,.15,.01);}
            quillHoriz=approach(quillHoriz,-.5,.01);
            if(quillY>=.025&&quillR>-20)quillR=approach(quillR,-20,.3);
        } else if(quillUp) {if(quillY>=.2)quillUp=false;else quillY=retreat(quillY,.2,.15,.01);}
        else if(quillY<=.15)quillUp=true;
        else quillY=retreat(quillY,.15,.2,.01);
        if(bookUp) {if(bookY>=.01)bookUp=false;else bookY=retreat(bookY,.01,0,.005);}
        else if(bookY<=0)bookUp=true;
        else bookY=retreat(bookY,0,.01,.005);
        if(page>0) {page+=.1f;if(page>1)page=powered&&random.applyAsInt(4)==0?.1f:0;}
        else if(powered&&random.applyAsInt(100)==0) {page=.1f;return random.applyAsInt(8)==0;}
        return false;
    }
    private static double approach(double a,double b,double step){return a+Math.signum(b-a)*Math.min(Math.abs(b-a),step);}
    private static double retreat(double a,double b,double from,double ratio){return approach(a,b,(Math.abs(from-a)+.025)*ratio);}
}

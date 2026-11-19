package wgmodern;
/** Deterministic parts of the original node transduction and selection mechanics. */
public class PrimordialRules {
 public static int nodeCharge(int amount,int modifier){double factor=modifier==0?1.2:modifier==1?.8:modifier==2?.5:1;return (int)Math.sqrt(Math.max(0,(int)(amount*factor)));}
 public static int radialSlot(double x,double y){double angle=Math.toDegrees(Math.atan2(x,-y));if(angle<0)angle+=360;return angle>288?0:angle<72?1:Math.min(4,2+(int)((288-angle)/72));}
 public static int reducedDuration(int duration,int orderPieces){return duration<0?duration:duration/(Math.max(0,orderPieces)+1);}
 public static int reducedAmplifier(int amplifier,int orderPieces){return Math.max(0,amplifier-Math.max(0,orderPieces));}
}

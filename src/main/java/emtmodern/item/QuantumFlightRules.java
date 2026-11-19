package emtmodern.item;

/** Numeric rules isolated from player mutation so height and low-charge edges are testable. */
public class QuantumFlightRules {
    public static float jetpackPower(double charge,double y,int ceiling){
        float power=charge/30000<=.05f?(float)(charge/30000*.05f):1;
        if(y>ceiling-25)power*=((ceiling-Math.min(y,ceiling))/25);return power;
    }
    public static double jetpackVertical(double previous,float power,boolean hover,boolean jump,boolean sneak){
        double vertical=Math.min(previous+power*.2f,.6f);
        if(hover){float limit=jump?.1f:sneak?-.1f:-.025f;if(vertical>limit){vertical=limit;if(previous>vertical)vertical=previous;}}
        return vertical;
    }
}

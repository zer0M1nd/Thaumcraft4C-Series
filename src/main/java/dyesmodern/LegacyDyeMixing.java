package dyesmodern;

import java.util.List;

/** 1.7.10 EntitySheep.fleeceColorTable, indexed by modern DyeColor/wool metadata. */
public final class LegacyDyeMixing {
    private static final float[][] COLORS={{1,1,1},{.85f,.5f,.2f},{.7f,.3f,.85f},{.4f,.6f,.85f},{.9f,.9f,.2f},{.5f,.8f,.1f},{.95f,.5f,.65f},{.3f,.3f,.3f},{.6f,.6f,.6f},{.3f,.5f,.6f},{.5f,.25f,.7f},{.2f,.3f,.7f},{.4f,.3f,.2f},{.4f,.5f,.2f},{.6f,.2f,.2f},{.1f,.1f,.1f}};
    private LegacyDyeMixing(){}
    public static int mix(int existing,List<Integer> colors){
        if(colors.isEmpty())throw new IllegalArgumentException("At least one dye is required");
        int r=existing>>16&255,g=existing>>8&255,b=existing&255,brightness=Math.max(r,Math.max(g,b)),count=1;
        for(int color:colors){if(color<0||color>=16)throw new IllegalArgumentException("Invalid dye color");var rgb=COLORS[color];int cr=(int)(rgb[0]*255),cg=(int)(rgb[1]*255),cb=(int)(rgb[2]*255);r+=cr;g+=cg;b+=cb;brightness+=Math.max(cr,Math.max(cg,cb));count++;}
        r/=count;g/=count;b/=count;float target=brightness/count,max=Math.max(r,Math.max(g,b));
        if(max==0)return 0;
        r=(int)(r*target/max);g=(int)(g*target/max);b=(int)(b*target/max);
        return r<<16|g<<8|b;
    }
}

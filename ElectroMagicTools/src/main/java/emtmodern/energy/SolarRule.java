package emtmodern.energy;

/** Original author 1.2.6.2 generation rules, separated from the world for verification. */
public class SolarRule {
    public static final java.util.List<String> KINDS=java.util.List.of("compressed","water","dark","order","fire","air","earth");
    public static double production(String kind,double base,boolean sky,boolean day,boolean rain,boolean biomeRain,boolean ceiling,boolean nether,int height){
        // In the original fire implementation, checkConditions never calls createEnergy outside the Nether.
        if(kind.equals("fire"))return nether?base*2:0;
        if(ceiling||!sky)return 0;
        if(kind.equals("water"))return rain?base:0;
        if(kind.equals("dark"))return !day&&!rain?base:0;
        if(!day||rain&&biomeRain)return 0;
        return base*(kind.equals("order")?3:kind.equals("air")&&height>=160||kind.equals("earth")&&height<=10?2.5:1);
    }
}

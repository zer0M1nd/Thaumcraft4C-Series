package emtmodern.item;

import java.util.function.IntUnaryOperator;

/** Exact original 1.2.6.2 thresholds and ordered, independent random draws. */
public class OneRingRules {
    public static String effect(int corruption,IntUnaryOperator random){
        if(corruption<=0)return "warning";
        else if(corruption>6000&&corruption<24000&&random.applyAsInt(2000)==0)return "blindness";
        else if(corruption>=6000&&corruption<24000&&random.applyAsInt(2000)==0)return "nausea";
        else if(corruption>=24000&&corruption<72000&&random.applyAsInt(2000)==0)return "magic";
        else if(corruption>=72000&&corruption<120000&&random.applyAsInt(4000)==0)return "launch";
        else if(corruption>=120000&&random.applyAsInt(10000)==0)return "wither";
        else if(corruption>=Integer.MAX_VALUE-100)return "death";
        return "none";
    }
    public static boolean removable(int corruption){return corruption>600;}
}

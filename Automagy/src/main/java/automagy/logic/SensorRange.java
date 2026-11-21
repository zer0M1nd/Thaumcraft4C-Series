package automagy.logic;

/** A blocked side redirects its five blocks of reach to the opposite open side. */
public final class SensorRange {
    public record Span(int minimum,int maximum){}
    public static Span axis(boolean negativeBlocked,boolean positiveBlocked){
        int minimum=negativeBlocked?0:-5,maximum=positiveBlocked?1:6;
        if(positiveBlocked&&!negativeBlocked)minimum-=5;
        if(negativeBlocked&&!positiveBlocked)maximum+=5;
        return new Span(minimum,maximum);
    }
    private SensorRange(){}
}

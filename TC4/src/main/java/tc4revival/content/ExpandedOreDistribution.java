package tc4revival.content;

/** Per-column volume weighting preserves the old expected attempts per eligible Y layer. */
public class ExpandedOreDistribution {
    public record Band(int minimum,int maximumExclusive,int originalSpan) {
        public Band {if(originalSpan<=0)throw new IllegalArgumentException("Original span must be positive");}
        public int span(){return Math.max(0,maximumExclusive-minimum);}
        public double weight(){return (double)span()/originalSpan;}
    }
    private final Band[] columns;
    private final double[] cumulative;
    private final double total;
    private final double expectedAttempts;
    public ExpandedOreDistribution(Band[] columns,int originalAttempts){
        this(columns,originalAttempts,1);
    }
    public ExpandedOreDistribution(Band[] columns,int originalAttempts,double multiplier){
        if(columns.length==0||originalAttempts<0)throw new IllegalArgumentException("Invalid generation columns");
        if(!Double.isFinite(multiplier)||multiplier<0)throw new IllegalArgumentException("Invalid ore density multiplier");
        this.columns=columns.clone();cumulative=new double[columns.length];double sum=0;
        for(int i=0;i<columns.length;i++)cumulative[i]=sum+=columns[i].weight();
        total=sum;expectedAttempts=originalAttempts*total/columns.length*multiplier;
    }
    public static Band cinnabar(int minimum,int maximum){return new Band(minimum,Math.min(maximum,51),51);}
    public static Band amber(int minimum,int maximum,int surface){return new Band(Math.max(minimum,surface-48),Math.min(maximum,surface+1),49);}
    public static Band infused(int minimum,int maximum,int surface){int span=Math.max(5,surface-5);return new Band(minimum,Math.min(maximum,span),span);}
    public Band band(int column){return columns[column];}
    public double expectedAttempts(){return expectedAttempts;}
    public int attempts(double roundRoll){int whole=(int)Math.floor(expectedAttempts);return whole+(roundRoll<expectedAttempts-whole?1:0);}
    public int column(double roll){
        if(total<=0)throw new IllegalStateException("No eligible ore height");
        double target=roll*total;int low=0,high=cumulative.length-1;
        while(low<high){int mid=(low+high)>>>1;if(cumulative[mid]<=target)low=mid+1;else high=mid;}
        return low;
    }
}

package tc4revival.content;
/** Non-overlapping gauge segment sizes in pixels. */
public record SanityGauge(int gap,int temporary,int sticky,boolean saturated) {
    public static SanityGauge of(int permanent,int sticky,int temporary) {
        float total=(float)((long)permanent+sticky+temporary),scale=total>100?100/total:1;
        return new SanityGauge((int)((100-Math.min(total,100))/100*48),(int)(temporary/100f*48*scale),(int)(sticky/100f*48*scale),total>=100);
    }
}

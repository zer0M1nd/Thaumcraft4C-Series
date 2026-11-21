package tc4revival.client;

/** Defers a middle-button action until release; small pointer jitter remains a click. */
public class ResearchMiddleGesture {
    private final double x,y;
    private boolean dragged;
    public ResearchMiddleGesture(double x,double y) { this.x=x;this.y=y; }
    public boolean moved(double x,double y) {
        dragged |= (x-this.x)*(x-this.x)+(y-this.y)*(y-this.y)>9;
        return dragged;
    }
    public boolean click(double x,double y) { return !moved(x,y); }
}

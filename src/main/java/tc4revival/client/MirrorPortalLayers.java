package tc4revival.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Texture-matrix and colour parameters shared by all six old mirror orientations. */
public  class MirrorPortalLayers {
    public record Layer(int index,float depth,float scale,float brightness,float red,float green,float blue,float degrees,boolean additive) {}
    private static final List<Layer> LAYERS=create();
    private MirrorPortalLayers() {}
    public static List<Layer> layers() { return LAYERS; }
    private static List<Layer> create() {
        var random=new Random(31100L); var layers=new ArrayList<Layer>(16);
        for (int index=0;index<16;index++) {
            float depth=index==0 ? 65 : 16-index;
            float scale=index==0 ? .125f : index==1 ? .5f : .0625f;
            float brightness=index==0 ? .1f : 1f/(17-index);
            float red=random.nextFloat()*.5f+.1f,green=random.nextFloat()*.5f+.4f,blue=random.nextFloat()*.5f+.5f;
            if (index==0) { red=1; green=1; blue=1; }
            layers.add(new Layer(index,depth,scale,brightness,red*brightness,green*brightness,blue*brightness,
                    (float)(index*index*4321+index*9)*2,index>0));
        }
        return List.copyOf(layers);
    }
    public static float scroll(long milliseconds) { return (float)(milliseconds%700000L)/250000f; }
    public record Projection(double q,double translateS,double translateT) {}
    /** EYE_LINEAR Q after the legacy temporary model-view translation; inputs are camera-relative. */
    public static Projection projection(Layer layer,double plane,double eyeNormal,double eyeS,double eyeT,double cameraS,double cameraT,boolean positivePlane) {
        double distance=positivePlane ? plane-eyeNormal : eyeNormal-plane;
        double denominator=distance+layer.depth();
        if (!Double.isFinite(distance) || !Double.isFinite(denominator) || Math.abs(distance)<1e-7 || Math.abs(denominator)<1e-7) return null;
        double q=-distance/denominator;
        double s=-cameraS+eyeS*layer.depth()/distance,t=-cameraT+eyeT*layer.depth()/distance;
        return Double.isFinite(q) && Double.isFinite(s) && Double.isFinite(t) ? new Projection(q,s,t) : null;
    }
    /** Applies the old texture matrix to already generated homogeneous S/T/Q coordinates. */
    public static double[] textureCoordinates(Layer layer,double s,double t,double q,double translateS,double translateT,long milliseconds) {
        double x=s+(translateS-.5)*q,y=t+(translateT-.5)*q;
        double angle=Math.toRadians(layer.degrees()),cos=Math.cos(angle),sin=Math.sin(angle);
        double u=layer.scale()*(cos*x-sin*y+.5*q);
        double v=layer.scale()*(sin*x+cos*y+.5*q)+scroll(milliseconds)*q;
        return new double[]{u,v,q};
    }
}

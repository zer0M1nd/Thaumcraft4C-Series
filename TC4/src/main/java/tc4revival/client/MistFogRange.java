package tc4revival.client;

/** A finite linear approximation of the legacy exponential mist, compatible with vanilla shaders. */
public record MistFogRange(float near,float far) {
    public static MistFogRange apply(float near,float far,float density) {
        if(density<=0)return new MistFogRange(near,far);
        float end=Math.min(far,3f/density);
        return new MistFogRange(Math.min(near,end*.05f),end);
    }
}

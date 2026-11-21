package thaumicfutures.foci;

/** TC5 excludes both the clicked block and the endpoint at the player's coordinate. */
public final class BuilderLayout {
    public static int length(double blockCenter,double playerAxis,int enlarge) {
        if(!Double.isFinite(blockCenter) || !Double.isFinite(playerAxis))return 0;
        return Math.max(0,(int)Math.min(Math.abs(blockCenter-playerAxis),16+Math.max(0,Math.min(5,enlarge))*8)-1);
    }
    private BuilderLayout() {}
}

package conciliummodern;

/** Bounds shared by NBT decoding, gem binding, and dimensional travel. */
final class GemRules {
    static final int SAMPLE_LIMIT=16;
    static final double WORLD_LIMIT=29999984;
    static int sampleCount(int saved){return Math.max(0,Math.min(SAMPLE_LIMIT,saved));}
    static int selected(int saved,int count){return count<=0?0:Math.floorMod(saved,count);}
    static boolean canBind(int rifts,int earth,int others,int stored,boolean captured){
        return rifts==1&&earth==1&&others==0&&stored>=0&&stored<SAMPLE_LIMIT&&captured;
    }
    static double coordinate(double source,boolean entering,boolean reverse){return source*(entering?1:reverse?.1:10);}
    static boolean validCoordinate(double coordinate){return Double.isFinite(coordinate)&&Math.abs(coordinate)<=WORLD_LIMIT;}
    private GemRules(){}
}

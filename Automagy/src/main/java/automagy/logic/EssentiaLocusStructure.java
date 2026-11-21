package automagy.logic;

/** Relative positions are measured from the floating locus, not the pool center. */
public final class EssentiaLocusStructure {
    public enum Cell { UNKNOWN, AIR, SOURCE_WATER, FLOWING_WATER, LAPIS, SILVERWOOD, WATER_CLUSTER, AGGREGATOR, OTHER }
    public enum Validity { VALID, INVALID, UNLOADED }
    @FunctionalInterface public interface View {Cell at(int x,int y,int z);}
    public record Capacity(int jars,int eyes,int distance,boolean loaded) {}
    private static final int[][] DIRECTIONS={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    public static Validity validate(View view,boolean placement){
        boolean missing=false;
        for(int y=-2;y<=0;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            boolean center=x==0&&z==0,inner=Math.abs(x)<=1&&Math.abs(z)<=1;
            boolean rim=y==-2&&(Math.abs(x)==2&&Math.abs(z)<=1||Math.abs(z)==2&&Math.abs(x)<=1);
            if(!(y==-2&&(inner||rim)||y>=-1&&inner&&(!center||y==-1)))continue;
            var cell=view.at(x,y,z);if(cell==Cell.UNKNOWN){missing=true;continue;}
            boolean valid;
            if(rim)valid=cell==Cell.SILVERWOOD;
            else if(y==-2)valid=center?cell==Cell.LAPIS:cell==Cell.SOURCE_WATER||!placement&&cell==Cell.FLOWING_WATER;
            else if(center)valid=placement?cell==Cell.WATER_CLUSTER:cell==Cell.AIR||cell==Cell.AGGREGATOR;
            else valid=cell==Cell.AIR;
            if(!valid)return Validity.INVALID;
        }
        return missing?Validity.UNLOADED:Validity.VALID;
    }
    public static Capacity capacity(View view){
        int clusters=0;boolean loaded=true;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2&&Math.abs(z)<=1||Math.abs(z)==2&&Math.abs(x)<=1)
            for(var d:DIRECTIONS){var cell=view.at(x+d[0],-2+d[1],z+d[2]);if(cell==Cell.UNKNOWN)loaded=false;else if(cell==Cell.WATER_CLUSTER)clusters++;}
        // The original counts cluster contacts per rim plank, including shared corner contacts.
        return new Capacity(12+8*clusters,Math.min(40,1+clusters),20+8*clusters,loaded);
    }
    private EssentiaLocusStructure(){}
}

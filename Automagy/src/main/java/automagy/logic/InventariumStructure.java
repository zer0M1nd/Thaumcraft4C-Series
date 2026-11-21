package automagy.logic;

/** Relative-coordinate structure rules. Unknown chunks are distinct from missing blocks. */
public final class InventariumStructure {
    public record Cell(boolean loaded, boolean air, boolean amber, boolean orderCrystal,
                       int shelfSlots, boolean memoryBlock) {
        public Cell { shelfSlots=Math.max(0,shelfSlots); }
    }
    @FunctionalInterface public interface View { Cell at(int x,int y,int z); }
    public enum Validity { VALID, INVALID, UNLOADED }
    public record Capacity(int slots,int nodes,int distance,int memories,boolean loaded) {}
    private static final int[][] SHELVES={{-2,-1},{-2,1},{2,-1},{2,1},{-1,-2},{1,-2},{-1,2},{1,2}};
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};

    public static Validity validate(View view,boolean placement){
        // Resolve all required cells first: an unloaded edge must never destroy a core.
        boolean valid=true,loaded=true;
        Cell center=view.at(0,-1,0),base=view.at(0,-2,0);
        loaded&=center.loaded&&base.loaded;
        valid&=(placement?center.orderCrystal:center.air)&&base.amber;
        for(int y=-1;y<=0;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            if(x==0&&z==0)continue;
            Cell c=view.at(x,y,z);loaded&=c.loaded;valid&=c.air;
        }
        for(int[] shelf:SHELVES){Cell c=view.at(shelf[0],-1,shelf[1]);loaded&=c.loaded;valid&=c.shelfSlots>0;}
        return !loaded?Validity.UNLOADED:valid?Validity.VALID:Validity.INVALID;
    }

    public static Capacity capacity(View view){
        long slots=0;int clusters=0,memories=0;boolean loaded=true;
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){
            Cell c=view.at(x,y,z);loaded&=c.loaded;if(!c.loaded)continue;
            if(c.shelfSlots>0){slots+=c.shelfSlots;continue;}
            if(c.orderCrystal){clusters++;continue;}
            if(y>-2||!c.memoryBlock)continue;
            boolean amber=false,neighborsLoaded=true;
            for(int[] d:DIRECTIONS){Cell n=view.at(x+d[0],y+d[1],z+d[2]);neighborsLoaded&=n.loaded;amber|=n.loaded&&n.amber;}
            if(amber)memories++;else loaded&=neighborsLoaded;
        }
        return new Capacity((int)Math.min(Integer.MAX_VALUE,slots),1+clusters,10+8*clusters,memories,loaded);
    }
    public static boolean inRange(int dx,int dy,int dz,int distance){
        return distance>=0&&(double)dx*dx+(double)dy*dy+(double)dz*dz<=(double)distance*distance;
    }
    private InventariumStructure(){}
}

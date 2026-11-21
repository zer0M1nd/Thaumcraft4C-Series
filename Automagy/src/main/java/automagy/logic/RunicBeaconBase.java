package automagy.logic;

/** Legacy runes support only the first (3x3) tier; deeper tiers retain the vanilla tag rule. */
public final class RunicBeaconBase {
    public record Cell(boolean loaded,boolean ordinary,boolean rune) {}
    @FunctionalInterface public interface View { Cell at(int x,int depth,int z); }
    /** -1 means this is not a runic base and the original beacon result must be preserved. */
    public static int levels(View view){
        boolean runic=false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var cell=view.at(x,1,z);
            if(!cell.loaded()||!cell.ordinary()&&!cell.rune())return -1;
            runic|=cell.rune();
        }
        if(!runic)return -1;
        for(int depth=2;depth<=4;depth++)for(int x=-depth;x<=depth;x++)for(int z=-depth;z<=depth;z++){
            var cell=view.at(x,depth,z);
            if(!cell.loaded()||!cell.ordinary())return depth-1;
        }
        return 4;
    }
    private RunicBeaconBase() {}
}

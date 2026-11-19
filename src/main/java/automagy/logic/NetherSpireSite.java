package automagy.logic;
/** Looks for three complete lava layers followed by thirty completely empty layers. */
public final class NetherSpireSite {
    public interface Column {int minimumY();int maximumY();boolean lavaLayer(int y);boolean airLayer(int y);}
    private NetherSpireSite() {}
    public static java.util.OptionalInt find(Column world){
        int consecutive=0;
        for(int y=world.minimumY()+1;y+30<=world.maximumY();y++){
            if(world.lavaLayer(y)){consecutive++;continue;}
            if(consecutive>=3){boolean clear=true;for(int above=0;above<30;above++)if(!world.airLayer(y+above)){clear=false;break;}if(clear)return java.util.OptionalInt.of(y);}
            consecutive=0;
        }return java.util.OptionalInt.empty();
    }
}

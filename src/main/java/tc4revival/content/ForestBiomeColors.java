package tc4revival.content;

import net.minecraft.world.level.biome.Biome;

/** Client-published palette; this hook can also load safely on a dedicated server. */
public  class ForestBiomeColors {
    private record Palette(Biome forest,boolean blue) {}
    private static volatile Palette palette=new Palette(null,false);
    private ForestBiomeColors() {}
    public static boolean update(Biome forest,boolean blue) {
        var previous=palette;
        if (previous.forest()==forest && previous.blue()==blue) return false;
        palette=new Palette(forest,blue); return true;
    }
    public static int grass(int original,Biome biome) {
        var current=palette;
        return current.blue() && current.forest()==biome ? 0x66AACC : original;
    }
    public static int foliage(int original,Biome biome) {
        var current=palette;
        return current.blue() && current.forest()==biome ? 0x77CCEE : original;
    }
}

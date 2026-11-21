package technomancy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Player-owned state, retained across death by PlayerEvents. */
public final class PlayerPower {
    public enum Affinity { EARTH, FIRE, WATER, LIGHT, DARK, NORMAL }
    public static CompoundTag data(Player player) {
        var root=player.getPersistentData();
        if(!root.contains(Technomancy.ID)) root.put(Technomancy.ID,new CompoundTag());
        var data=root.getCompound(Technomancy.ID);
        if(!data.contains("level")) {data.putInt("level",1);data.putInt("power",1);}
        for(var a:Affinity.values())if(a!=Affinity.NORMAL&&!data.contains(a.name()))data.putInt(a.name(),1);
        return data;
    }
    public static void train(Player player,int count) {grow(player,"level","levelProgress",100,count);}
    public static void affinity(Player player,Affinity affinity,int count) {
        if(affinity!=Affinity.NORMAL)grow(player,affinity.name(),affinity.name()+"Progress",10,count);
    }
    private static void grow(Player p,String key,String progress,int scale,int count) {
        var d=data(p);
        for(int i=0;i<count;i++) {
            long bound=Math.max(1L,(long)Math.max(1,d.getInt(key))*scale-d.getInt(progress));
            if(p.getRandom().nextInt((int)Math.min(Integer.MAX_VALUE,bound))==0) {
                d.putInt(key,Math.min(1000000,d.getInt(key)+1));d.putInt(progress,0);
            }else d.putInt(progress,d.getInt(progress)+1);
        }
    }
    public static Affinity affinity(Player p) {
        var d=data(p);if(d.getInt("level")<20)return Affinity.NORMAL;
        int total=0;for(var a:Affinity.values())if(a!=Affinity.NORMAL)total+=d.getInt(a.name());
        for(var a:Affinity.values())if(a!=Affinity.NORMAL&&d.getInt(a.name())>total/2)return a;
        return Affinity.NORMAL;
    }
    private PlayerPower(){}
}

package thaumcraft.api;

public interface IRepairableExtended extends IRepairable {
    boolean doRepair(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player,int level);
    @Override default boolean beforeVisRepair(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player,int level) { return doRepair(stack,player,level); }
}

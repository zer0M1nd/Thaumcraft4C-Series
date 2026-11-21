package thaumcraft.api;

public interface IWarpingGear extends tc4revival.content.WarpingGear {
    int getWarp(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player);
    @Override default int warp(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player) { return getWarp(stack,player); }
}

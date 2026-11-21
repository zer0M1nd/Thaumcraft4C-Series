package thaumcraft.api.wands;

public interface IWandable extends tc4revival.content.WandInteractable {
    @Override default net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context) { return net.minecraft.world.InteractionResult.PASS; }
    @Override default java.util.Optional<net.minecraft.world.item.ItemStack> onWandUse(net.minecraft.world.level.Level level,net.minecraft.world.item.ItemStack wand,net.minecraft.world.entity.player.Player player) { return java.util.Optional.empty(); }
    @Override default void onWandUseTick(net.minecraft.world.item.ItemStack wand,net.minecraft.world.entity.player.Player player,int remaining) {}
    @Override default void onWandRelease(net.minecraft.world.item.ItemStack wand,net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,int remaining) {}
}

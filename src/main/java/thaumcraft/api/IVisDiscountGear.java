package thaumcraft.api;

public interface IVisDiscountGear extends tc4revival.content.VisDiscountGear {
    int getVisDiscount(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player,thaumcraft.api.aspects.Aspect aspect);
    @Override default int visDiscount(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player,tc4revival.aspect.Aspect aspect) { return getVisDiscount(stack,player,thaumcraft.api.aspects.Aspect.wrap(aspect)); }
}

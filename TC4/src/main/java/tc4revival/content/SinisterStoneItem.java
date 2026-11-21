package tc4revival.content;
import net.minecraft.world.item.*;
public  class SinisterStoneItem extends Item implements WarpingGear {
    public SinisterStoneItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    public static int legacyMetadata(ItemStack stack) { return stack.hasTag()&&stack.getTag().getBoolean("LegacyActive")?1:0; }
    @Override public int warp(ItemStack stack,net.minecraft.world.entity.player.Player wearer) { return legacyMetadata(stack)==0?1:0; }
    @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected) {
        if(level.isClientSide)tc4revival.client.SinisterStoneView.update(level,entity);
    }
}

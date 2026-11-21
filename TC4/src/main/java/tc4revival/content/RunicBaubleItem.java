package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public  class RunicBaubleItem extends Item implements ICurioItem,RunicUpgradeGear {
    private final RunicBaubleType type;
    public RunicBaubleItem(RunicBaubleType type) { super(new Properties().stacksTo(1).rarity(type==RunicBaubleType.LESSER_RING ? Rarity.UNCOMMON : Rarity.RARE)); this.type=type; }
    public RunicBaubleType type() { return type; }
    @Override public int intrinsicRunicCharge(ItemStack stack) { return type.charge(); }
    @Override public Upgrade runicUpgrade(ItemStack stack) { return type.upgrade(); }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material) { return false; }
    @Override public void onEquip(SlotContext context,ItemStack previous,ItemStack stack) { dirty(context); }
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack) { dirty(context); }
    private static void dirty(SlotContext context) { if (context.entity() instanceof net.minecraft.server.level.ServerPlayer player) RunicShieldEvents.equipmentChanged(player); }
}

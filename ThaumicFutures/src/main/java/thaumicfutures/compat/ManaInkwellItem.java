package thaumicfutures.compat;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import thaumcraft.api.IScribeTools;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaItem;

/** Legacy 150 uses, 50 mana per ink unit, recharged only from a mana pool. */
public final class ManaInkwellItem extends Item implements IScribeTools {
    public static final int CAPACITY = 7500;
    public ManaInkwellItem() { super(new Properties().stacksTo(1)); }
    public static int mana(ItemStack stack) { return stack.hasTag() ? Math.max(0, Math.min(CAPACITY, stack.getTag().getInt("Mana"))) : 0; }
    private static void setMana(ItemStack stack, long amount) { stack.getOrCreateTag().putInt("Mana", (int) Math.max(0, Math.min(CAPACITY, amount))); }
    @Override public int getInk(ItemStack stack) { return thaumicfutures.FutureFeatures.Group.BOTANIA.enabled() ? mana(stack) / 50 : 0; }
    @Override public void setInk(ItemStack stack, int ink) {
        if (!thaumicfutures.FutureFeatures.Group.BOTANIA.enabled()) return;
        // The scribing API consumes whole ink units; retain fractional mana between uses.
        setMana(stack, (long) mana(stack) + ((long) Math.max(0, Math.min(150, ink)) - getInk(stack)) * 50);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return mana(stack) < CAPACITY; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * mana(stack) / CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return 0x36C8E8; }
    @Override public int getEntityLifespan(ItemStack stack, net.minecraft.world.level.Level level) { return Integer.MAX_VALUE; }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        LazyOptional<ManaItem> capability = LazyOptional.of(() -> new ManaItem() {
            @Override public int getMana() { return mana(stack); }
            @Override public int getMaxMana() { return CAPACITY; }
            @Override public void addMana(int amount) { if(thaumicfutures.FutureFeatures.Group.BOTANIA.enabled()) setMana(stack, (long) mana(stack) + amount); }
            @Override public boolean canReceiveManaFromPool(BlockEntity pool) { return thaumicfutures.FutureFeatures.Group.BOTANIA.enabled(); }
            @Override public boolean canReceiveManaFromItem(ItemStack other) { return false; }
            @Override public boolean canExportManaToPool(BlockEntity pool) { return false; }
            @Override public boolean canExportManaToItem(ItemStack other) { return false; }
            @Override public boolean isNoExport() { return true; }
        });
        return new ICapabilityProvider() {
            @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                return cap == BotaniaForgeCapabilities.MANA_ITEM ? capability.cast() : LazyOptional.empty();
            }
        };
    }
}

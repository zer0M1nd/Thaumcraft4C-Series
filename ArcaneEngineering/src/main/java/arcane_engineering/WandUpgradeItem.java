package arcane_engineering;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

public class WandUpgradeItem extends Item implements IUpgrade {
    public final String key;
    public WandUpgradeItem(String key) { super(new Properties().stacksTo(1)); this.key = key; }
    @Override public Set<String> getUpgradeTypes(ItemStack stack) { return Set.of("WAND"); }
    @Override public boolean canApplyUpgrades(ItemStack target, ItemStack upgrade) { return true; }
    @Override public void applyUpgrades(ItemStack target, ItemStack upgrade, CompoundTag modifications) { modifications.putInt(key, modifications.getInt(key)+1); }
}

package thaumicomniverse.compat.backpack;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import tc4revival.content.ArcaneWorkbenchBlockEntity;

public final class ArcaneCraftingUpgradeItem extends UpgradeItemBase<ArcaneCraftingUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    public ArcaneCraftingUpgradeItem() { super(Config.SERVER.maxUpgradesPerStorage); }
    @Override public UpgradeType<Wrapper> getType() { return TYPE; }
    @Override public List<UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getUpgradesPerStorage(String storageType) { return 1; }
    public static final class Wrapper extends UpgradeWrapperBase<Wrapper, ArcaneCraftingUpgradeItem> {
        private final ItemStackHandler inventory;
        @Override public boolean isEnabled() { return thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled() && super.isEnabled(); }
        public Wrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) {
            super(storage, upgrade, save);
            inventory = new ItemStackHandler(10) {
                @Override public boolean isItemValid(int slot, ItemStack stack) {
                    return slot < 9 || ArcaneWorkbenchBlockEntity.validWand(stack);
                }
                @Override public int getSlotLimit(int slot) { return slot == 9 ? 1 : 64; }
                @Override protected void onContentsChanged(int slot) { persist(); }
            };
            if (upgrade.hasTag() && upgrade.getTag().contains("OmniverseArcaneGrid"))
                inventory.deserializeNBT(upgrade.getTag().getCompound("OmniverseArcaneGrid"));
        }
        public ItemStackHandler inventory() { return inventory; }
        public net.minecraftforge.items.IItemHandlerModifiable storage() { return storageWrapper.getInventoryHandler(); }
        public boolean setting(String key) {
            var tag = upgrade.getOrCreateTag(); return tag.contains(key) ? tag.getBoolean(key) : key.equals("intoStorage");
        }
        public void setting(String key, boolean value) {
            if (!List.of("intoStorage", "refill").contains(key)) return;
            upgrade.getOrCreateTag().putBoolean(key, value); save();
        }
        public void persist() { upgrade.getOrCreateTag().put("OmniverseArcaneGrid", inventory.serializeNBT()); save(); }
        @Override public boolean canBeDisabled() { return false; }
    }
}

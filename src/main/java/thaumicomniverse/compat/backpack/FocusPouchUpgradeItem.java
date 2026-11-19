package thaumicomniverse.compat.backpack;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import tc4revival.content.WandFocusItem;
import thaumcraft.api.wands.FocusStorageRegistry;

public final class FocusPouchUpgradeItem extends UpgradeItemBase<FocusPouchUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    public FocusPouchUpgradeItem() { super(Config.SERVER.maxUpgradesPerStorage); }
    @Override public UpgradeType<Wrapper> getType() { return TYPE; }
    @Override public List<UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getUpgradesPerStorage(String storageType) { return 1; }

    public static final class Wrapper extends UpgradeWrapperBase<Wrapper, FocusPouchUpgradeItem>
            implements FocusStorageRegistry.Storage {
        private static final String HOMES = "OmniverseFocusHomes";
        @Override public boolean isEnabled() { return thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled() && super.isEnabled(); }
        public Wrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) { super(storage, upgrade, save); }
        @Override public boolean hideSettingsTab() { return true; }
        @Override public int slots() { return Math.min(4096, storageWrapper.getInventoryHandler().getSlots()); }
        @Override public ItemStack get(int slot) { return storageWrapper.getInventoryHandler().getStackInSlot(slot).copy(); }
        @Override public void set(int slot, ItemStack value) {
            var previous = get(slot);
            if (value.isEmpty() && previous.getItem() instanceof WandFocusItem focus) {
                var root = upgrade.getOrCreateTag();
                var homes = root.getCompound(HOMES);
                homes.putInt(focus.sortingKey(previous), slot);
                root.put(HOMES, homes);
                save();
            }
            if (value.getItem() instanceof WandFocusItem focus) {
                var root = upgrade.getOrCreateTag();
                var homes = root.getCompound(HOMES);
                homes.remove(focus.sortingKey(value));
                root.put(HOMES, homes);
                save();
            }
            storageWrapper.getInventoryHandler().setStackInSlot(slot, value.copy());
        }
        @Override public net.minecraft.nbt.CompoundTag checkpoint(){return upgrade.hasTag()?upgrade.getTag().copy():new net.minecraft.nbt.CompoundTag();}
        @Override public void restoreCheckpoint(net.minecraft.nbt.CompoundTag state){upgrade.setTag(state.copy());save();}
        @Override public int preferredReturnSlot(ItemStack value){return preferredSlot(value);}
        public int preferredSlot(ItemStack value) {
            if (!(value.getItem() instanceof WandFocusItem focus)) return -1;
            var homes = upgrade.hasTag()?upgrade.getTag().getCompound(HOMES):new net.minecraft.nbt.CompoundTag();
            String key = focus.sortingKey(value);
            if (!homes.contains(key)) return -1;
            int slot = homes.getInt(key);
            return slot >= 0 && slot < slots() && get(slot).isEmpty() && accepts(slot, value) ? slot : -1;
        }
        @Override public boolean accepts(int slot, ItemStack value) {
            if (value.isEmpty()) return true;
            if (!FocusStorageRegistry.Storage.super.accepts(slot, value)
                    || !storageWrapper.getInventoryHandler().isItemValid(slot, value)) return false;
            var homes = upgrade.hasTag()?upgrade.getTag().getCompound(HOMES):new net.minecraft.nbt.CompoundTag();
            var key = ((WandFocusItem) value.getItem()).sortingKey(value);
            if (!homes.contains(key)) return true;
            int home = homes.getInt(key);
            // Reject earlier free slots only while the original slot remains usable.
            return home == slot || home < 0 || home >= slots() || !get(home).isEmpty()
                    || !storageWrapper.getInventoryHandler().isItemValid(home, value);
        }
    }
}

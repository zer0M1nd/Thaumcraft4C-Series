package thaumicomniverse.compat.backpack;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;
import thaumcraft.api.aspects.IEssentiaStorage;

public final class EssentiaTankUpgradeItem extends UpgradeItemBase<EssentiaTankUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    public EssentiaTankUpgradeItem() { super(Config.SERVER.maxUpgradesPerStorage); }
    @Override public UpgradeType<Wrapper> getType() { return TYPE; }
    @Override public List<UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getUpgradesPerStorage(String storageType) { return 1; }
    @Override public int getInventoryColumnsTaken() { return 2; }
    public int baseCapacity(IStorageWrapper storage) { return getInventoryColumnsTaken()*storage.getNumberOfSlotRows()*8; }
    public static double stackRatio() { return Config.SERVER.tankUpgrade.stackMultiplierRatio.get(); }
    public int tankCapacity(IStorageWrapper storage,double stackMultiplier) {
        return (int)Math.min(Integer.MAX_VALUE,baseCapacity(storage)*(1+stackRatio()*(stackMultiplier-1)));
    }
    @Override public net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult checkExtraInsertConditions(
            ItemStack stack,IStorageWrapper storage,boolean client,IUpgradeItem<?> replaced) {
        double multiplier=storage.getInventoryHandler().getStackSizeMultiplier();
        if(replaced instanceof net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem upgrade)multiplier/=upgrade.getStackSizeMultiplier();
        int amount=stack.getOrCreateTag().getCompound(Wrapper.DATA).getInt("Amount");
        if(amount>tankCapacity(storage,multiplier))return new net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult.Fail(
                net.minecraft.network.chat.Component.translatable("item.thaumicomniverse.essentia_tank_upgrade.too_full"),java.util.Set.of(),java.util.Set.of(),java.util.Set.of());
        return new net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult.Success();
    }

    public static Wrapper find(IStorageWrapper storage) {
        return storage.getUpgradeHandler().getTypeWrappers(TYPE).stream().filter(Wrapper::isEnabled).findFirst().orElse(null);
    }

    public static final class Wrapper extends UpgradeWrapperBase<Wrapper, EssentiaTankUpgradeItem>
            implements IEssentiaStorage, ITickableUpgrade, IStackableContentsUpgrade {
        private static final String DATA = "OmniverseEssentia";
        private int reserved;
        @Override public boolean isEnabled() { return thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled() && super.isEnabled(); }
        public Wrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) { super(storage, upgrade, save); }
        public Aspect aspect() { return TCAspects.catalog().find(upgrade.getOrCreateTag().getCompound(DATA).getString("Aspect")).orElse(null); }
        public int amount() { return aspect() == null ? 0 : Math.max(0, upgrade.getOrCreateTag().getCompound(DATA).getInt("Amount")); }
        public int tankCapacity() { return upgradeItem.tankCapacity(storageWrapper,storageWrapper.getInventoryHandler().getStackSizeMultiplier()); }
        @Override public boolean canBeDisabled() { return false; }
        @Override public int getMinimumMultiplierRequired() {
            double base=upgradeItem.baseCapacity(storageWrapper),ratio=stackRatio();
            if(amount()<=base||ratio<=0)return 1;
            return (int)Math.min(Integer.MAX_VALUE,Math.ceil(1+(amount()/base-1)/ratio));
        }
        @Override public AspectAmounts contents() {
            var aspect = aspect(); int amount = amount();
            return aspect == null || amount == 0 ? new AspectAmounts() : new AspectAmounts().add(aspect, amount);
        }
        @Override public int capacity(Aspect aspect) { return aspect != null && (amount() == 0 || aspect.equals(aspect())) ? tankCapacity() : 0; }
        private void store(Aspect aspect, int amount) {
            var data = new net.minecraft.nbt.CompoundTag();
            if (amount > 0) { data.putString("Aspect", aspect.tag()); data.putInt("Amount", amount); }
            upgrade.getOrCreateTag().put(DATA, data);
            save();
        }
        @Override public int insert(Aspect aspect, int amount, boolean simulate) {
            if (amount < 0) throw new IllegalArgumentException("Negative essentia");
            if(!thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled())return 0;
            int accepted = Math.min(amount, Math.max(0, capacity(aspect) - amount() - reserved));
            if (accepted > 0 && !simulate) store(aspect, amount() + accepted);
            return accepted;
        }
        @Override public int extract(Aspect aspect, int amount, boolean simulate) {
            if (amount < 0) throw new IllegalArgumentException("Negative essentia");
            int taken = aspect != null && aspect.equals(aspect()) ? Math.min(amount, amount()) : 0;
            if (taken > 0 && !simulate) store(aspect, amount() - taken);
            return taken;
        }
        @Override public void tick(Entity holder, Level level, BlockPos pos) {
            if(!isEnabled())return;
            if (holder != null || level.isClientSide || level.getGameTime() % 5 != 0 || amount() >= tankCapacity()) return;
            for (var side : Direction.values()) {
                var adjacent = pos.relative(side);
                if (!level.hasChunkAt(adjacent)) continue;
                if (!(level.getBlockEntity(adjacent) instanceof EssentiaTransport source)) continue;
                var face = side.getOpposite();
                if (!source.connectable(face) || !source.canOutput(face) || source.suction(face) >= 32 || source.minimumSuction() > 32) continue;
                var aspect = source.essentiaType(face);
                if (aspect == null || insert(aspect, 1, true) == 0) continue;
                int taken;
                reserved++;
                try { taken = source.takeEssentia(aspect, 1, face); }
                finally { reserved--; }
                if (taken > 0) {
                    insert(aspect, taken, false);
                    changed(level, pos);
                }
                if (amount() >= tankCapacity()) break;
            }
        }
        public static void changed(Level level, BlockPos pos) {
            if (level == null || level.isClientSide) return;
            var owner = level.getBlockEntity(pos);
            if (owner != null) {
                owner.setChanged();
                thaumcraft.api.aspects.EssentiaStorageApi.changed(owner);
                level.sendBlockUpdated(pos, owner.getBlockState(), owner.getBlockState(), 2);
            }
        }
    }
}

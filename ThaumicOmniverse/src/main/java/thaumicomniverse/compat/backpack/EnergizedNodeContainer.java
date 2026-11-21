package thaumicomniverse.compat.backpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import tc4revival.aspect.AspectAmounts;

public final class EnergizedNodeContainer extends UpgradeContainerBase<EnergizedNodeUpgradeItem.Wrapper, EnergizedNodeContainer> {
    public static final UpgradeContainerType<EnergizedNodeUpgradeItem.Wrapper, EnergizedNodeContainer> TYPE = new UpgradeContainerType<>(EnergizedNodeContainer::new);
    public EnergizedNodeContainer(Player player, int id, EnergizedNodeUpgradeItem.Wrapper wrapper,
                                  UpgradeContainerType<EnergizedNodeUpgradeItem.Wrapper, EnergizedNodeContainer> type) { super(player, id, wrapper, type); }
    public AspectAmounts rates() { return upgradeWrapper.rates(); }
    @Override public void handleMessage(CompoundTag data) {}
}

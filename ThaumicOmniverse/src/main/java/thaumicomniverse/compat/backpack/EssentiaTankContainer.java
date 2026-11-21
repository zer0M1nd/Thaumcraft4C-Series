package thaumicomniverse.compat.backpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import tc4revival.aspect.Aspect;

public final class EssentiaTankContainer extends UpgradeContainerBase<EssentiaTankUpgradeItem.Wrapper, EssentiaTankContainer> {
    public static final UpgradeContainerType<EssentiaTankUpgradeItem.Wrapper, EssentiaTankContainer> TYPE = new UpgradeContainerType<>(EssentiaTankContainer::new);
    public EssentiaTankContainer(Player player, int id, EssentiaTankUpgradeItem.Wrapper wrapper,
                                  UpgradeContainerType<EssentiaTankUpgradeItem.Wrapper, EssentiaTankContainer> type) {
        super(player, id, wrapper, type);
    }
    @Override public void handleMessage(CompoundTag data) {}
    public Aspect aspect() { return upgradeWrapper.aspect(); }
    public int amount() { return upgradeWrapper.amount(); }
    public int capacity() { return upgradeWrapper.tankCapacity(); }
}

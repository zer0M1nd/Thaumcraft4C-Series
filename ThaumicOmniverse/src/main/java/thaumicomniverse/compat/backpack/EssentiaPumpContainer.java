package thaumicomniverse.compat.backpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;

public final class EssentiaPumpContainer extends UpgradeContainerBase<EssentiaPumpUpgradeItem.Wrapper, EssentiaPumpContainer> {
    public static final UpgradeContainerType<EssentiaPumpUpgradeItem.Wrapper, EssentiaPumpContainer> TYPE = new UpgradeContainerType<>(EssentiaPumpContainer::new);
    public EssentiaPumpContainer(Player player, int id, EssentiaPumpUpgradeItem.Wrapper wrapper,
                                  UpgradeContainerType<EssentiaPumpUpgradeItem.Wrapper, EssentiaPumpContainer> type) { super(player, id, wrapper, type); }
    public boolean setting(String key) { return upgradeWrapper.setting(key); }
    public void setting(String key, boolean value) { upgradeWrapper.setting(key, value); sendBooleanToServer(key, value); }
    public long pending() { return upgradeWrapper.pending().total(); }
    @Override public void handleMessage(CompoundTag data) {
        for (String key : java.util.List.of("input", "hand", "adjacent")) if (data.contains(key)) setting(key, data.getBoolean(key));
    }
}

package thaumicomniverse.compat.backpack;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.EssentiaStorageApi;

public final class EssentiaPumpUpgradeItem extends UpgradeItemBase<EssentiaPumpUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    public EssentiaPumpUpgradeItem() { super(Config.SERVER.maxUpgradesPerStorage); }
    @Override public UpgradeType<Wrapper> getType() { return TYPE; }
    @Override public List<UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }
    @Override public int getUpgradesPerStorage(String storageType) { return 1; }

    public static final class Wrapper extends UpgradeWrapperBase<Wrapper, EssentiaPumpUpgradeItem> implements ITickableUpgrade {
        @Override public boolean isEnabled() { return thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled() && super.isEnabled(); }
        public Wrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> save) { super(storage, upgrade, save); }
        public boolean setting(String key) { var tag = upgrade.getOrCreateTag(); return !tag.contains(key) || tag.getBoolean(key); }
        public void setting(String key, boolean value) {
            if (!List.of("input", "hand", "adjacent").contains(key)) return;
            upgrade.getOrCreateTag().putBoolean(key, value); save();
        }
        public AspectAmounts pending() { return AspectNbt.read(upgrade.getOrCreateTag(), "OmniversePending", TCAspects.catalog()).amounts(); }
        private void pending(AspectAmounts amounts) { AspectNbt.write(upgrade.getOrCreateTag(), "OmniversePending", amounts); save(); }
        @Override public void tick(Entity holder, Level level, BlockPos pos) {
            if(!isEnabled())return;
            if (!(level instanceof ServerLevel server) || isInCooldown(level)) return;
            setCooldown(level, 20);
            var tank = EssentiaTankUpgradeItem.find(storageWrapper);
            if (tank == null) return;
            // A destination may change after simulation. Keep any unreturnable remainder on this upgrade.
            var buffer = pending();
            if (!buffer.isEmpty()) {
                for (var aspect : buffer.aspects()) buffer.reduce(aspect, tank.insert(aspect, buffer.amount(aspect), false));
                var retained = new AspectAmounts();
                for (var aspect : buffer.aspects()) if (buffer.amount(aspect) > 0) retained.add(aspect, buffer.amount(aspect));
                pending(retained);
                if (holder == null) EssentiaTankUpgradeItem.Wrapper.changed(level, pos);
                if (!retained.isEmpty()) return;
            }
            if (setting("hand")) {
                if (holder instanceof Player player) {
                    if (HandEssentia.transfer(player, tank, setting("input"))) return;
                } else if (holder == null) {
                    for (var player : server.getEntitiesOfClass(Player.class,
                            new net.minecraft.world.phys.AABB(pos).inflate(3),
                            player -> !player.isSpectator() && server.mayInteract(player, pos))) {
                        if (HandEssentia.transfer(player, tank, setting("input"))) {
                            EssentiaTankUpgradeItem.Wrapper.changed(level, pos);
                            return;
                        }
                    }
                }
            }
            if (!setting("adjacent")) return;
            for (var face : Direction.values()) {
                var target = pos.relative(face);
                if (!server.hasChunkAt(target)) continue;
                if (holder instanceof Player player && !server.mayInteract(player, target)) continue;
                var external = EssentiaStorageApi.at(server, target, face.getOpposite());
                var source = setting("input") ? external : tank;
                var destination = setting("input") ? tank : external;
                for (var aspect : source.contents().aspects()) {
                    var moved = EssentiaStorageApi.transfer(source, destination, aspect, 8);
                    if (moved.remainder() > 0) pending(new AspectAmounts().add(aspect, moved.remainder()));
                    if (moved.inserted() > 0 || moved.remainder() > 0) {
                        if (holder == null) EssentiaTankUpgradeItem.Wrapper.changed(level, pos);
                        return;
                    }
                }
            }
        }
    }
}

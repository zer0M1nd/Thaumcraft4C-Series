package thaumicomniverse.content;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "thaumicomniverse")
public final class ObeliskCompassItem extends Item {
    private record Session(ServerLevel level, int boss, ObeliskSearch search, long completedAt) {}
    private static final Map<UUID, Session> SEARCHES = new HashMap<>();
    private static final java.util.LinkedHashSet<UUID> PENDING = new java.util.LinkedHashSet<>();
    private final boolean primal;
    public ObeliskCompassItem(boolean primal) { super(new Properties().stacksTo(1).rarity(primal ? Rarity.EPIC : Rarity.RARE)); this.primal = primal; }
    public static int boss(ItemStack stack) {
        return stack.hasTag() ? Math.max(-1, Math.min(3, stack.getTag().getInt("Boss") - 1)) : -1;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!thaumicomniverse.RevivalFeatures.Group.COMPASSES.enabled() || !primal) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.getOrCreateTag().putInt("Boss", (boss(stack) + 2) % 5);
            stack.getOrCreateTag().remove("Target"); SEARCHES.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("omniverse.compass.mode", bossName(boss(stack))), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    public static Component bossName(int boss) {
        return Component.translatable(switch (boss) { case 0 -> "tc.boss.golem"; case 1 -> "tc.boss.warden"; case 2 -> "tc.boss.crimson"; case 3 -> "tc.boss.taint"; default -> "omniverse.compass.any"; });
    }
    @Override public void inventoryTick(ItemStack stack, Level world, Entity holder, int slot, boolean selected) {
        if (!thaumicomniverse.RevivalFeatures.Group.COMPASSES.enabled())return;
        if (!(world instanceof ServerLevel level) || !(holder instanceof ServerPlayer player)
                || !(selected || player.getOffhandItem() == stack && !(player.getMainHandItem().getItem() instanceof ObeliskCompassItem))) return;
        int mode = primal ? boss(stack) : -1;
        var session = SEARCHES.get(player.getUUID());
        BlockPos previous = session == null ? null : session.search().result();
        boolean conquered = previous != null && ObeliskClaims.get(level.getServer()).conquered(previous.getX() >> 4, previous.getZ() >> 4);
        if (session == null || session.level() != level || session.boss() != mode || conquered
                || session.search().complete() && (level.getGameTime() - session.completedAt() >= 600
                || session.search().origin.distSqr(player.blockPosition()) > 1024)) {
            session = new Session(level, mode, new ObeliskSearch(level, player.blockPosition(), mode), level.getGameTime());
            SEARCHES.put(player.getUUID(), session);
            stack.getOrCreateTag().remove("Target");
        }
        if (!session.search().complete()) PENDING.add(player.getUUID());
        if (level.getGameTime() % 20 != 0) return;
        BlockPos target = session.search().complete() ? session.search().result() : null;
        if (target == null) {
            player.displayClientMessage(Component.translatable(session.search().complete() ? "omniverse.compass.none" : "omniverse.compass.searching"), true);
        } else {
            stack.getOrCreateTag().putLong("Target", target.asLong());
            stack.getOrCreateTag().putString("Dimension", level.dimension().location().toString());
            long distance = Math.round(Math.hypot(target.getX() - player.getX(), target.getZ() - player.getZ()));
            player.displayClientMessage(Component.translatable("omniverse.compass.distance", distance, bossName(mode)), true);
        }
    }
    public static GlobalPos target(ItemStack stack) {
        if (!thaumicomniverse.RevivalFeatures.Group.COMPASSES.enabled() || !stack.hasTag() || !stack.getTag().contains("Target")) return null;
        var id = ResourceLocation.tryParse(stack.getTag().getString("Dimension"));
        return id == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, id), BlockPos.of(stack.getTag().getLong("Target")));
    }
    /** One candidate per server tick, shared fairly by all compass holders and dimensions. */
    @SubscribeEvent public static void searchTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (!thaumicomniverse.RevivalFeatures.Group.COMPASSES.enabled()) { SEARCHES.clear(); PENDING.clear(); return; }
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || PENDING.isEmpty()) return;
        UUID id = PENDING.iterator().next();
        PENDING.remove(id);
        var session = SEARCHES.get(id);
        if (session == null || session.search().complete()) return;
        var player = session.level().getServer().getPlayerList().getPlayer(id);
        if (player == null || player.level() != session.level()
                || !(player.getMainHandItem().getItem() instanceof ObeliskCompassItem)
                && !(player.getOffhandItem().getItem() instanceof ObeliskCompassItem)) return;
        session.search().step();
        if (session.search().complete()) SEARCHES.put(id, new Session(session.level(), session.boss(), session.search(), session.level().getGameTime()));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SEARCHES.clear(); PENDING.clear(); }
    @SubscribeEvent public static void loggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SEARCHES.remove(event.getEntity().getUUID()); PENDING.remove(event.getEntity().getUUID());
    }
}

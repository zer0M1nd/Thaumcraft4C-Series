package thaumicomniverse.content;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.EldritchMazeSavedData;
import tc4revival.content.TCItems;

/** A maze is conquered when its reward-bearing boss is defeated, independent of who loots it. */
@Mod.EventBusSubscriber(modid = "thaumicomniverse")
public final class ObeliskClaims extends SavedData {
    private static final String HOME = "thaumicomniverse:ObeliskHome";
    private final Set<Long> conquered = new HashSet<>();
    public static ObeliskClaims get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ObeliskClaims::load, ObeliskClaims::new, "thaumicomniverse_obelisks");
    }
    public boolean conquered(int x, int z) { return conquered.contains(ChunkPos.asLong(x, z)); }
    public void conquer(int x, int z) { if (conquered.add(ChunkPos.asLong(x, z))) setDirty(); }
    public static ObeliskClaims load(CompoundTag tag) {
        var data = new ObeliskClaims();
        for (long pos : tag.getLongArray("Conquered")) data.conquered.add(pos);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Conquered", conquered.stream().mapToLong(Long::longValue).sorted().toArray());
        return tag;
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void rememberOrigin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !EldritchMazeSavedData.isMazeDimension(level.dimension())
                || !(event.getEntity() instanceof net.minecraft.world.entity.LivingEntity)
                || !net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).getNamespace().equals("thaumcraft")
                || event.getEntity().getPersistentData().contains(HOME)) return;
        var home = planAt(level, event.getEntity().blockPosition());
        if (home != null) event.getEntity().getPersistentData().putLong(HOME, ChunkPos.asLong(home.centerX(), home.centerZ()));
    }
    private static EldritchMazeSavedData.Plan planAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        int roomX = pos.getX() >> 4, roomZ = pos.getZ() >> 4;
        for (var plan : EldritchMazeSavedData.get(level).plans()) {
            int x = roomX - plan.originX(), z = roomZ - plan.originZ();
            if (x >= 0 && z >= 0 && x < plan.layout().width() && z < plan.layout().height()
                    && plan.layout().cell(x, z).packed() != 0) return plan;
        }
        return null;
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void bossDefeated(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)
                || event.getDrops().stream().noneMatch(drop -> drop.getItem().is(TCItems.PRIMORDIAL_PEARL.get()))) return;
        if (event.getEntity().getPersistentData().contains(HOME)) {
            long home = event.getEntity().getPersistentData().getLong(HOME);
            get(level.getServer()).conquer(ChunkPos.getX(home), ChunkPos.getZ(home));
        } else if (EldritchMazeSavedData.isMazeDimension(level.dimension())) {
            var plan = planAt(level, event.getEntity().blockPosition());
            if (plan != null) get(level.getServer()).conquer(plan.centerX(), plan.centerZ());
        }
    }
}

package automagy.content;

import automagy.Automagy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.KnowledgeSavedData;
import thaumcraft.api.research.ScanEvent;

/** Discovery requires both observation and a player kill, in either order. */
@Mod.EventBusSubscriber(modid=Automagy.ID)
public final class NetherResearchEvents {
    private NetherResearchEvents() {}
    private static String family(Entity entity) {
        return switch(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString()) {
            case "minecraft:zombified_piglin" -> "ZOMBIE";
            case "minecraft:ghast" -> "GHAST";
            case "minecraft:skeleton", "minecraft:wither_skeleton" -> "SKELETON";
            case "minecraft:magma_cube" -> "SLIME";
            case "thaumcraft:fire_bat" -> "BAT";
            case "automagy:nether_wisp" -> "WISP";
            default -> null;
        };
    }
    private static void record(ServerPlayer player,Entity entity,String action) {
        String family=family(entity); if(family==null)return;
        var data=KnowledgeSavedData.get(player.server);
        String prefix="automagy:nether_observation/"+family+"/";
        if(data.snapshot(player.getUUID()).map(k->k.researchMarkers().contains(prefix+action)).orElse(false))return;
        data.update(player.getUUID(),player.getRandom()::nextInt,k->{
            k.rememberResearch(prefix+action);
            if(k.researchMarkers().contains(prefix+"scan")&&k.researchMarkers().contains(prefix+"kill"))
                k.rememberResearch("@automagy:NETHERRUNE_"+family);
            return null;
        });
    }
    @SubscribeEvent public static void scan(ScanEvent event) {
        if(event.getScanResult().succeeded() && event.getTarget().continuity() instanceof tc4revival.research.ScanTarget.Entity target) {
            Entity entity=event.getPlayer().serverLevel().getEntity(target.uuid());
            if(entity!=null)record(event.getPlayer(),entity,"scan");
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(event.getSource().getEntity() instanceof ServerPlayer player)record(player,event.getEntity(),"kill");
    }
}

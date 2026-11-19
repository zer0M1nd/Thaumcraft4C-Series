package tc4revival.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Boss-room placement restriction from the old world event handler. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class EldritchPlacementProtection {
    private EldritchPlacementProtection() {}
    // EntityMultiPlaceEvent inherits this event; inspect its primary position just as the old handler did.
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level) || !EldritchMazeSavedData.isMazeDimension(level.dimension())
                || !(event.getEntity() instanceof Player player) || player.getAbilities().instabuild) return;
        var pos=event.getPos();
        var cell=EldritchMazeSavedData.get(level).cell(pos.getX()>>4,pos.getZ()>>4);
        if(cell.isEmpty() || cell.get().feature()<2 || cell.get().feature()>5) return;
        // The legacy utility queried a cube centered at integer block coordinates, not a sphere.
        var area=new AABB(pos.getX()-32,pos.getY()-32,pos.getZ()-32,pos.getX()+32,pos.getY()+32,pos.getZ()+32);
        if(!level.getEntitiesOfClass(ThaumcraftBossEntity.class,area).isEmpty()) event.setCanceled(true);
    }
}

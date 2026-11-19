package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.network.ResearchCompletionPacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.ResearchCatalogs;

public  class EldritchPortalBlockEntity extends BlockEntity {
    private int ticks,opening=-1;
    public EldritchPortalBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.PORTAL_ENTITY.get(),pos,state); }
    public int opening() { return opening; }
    public void tick() {
        ticks++;
        if (level==null) return;
        if (level.isClientSide) {
            if (opening<30) opening++;
            if (ticks%250==0) level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,
                    TCSounds.EVIL_PORTAL.get(),SoundSource.BLOCKS,1,1,false);
            return;
        }
        if (!(level instanceof ServerLevel server) || ticks%5!=0) return;
        for (ServerPlayer player:server.getEntitiesOfClass(ServerPlayer.class,new AABB(worldPosition).inflate(.5,1,.5))) {
            if (player.isPassenger() || player.isVehicle()) continue;
            boolean cooling=player.isOnPortalCooldown(); player.setPortalCooldown(100);
            if (cooling) continue;
            boolean entering=!server.dimension().equals(TCEldritch.OUTER_LANDS);
            ServerLevel destination=server.getServer().getLevel(entering ? TCEldritch.OUTER_LANDS : Level.OVERWORLD);
            if (destination==null || !EldritchPortalTravel.transfer(player,destination)) continue;
            if (entering) KnowledgeSavedData.get(server.getServer()).update(player.getUUID(),server.random::nextInt,
                    knowledge->ResearchCatalogs.progression().completeTrusted(knowledge,"ENTEROUTER",tc4revival.config.TCConfig.SERVER.disableWarp.get()))
                    .ifPresent(result->TCNetwork.send(player,ResearchCompletionPacket.from(result)));
        }
    }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1); }
}

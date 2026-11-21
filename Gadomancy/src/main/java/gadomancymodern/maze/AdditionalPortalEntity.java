package gadomancymodern.maze;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public class AdditionalPortalEntity extends BlockEntity {
    protected int ticks,opening;
    public AdditionalPortalEntity(BlockPos pos,BlockState state){super(Gadomancy.PORTAL_BE.get(),pos,state);}
    public int opening(){return opening;}
    public void tick(){if(level==null)return;ticks++;if(level.isClientSide){opening=Math.min(30,opening+1);if(ticks%250==0)level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,tc4revival.content.TCSounds.EVIL_PORTAL.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1,false);return;}if(ticks%5!=0||!(level instanceof ServerLevel server))return;for(var player:server.getEntitiesOfClass(ServerPlayer.class,new AABB(worldPosition).inflate(.5,1,.5))){if(player.isOnPortalCooldown()||player.isPassenger()||player.isVehicle()||!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"gadomancy:e_portal_creator"))continue;if(server.dimension().equals(MazeSessions.DIMENSION))MazeSessions.close(player,true);else MazeSessions.begin(player,worldPosition);break;}}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(1);}
}

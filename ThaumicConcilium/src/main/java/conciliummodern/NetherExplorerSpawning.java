package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class NetherExplorerSpawning {
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || !level.dimension().equals(Level.NETHER)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.getGameTime()%Math.max(1,ConciliumMobConfig.NETHER_EXPLORER_RATE.get())!=0)return;
        var chunks=new java.util.HashSet<ChunkPos>();
        for(var player:level.players())if(!player.isSpectator()){
            var center=player.chunkPosition();for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)chunks.add(new ChunkPos(center.x+x,center.z+z));
        }
        if(chunks.isEmpty())return;
        int count=0;for(var entity:level.getAllEntities())if(entity instanceof NetherExplorer && entity.isAlive() && ++count>3)return;
        var ordered=new java.util.ArrayList<>(chunks);
        for(int i=ordered.size()-1;i>0;i--)java.util.Collections.swap(ordered,i,level.random.nextInt(i+1));
        for(var chunk:ordered){
            if(!level.hasChunk(chunk.x,chunk.z))continue;
            int x=chunk.getMinBlockX()+level.random.nextInt(16),z=chunk.getMinBlockZ()+level.random.nextInt(16);
            int y=level.getMinBuildHeight()+level.random.nextInt(Math.min(level.getHeight(),level.dimensionType().logicalHeight()));
            for(int attempt=0;attempt<4;attempt++){
                x+=level.random.nextInt(6)-level.random.nextInt(6);z+=level.random.nextInt(6)-level.random.nextInt(6);var pos=new BlockPos(x,y,z);
                if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                        || !level.getFluidState(pos).is(FluidTags.LAVA) || level.getNearestPlayer(x+.5,y,z+.5,24,false)!=null
                        || pos.distSqr(level.getSharedSpawnPos())<576)continue;
                var explorer=ConciliumEntities.NETHER_EXPLORER.get().create(level);if(explorer==null)return;
                explorer.moveTo(x+.5,y,z+.5,level.random.nextFloat()*360,0);
                if(!level.noCollision(explorer) || !net.minecraftforge.event.ForgeEventFactory.checkSpawnPosition(explorer,level,MobSpawnType.NATURAL))continue;
                explorer.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null,null);
                if(level.addFreshEntity(explorer))return;
            }
        }
    }
    private NetherExplorerSpawning(){}
}

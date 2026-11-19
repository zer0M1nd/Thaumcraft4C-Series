package conciliummodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class PredictabilityStructure {
    private static final ThreadLocal<Boolean> CHANGING=ThreadLocal.withInitial(()->false);
    public static boolean loaded(Level level,BlockPos center){for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))if(!level.hasChunkAt(pos))return false;return true;}
    public static boolean complete(Level level,BlockPos center){
        if(!loaded(level,center))return false;
        for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))
            if(!(level.getBlockEntity(pos) instanceof PredictabilityBlockEntity tile) || !tile.center().equals(center))return false;
        return true;
    }
    public static void restore(Level level,BlockPos center,BlockPos removed){
        if(CHANGING.get())return;CHANGING.set(true);
        try{
            for(var cursor:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))){
                var pos=cursor.immutable();if(!level.hasChunkAt(pos))continue;
                boolean member=level.getBlockEntity(pos) instanceof PredictabilityBlockEntity tile && tile.center().equals(center);
                if(pos.equals(removed)){
                    // A removed ring member drops its original stone. The bedrock center is restored in place.
                    if(pos.equals(center) && level.isEmptyBlock(pos))level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),3);
                }else if(member)level.setBlock(pos,(pos.equals(center)?Blocks.BEDROCK:TCEldritch.STONE.get()).defaultBlockState(),3);
            }
        }finally{CHANGING.remove();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void use(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getItemStack().getItem() instanceof CastingWandItem wand) || event.getUseBlock()==Event.Result.DENY)return;
        var pos=event.getPos();var stack=event.getItemStack();
        if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,event.getFace(),stack))return;
        if(level.getBlockState(pos).is(Blocks.BEDROCK)){
            if(!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:HEXOFPREDICTABILITY") || !loaded(level,pos))return;
            var snapshots=new ArrayList<BlockSnapshot>();
            for(var cursor:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,0,1))){
                var at=cursor.immutable();if(!level.getWorldBorder().isWithinBounds(at) || !level.mayInteract(player,at) || !player.mayUseItemAt(at,Direction.UP,stack)
                        || level.getBlockEntity(at)!=null || !level.getBlockState(at).is(at.equals(pos)?Blocks.BEDROCK:TCEldritch.STONE.get()))return;
                snapshots.add(BlockSnapshot.create(level.dimension(),level,at));
            }
            var costs=Map.of(tc4revival.aspect.TCAspects.catalog().require("ordo"),100,tc4revival.aspect.TCAspects.catalog().require("terra"),100);
            if(!wand.consumeForPlayer(stack,player,costs,true,true,false))return;
            boolean accepted=false;CHANGING.set(true);
            try{
                for(var snapshot:snapshots){
                    if(!level.setBlock(snapshot.getPos(),ConciliumBlocks.HEX.get().defaultBlockState(),2|16))return;
                    if(!(level.getBlockEntity(snapshot.getPos()) instanceof PredictabilityBlockEntity tile))return;
                    tile.configure(pos);
                }
                if(net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(player,snapshots,Direction.UP))return;
                if(!complete(level,pos) || !wand.consumeForPlayer(stack,player,costs,true,true,true))return;
                accepted=true;
            }finally{
                if(!accepted){boolean old=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;try{for(var snapshot:snapshots)snapshot.restore(true,false);}finally{level.restoringBlockSnapshots=old;}}
                CHANGING.remove();
            }
            for(var snapshot:snapshots){level.updateNeighborsAt(snapshot.getPos(),ConciliumBlocks.HEX.get());tc4revival.network.FocusEffectPacket.blockSparkle(level,snapshot.getPos(),-9999);}
            level.playSound(null,pos,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);success(event);
        }else if(level.getBlockEntity(pos) instanceof NodeJarBlockEntity
                && level.getBlockEntity(pos.below()) instanceof PredictabilityBlockEntity tile && tile.isMaster()
                && complete(level,tile.center()) && tile.rift()==null){
            if(!level.getEntitiesOfClass(RiftEntity.class,new net.minecraft.world.phys.AABB(tile.center()).inflate(2,4,2)).isEmpty())return;
            if(!level.getEntitiesOfClass(MaterialPeeler.class,new net.minecraft.world.phys.AABB(tile.center()).inflate(2,4,2)).isEmpty())return;
            if(net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1)return;
            var rift=ConciliumEntities.RIFT.get().create(level);if(rift==null)return;
            rift.setPos(tile.center().getX()+.5,tile.center().getY()+2,tile.center().getZ()+.5);rift.initialize(level.random.nextInt(),25);rift.bind(tile.center());
            if(!level.addFreshEntity(rift))return;
            if(!level.removeBlock(pos,false)){rift.discard();return;}
            success(event);
        }
    }
    private static void success(PlayerInteractEvent.RightClickBlock event){event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);}
    private PredictabilityStructure(){}
}

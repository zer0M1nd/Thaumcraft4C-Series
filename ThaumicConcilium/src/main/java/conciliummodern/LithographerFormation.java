package conciliummodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class LithographerFormation {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void use(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || event.getUseBlock()==Event.Result.DENY
                || !(event.getItemStack().getItem() instanceof CastingWandItem wand) || !level.getBlockState(event.getPos()).is(TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get())
                || !thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:LITHOGRAPHER"))return;
        var top=event.getPos();var center=top.below();var bed=center.below();
        if(!level.hasChunkAt(bed) || !level.getBlockState(center).is(TCArcane.THAUMIUM_BLOCK.get()) || !(level.getBlockState(bed).getBlock() instanceof BedBlock)
                || level.getBlockState(bed).getValue(BedBlock.PART)!=BedPart.HEAD)return;
        for(var direction:new Direction[]{Direction.EAST,Direction.SOUTH}){
            var positions=List.of(top,center,center.relative(direction),center.relative(direction.getOpposite()),bed.relative(direction),bed.relative(direction.getOpposite()));
            boolean valid=true;var snapshots=new ArrayList<BlockSnapshot>();
            for(int i=0;i<positions.size();i++){
                var pos=positions.get(i);if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player,pos)
                        || !player.mayUseItemAt(pos,Direction.UP,event.getItemStack()) || level.getBlockEntity(pos)!=null || (i>=2 && !level.getBlockState(pos).is(Blocks.GOLD_BLOCK))){valid=false;break;}
                snapshots.add(BlockSnapshot.create(level.dimension(),level,pos));
            }
            if(!valid)continue;
            var costs=new LinkedHashMap<tc4revival.aspect.Aspect,Integer>();for(String key:new String[]{"ignis","ordo","aqua","aer"})costs.put(tc4revival.aspect.TCAspects.catalog().require(key),key.equals("aqua")?50:100);
            if(!wand.consumeForPlayer(event.getItemStack(),player,costs,true,true,false))return;
            boolean accepted=false;
            try{
                for(var pos:positions)if(!level.setBlock(pos,(pos.equals(center)?ConciliumBlocks.LITHOGRAPHER.get().defaultBlockState().setValue(LithographerBlock.AXIS,direction.getAxis()):Blocks.AIR.defaultBlockState()),2|16))return;
                if(net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(player,snapshots,Direction.UP))return;
                if(!(level.getBlockEntity(center) instanceof LithographerBlockEntity) || !wand.consumeForPlayer(event.getItemStack(),player,costs,true,true,true))return;
                accepted=true;
            }finally{if(!accepted){boolean old=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;try{for(var snapshot:snapshots)snapshot.restore(true,false);}finally{level.restoringBlockSnapshots=old;}}}
            for(var pos:positions){level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());tc4revival.network.FocusEffectPacket.blockSparkle(level,pos,-9999);}
            level.playSound(null,center,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);return;
        }
    }
    private LithographerFormation(){}
}

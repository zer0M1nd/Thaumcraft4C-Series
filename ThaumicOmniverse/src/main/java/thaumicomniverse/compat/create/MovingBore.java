package thaumicomniverse.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

public class MovingBore implements MovementBehaviour {
    @Override public void tick(MovementContext context){
        if(!ArcaneContraptions.enabled()||!(context.world instanceof ServerLevel level)||context.position==null||context.blockEntityData==null){context.stall=false;return;}
        if(!(context.temporaryData instanceof Work))context.temporaryData=new Work(context);
        ((Work)context.temporaryData).tick(level);
    }
    private class Work {
        private final MovementContext context;
        private final NonNullList<ItemStack> equipment=NonNullList.withSize(2,ItemStack.EMPTY);
        private final ArcaneBoreRepair repair=new ArcaneBoreRepair();
        private BlockPos target;
        private int wait;
        private float acceleration;
        Work(MovementContext context){this.context=context;ContainerHelper.loadAllItems(context.blockEntityData,equipment);acceleration=context.blockEntityData.getShort("SpeedyTime");}
        void tick(ServerLevel level){
            var mounted=context.getItemStorage();
            if(mounted!=null&&mounted.getSlots()>=2)for(int i=0;i<2;i++)equipment.set(i,mounted.getStackInSlot(i).copy());
            var pick=equipment.get(1);var inspected=ArcaneBoreEquipment.inspect(equipment.get(0),pick);
            var origin=BlockPos.containing(context.position);
            var worker=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            repair.tick(level,origin,pick,worker,this::save);
            save();
            var basePos=context.localPos.relative(context.state.getValue(ArcaneBoreBlock.MOUNT).getOpposite());
            var base=context.contraption.getBlocks().get(basePos);
            boolean powered=context.blockEntityData.getBoolean("OmniBorePowered")||ArcaneMovement.powered(context,context.localPos)||ArcaneMovement.powered(context,basePos);
            if(base==null||!base.state().is(TCArcane.ARCANE_BORE_BASE.get())||!powered||!inspected.canDig(pick)){
                target=null;context.stall=false;return;
            }
            if(acceleration<20){
                acceleration+=VisNetwork.drain(level,origin,TCAspects.get(LegacyAspect.ENTROPY),100)/5f;
                if(acceleration<20&&MountedArcaneStorage.of(context.contraption).extract(TCAspects.get(LegacyAspect.ENTROPY),1,false)==1)acceleration+=20;
            }
            if(target==null){
                target=find(level,context,Math.min(32,2+inspected.area()));
                if(target!=null){wait=Math.max(1,ArcaneBoreScan.delay(level.getBlockState(target).getDestroySpeed(level,target),inspected.speed(),acceleration));
                    context.blockEntityData.putLong("target",target.asLong());context.blockEntityData.putInt("targetSequence",context.blockEntityData.getInt("targetSequence")+1);ArcaneMovement.sync(context);}
            }
            context.stall=target!=null;
            if(target==null||--wait>0){save();return;}
            var state=level.getBlockState(target);
            if(level.hasChunkAt(target)&&!state.isAir()&&state.getDestroySpeed(level,target)>=0){
                // Respect the same cancellation hook as Create drills and player-driven block breaking.
                var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,target,state,worker);
                if(!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)){
                    var drops=ArcaneBoreOutput.collect(level,target,worker,inspected);
                    level.levelEvent(2001,target,Block.getId(state));level.setBlock(target,Blocks.AIR.defaultBlockState(),3);
                    for(var drop:drops)MovingBore.this.dropItem(context,drop);
                    ArcaneBoreOutput.damagePick(pick,level);if(acceleration>0)acceleration--;
                }
            }
            target=null;save();
        }
        void save(){
            var mounted=context.getItemStorage();if(mounted!=null&&mounted.getSlots()>=2)for(int i=0;i<2;i++)mounted.setStackInSlot(i,equipment.get(i).copy());
            ContainerHelper.saveAllItems(context.blockEntityData,equipment);
            context.blockEntityData.putShort("SpeedyTime",(short)acceleration);
        }
    }
    public static BlockPos find(ServerLevel level,MovementContext context,int radius){
        BlockPos nearest=null;double distance=Double.MAX_VALUE;
        var direction=ArcaneMovement.vector(context,context.state.getValue(ArcaneBoreBlock.FACING));
        for(var ray:MovingBoreGeometry.rays(context.position,direction,radius)){
            var hit=BlockGetter.traverseBlocks(ray.from(),ray.to(),ray,(r,pos)->{
                if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return new Probe(null);
                var state=level.getBlockState(pos);if(state.isAir()||state.getBlock() instanceof LiquidBlock)return null;
                var collision=state.getCollisionShape(level,pos).clip(r.from(),r.to(),pos);if(collision==null)return null;
                return new Probe(state.getDestroySpeed(level,pos)<0?null:pos.immutable());
            },r->new Probe(null));
            if(hit.pos()!=null){double d=hit.pos().distToCenterSqr(context.position);if(d<distance){nearest=hit.pos();distance=d;}}
        }return nearest;
    }
    private record Probe(BlockPos pos){}
}

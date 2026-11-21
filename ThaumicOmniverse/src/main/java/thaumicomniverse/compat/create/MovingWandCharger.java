package thaumicomniverse.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemAspects;
import tc4revival.content.*;
import thaumcraft.api.visnet.VisEndpoint;

public class MovingWandCharger implements MovementBehaviour {
    @Override @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void renderInContraption(MovementContext context,com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld world,com.simibubi.create.content.contraptions.render.ContraptionMatrices matrices,net.minecraft.client.renderer.MultiBufferSource buffers){
        if(context.blockEntityData==null)return;var parent=WandCharger.readParent(context.blockEntityData);if(parent==null)return;
        float pt=net.createmod.catnip.animation.AnimationTickHolder.getPartialTicks();
        var pose=matrices.getModelViewProjection();pose.pushPose();pose.translate(context.localPos.getX(),context.localPos.getY(),context.localPos.getZ());
        ChargerVisRenderer.beam(context.contraption.entity.toLocalVector(parent,pt).subtract(Vec3.atLowerCornerOf(context.localPos)),context.state.getValue(WandCharger.FACING),context.blockEntityData.getCompound("VisBeam"),pose,buffers,net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(context.world));pose.popPose();
    }
    @Override public void tick(MovementContext context){
        if(context.world.isClientSide||context.position==null||context.blockEntityData==null)return;
        if(!(context.temporaryData instanceof Receiver))context.temporaryData=new Receiver(context);
        var receiver=(Receiver)context.temporaryData;
        if(!ArcaneContraptions.enabled()){receiver.close();return;}
        receiver.tick();
    }
    @Override public void stopMoving(MovementContext context){if(context.temporaryData instanceof Receiver receiver)receiver.close();}
    private static class Receiver implements VisEndpoint {
        final MovementContext context;
        Key key;
        final CompoundTag buffer;
        Receiver(MovementContext context){this.context=context;buffer=context.blockEntityData.getCompound("RepairVis");}
        @Override public Level level(){return context.world;}
        @Override public Key key(){return key;}
        @Override public Vec3 point(){return context.position;}
        @Override public boolean valid(){return key!=null&&!context.contraption.disassembled&&ArcaneContraptions.enabled()&&context.contraption.entity!=null&&!context.contraption.entity.isRemoved();}
        @Override public int range(){return 8;}
        @Override public boolean source(){return false;}
        @Override public boolean relay(){return false;}
        @Override public boolean connectsTo(VisEndpoint other){
            if(context.contraption.entity==null)return false;
            var local=context.contraption.entity.toLocalVector(other.point(),1)
                .subtract(context.contraption.entity.toLocalVector(point(),1));
            return WandCharger.connects(context.state,local);
        }
        @Override public void parentChanged(VisEndpoint parent){WandCharger.writeParent(context.blockEntityData,parent);ArcaneMovement.sync(context);}
        @Override public void consumed(Aspect aspect,int amount){
            var beam=context.blockEntityData.getCompound("VisBeam");
            if(level().getGameTime()<beam.getLong("Until"))return;
            beam.putLong("Until",level().getGameTime()+5);beam.putInt("Color",aspect.color());
            context.blockEntityData.put("VisBeam",beam);ArcaneMovement.sync(context);
        }
        void close(){if(key!=null)VisNetwork.unregister(this);key=null;}
        int consume(Aspect aspect,Integer amount){return VisNetwork.consume(this,aspect,amount);}
        void tick(){
            var pos=BlockPos.containing(context.position);
            if(key==null||!key.position().equals(pos)){
                close();key=new Key(pos,ResourceLocation.parse("thaumicomniverse:charger/"+context.contraption.entity.getUUID()+"/"+Long.toUnsignedString(context.localPos.asLong())));
                VisNetwork.register(this);
            }else if(level().getGameTime()%10==0)VisNetwork.register(this);
            if(level().getGameTime()%10==0){WandCharger.writeParent(context.blockEntityData,VisNetwork.parent(this));ArcaneMovement.sync(context);}
            var mounted=context.getItemStorage();
            if(mounted!=null&&mounted.getSlots()>0){var wand=mounted.getStackInSlot(0).copy();if(VisCharging.charge(wand,this::consume))mounted.setStackInSlot(0,wand);}
            else {
                var handler=new ItemStackHandler(1);handler.deserializeNBT(context.blockEntityData.getCompound("Inventory"));
                if(VisCharging.charge(handler.getStackInSlot(0),this::consume)){context.blockEntityData.put("Inventory",handler.serializeNBT());ArcaneMovement.sync(context);}
            }
            for(var actor:context.contraption.getActors()){
                var target=actor.right;
                if(target==null)continue;
                var behavior=MovementBehaviour.REGISTRY.get(target.state.getBlock());
                if(!(behavior instanceof com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour))continue;
                // A stationary, unobserved contraption may never have created this worker.
                // Use Create's own lazy initializer to preserve its owner and inventory.
                var player=((thaumicomniverse.mixin.CreateDeployerAccess)behavior).omniverse$player(target);
                if(player==null)continue;
                var stack=player.getMainHandItem();boolean changed=VisCharging.charge(stack,this::consume);
                // Eligibility tags govern obtaining the enchantment. Once present (for
                // example via Apotheosis shelves), Repair also works on ordinary tools.
                changed|=VisItemRepair.repair(stack,level(),player,buffer,this::consume);
                if(changed){
                    target.data.put("HeldItem",stack.save(new CompoundTag()));
                    target.blockEntityData.put("Inventory",player.getInventory().save(new ListTag()));
                    target.blockEntityData.put("HeldItem",stack.save(new CompoundTag()));
                    ArcaneMovement.sync(target);
                }
            }
            context.blockEntityData.put("RepairVis",buffer);
        }
    }
}

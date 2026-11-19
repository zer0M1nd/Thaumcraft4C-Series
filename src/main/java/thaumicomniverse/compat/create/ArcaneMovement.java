package thaumicomniverse.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.*;
import tc4revival.content.*;

public class ArcaneMovement {
    public static void setup(){
        com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.REGISTRY.register(ArcaneCreateContent.DEPOT.get(),new com.simibubi.create.content.logistics.depot.MountedDepotInteractionBehaviour(){
            @Override public boolean handlePlayerInteraction(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,BlockPos local,com.simibubi.create.content.contraptions.AbstractContraptionEntity structure){
                return super.handlePlayerInteraction(player,hand,local,structure);
            }
        });
        MovementBehaviour.REGISTRY.register(ArcaneCreateContent.CHARGER.get(),new MovingWandCharger());
        MovementBehaviour.REGISTRY.register(TCArcane.ITEM_MIRROR.get(),new MovingMirror());
        MovementBehaviour.REGISTRY.register(ArcaneCreateContent.INTERFACE.get(),new com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement(){
            @Override public boolean isActive(MovementContext context){return ArcaneContraptions.enabled()&&super.isActive(context);}
            @Override public com.simibubi.create.content.contraptions.render.ActorVisual createVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext context,com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld world,MovementContext movement){return null;}
            @Override @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
            public void renderInContraption(MovementContext context,com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld world,com.simibubi.create.content.contraptions.render.ContraptionMatrices matrices,net.minecraft.client.renderer.MultiBufferSource buffer){ArcaneMachineRenderers.movingPsi(context,world,matrices,buffer);}
        });
        MovementBehaviour.REGISTRY.register(TCArcane.CRUCIBLE.get(),new Crucible());
        MovementBehaviour.REGISTRY.register(TCArcane.LEVITATOR.get(),new Levitator());
        MovementBehaviour.REGISTRY.register(TCArcane.ARCANE_BORE.get(),new MovingBore());
        for(var block:net.minecraftforge.registries.ForgeRegistries.BLOCKS)
            if(block instanceof ArcaneLampBlock lamp&&lamp.kind()!=ArcaneLampBlock.Kind.LIGHT)MovementBehaviour.REGISTRY.register(block,new MovingLamp());
        MovingArcaneDoor.setup();
    }
    public static void sync(MovementContext context){
        if(context.world.isClientSide||context.blockEntityData==null)return;
        ((ContraptionAccess)context.contraption).omniverse$updates().put(context.localPos,context.blockEntityData.copy());
        CreateArcaneNetwork.sync(context.contraption.entity,context.localPos,context.blockEntityData.copy());
    }
    public static void state(MovementContext context,net.minecraft.world.level.block.state.BlockState next){
        if(context.state==next)return;
        context.state=next;
        var info=new StructureBlockInfo(context.localPos,next,context.blockEntityData);
        context.contraption.entity.setBlock(context.localPos,info);
        for(var actor:context.contraption.getActors())if(actor.right==context){actor.setLeft(info);break;}
    }
    public static Vec3 vector(MovementContext context,Direction direction){return context.rotation.apply(Vec3.atLowerCornerOf(direction.getNormal())).normalize();}
    public static boolean powered(MovementContext context,BlockPos local){
        if(context.contraption.entity==null)return false;
        var real=BlockPos.containing(context.contraption.entity.toGlobalVector(local.getCenter(),1));
        if(context.world.hasNeighborSignal(real))return true;
        for(var direction:Direction.values()){
            var at=local.relative(direction);var info=context.contraption.getBlocks().get(at);if(info==null)continue;
            if(info.state().is(net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK))return true;
            if(info.state().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                &&info.state().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED))return true;
        }return false;
    }
    private static class Crucible implements MovementBehaviour {
        @Override public void tick(MovementContext context){
            if(!ArcaneContraptions.enabled()||!(context.world instanceof ServerLevel server)||context.position==null||context.blockEntityData==null)return;
            if(vector(context,Direction.UP).y>=-.001)return;
            var contents=new CrucibleContents(tc4revival.aspect.TCAspects.catalog());contents.load(context.blockEntityData);
            if(contents.water()==0&&contents.aspects().isEmpty())return;
            long pollution=contents.empty();contents.save(context.blockEntityData);
            FluxSpills.empty(server,BlockPos.containing(context.position),pollution);
            sync(context);
            server.playSound(null,BlockPos.containing(context.position),TCSounds.BUBBLE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
        }
    }
    private static class Levitator implements MovementBehaviour {
        @Override public void tick(MovementContext context){
            if(!ArcaneContraptions.enabled()||context.position==null||powered(context,context.localPos))return;
            var up=vector(context,Direction.UP);int range=10;
            for(var below=context.localPos.below();;below=below.below()){
                var info=context.contraption.getBlocks().get(below);if(info==null||!info.state().is(TCArcane.LEVITATOR.get())||powered(context,below))break;range+=10;
            }
            range=Math.min(256,range);
            var start=context.position.add(up.scale(.5));var end=start.add(up.scale(range));
            for(int i=0;i<range;i++){
                var pos=BlockPos.containing(start.add(up.scale(i)));
                if(!context.world.hasChunkAt(pos)||context.world.getBlockState(pos).isSolidRender(context.world,pos)){range=i;break;}
            }
            end=start.add(up.scale(range));
            for(var entity:context.world.getEntities(context.contraption.entity,new AABB(start,end).inflate(.9))){
                if(!(entity instanceof ItemEntity)&&!entity.isPushable()&&!(entity instanceof AbstractHorse))continue;
                var delta=entity.position().subtract(start);double height=delta.dot(up);
                if(height<-.2||height>range||delta.subtract(up.scale(height)).lengthSqr()>.5)continue;
                var motion=entity.getDeltaMovement();double lift=motion.dot(up);
                if(entity.isShiftKeyDown()){if(lift<0)motion=motion.subtract(up.scale(lift*.1));}
                else if(lift<.35)motion=motion.add(up.scale(.1));
                entity.setDeltaMovement(motion);entity.fallDistance=0;
            }
        }
    }
}

package thaumicomniverse.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Custom types have no Flywheel visual factory, so their fallback must render even when Flywheel is enabled. */
public class ArcaneMachineRenderers {
    public static final PartialModel CRANK=partial("thaumic_hand_crank_handle"),PSI_MIDDLE=partial("portable_essentia_interface_middle"),
        PSI_LIT=partial("portable_essentia_interface_middle_powered"),PSI_TOP=partial("portable_essentia_interface_top"),
        TANK_COGS=partial("void_backtank_cogs"),TANK_SHAFT=partial("void_backtank_shaft_input"),
        DEPOT_PLATE=partial("arcane_floating_depot_plate");
    public static final PartialModel[] SPOUT={partial("essentia_spout_top"),partial("essentia_spout_middle"),partial("essentia_spout_bottom")};
    public static final java.util.Map<String,PartialModel> HAND=java.util.stream.Stream.of("cog","thin_long","thin_short","wide_long","wide_short","holding","pointing","punching")
        .collect(java.util.stream.Collectors.toUnmodifiableMap(s->s,s->partial("magic_hand_"+s)));
    public static PartialModel partial(String name){return PartialModel.of(ResourceLocation.parse("thaumicomniverse:block/create/"+name));}
    public static void init(){}
    public static void register(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ArcaneCreateContent.CRUSHING_WHEEL_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->
            KineticBlockEntityRenderer.renderRotatingBuffer(tile,CachedBuffers.block(tile.getBlockState()),pose,buffers.getBuffer(RenderType.cutoutMipped()),light));
        event.registerBlockEntityRenderer(ArcaneCreateContent.SWITCH_TILE.get(),com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ArcaneCreateContent.MOTOR_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->shaft(tile,tile.getBlockState().getValue(ArcaneMotor.FACING),pose,buffers,light));
        event.registerBlockEntityRenderer(ArcaneCreateContent.MAGNET_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->shaft(tile,tile.getBlockState().getValue(ArcaneMagnet.FACING).getOpposite(),pose,buffers,light));
        event.registerBlockEntityRenderer(ArcaneCreateContent.CRANK_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->{
            var face=tile.getBlockState().getValue(BlockStateProperties.FACING);
            if(tile.shouldRenderShaft())KineticBlockEntityRenderer.renderRotatingBuffer(tile,
                CachedBuffers.block(tile.getBlockState()),pose,buffers.getBuffer(RenderType.solid()),light);
            var handle=CachedBuffers.partialFacing(CRANK,tile.getBlockState(),face.getOpposite());
            KineticBlockEntityRenderer.kineticRotationTransform(handle,tile,face.getAxis(),tile.getIndependentAngle(pt),light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
        });
        event.registerBlockEntityRenderer(ArcaneCreateContent.BURNER_TILE.get(),com.simibubi.create.content.processing.burner.BlazeBurnerRenderer::new);
        event.registerBlockEntityRenderer(ArcaneCreateContent.DEPOT_TILE.get(),FloatingDepotRenderer::new);
        event.registerBlockEntityRenderer(ArcaneCreateContent.CHARGER_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->{
            var face=tile.getBlockState().getValue(WandCharger.FACING);
            if(tile.getLevel() instanceof net.createmod.ponder.api.level.PonderLevel&&tile.beamParent()!=null)
                thaumicomniverse.compat.ponder.PonderNodeRenderers.beam(tile.beamParent().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(tile.getBlockPos())),new net.minecraft.world.phys.Vec3(.5,.5,.5),pose,buffers);
            else if(!(tile.getLevel() instanceof com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld)&&tile.beamParent()!=null)
                ChargerVisRenderer.beam(tile.beamParent().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(tile.getBlockPos())),face,tile.beam(),pose,buffers,net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(tile.getLevel()));
            var stack=tile.inventory.getStackInSlot(0);if(stack.isEmpty())return;
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(face.getRotation());
            if(tile.getBlockState().getValue(WandCharger.ROTATED))pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.translate(-.5,-.5,-.5);
            if(stack.getItem() instanceof tc4revival.content.CastingWandItem){
                boolean staff=tc4revival.content.WandParts.rod(stack.getTag()).staff(),sceptre=tc4revival.content.WandParts.sceptre(stack.getTag());
                boolean focus=!tc4revival.content.WandFocusItem.attached(stack).isEmpty();
                double radius=focus?(staff?.0984375:.09375):sceptre?.0975:staff?.08125:.075;
                // Draw the actual wand model. FIXED carries an item-frame offset,
                // which displaced the midpoint when rotated horizontally.
                pose.translate(.5,.75+radius*.6,.5);pose.mulPose(Axis.YP.rotationDegrees(45));pose.mulPose(Axis.ZP.rotationDegrees(90));pose.scale(.6f,.6f,.6f);
                pose.translate(0,-(staff?1.365625:sceptre?.615625:.625),0);
                tc4revival.client.WandRenderer.instance().renderModel(stack,pose,buffers,light,overlay,net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(tile.getLevel()));
            }else{
                pose.translate(.5,.8,.5);pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.6f,.6f,.6f);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),0);
            }
            pose.popPose();
        });
        event.registerBlockEntityRenderer(ArcaneCreateContent.INTERFACE_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->
            psi(tile.getBlockState(),tile.renderConnected(),tile.renderExtension(pt),null,p->p.light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()))));
        event.registerBlockEntityRenderer(ArcaneCreateContent.BACKTANK_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->{
            var state=tile.getBlockState();
            KineticBlockEntityRenderer.renderRotatingBuffer(tile,CachedBuffers.partial(TANK_SHAFT,state),pose,buffers.getBuffer(RenderType.cutoutMipped()),light);
            var cogs=CachedBuffers.partial(TANK_COGS,state);
            float angle=tile.getSpeed()/4*net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(tile.getLevel())%360;
            cogs.center().rotateYDegrees(180+AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.HORIZONTAL_FACING))).uncenter()
                .translate(0,.40625,.6875).rotateXDegrees(angle).translate(0,-.40625,-.6875).light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
        });
        event.registerBlockEntityRenderer(ArcaneCreateContent.SPOUT_TILE.get(),ctx->(tile,pt,pose,buffers,light,overlay)->{
            float t=tile.processingTicks-pt;float progress=net.minecraft.util.Mth.clamp(1-(t-5)/10,0,1);
            float squeeze=t<0?0:t<2?-t/2:t<10?-1:(float)(Math.pow(2*progress-1,2)-1);
            pose.pushPose();for(var bit:SPOUT){CachedBuffers.partial(bit,tile.getBlockState()).light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));pose.translate(0,-3*squeeze/32,0);}pose.popPose();
            if(tile.visualAspect()!=null){
                if(tile.amount()>0)ArcaneEssentiaVisuals.liquid(tile.visualAspect(),pose,buffers,3/16f,3/16f,3/16f,13/16f,3/16f+tile.amount()/64f*10/16f,13/16f);
                if(t>=0&&t<=12)ArcaneEssentiaVisuals.liquid(tile.visualAspect(),pose,buffers,.475f,-1.2f,.475f,.525f,-.25f,.525f);
            }
        });
    }
    private static void shaft(KineticBlockEntity tile,Direction face,PoseStack pose,MultiBufferSource buffers,int light){
        var shaft=CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,tile.getBlockState(),face);
        if(tile instanceof ArcaneMagnet.Tile)shaft.translate(face.getStepX()*.002,face.getStepY()*.002,face.getStepZ()*.002);
        KineticBlockEntityRenderer.renderRotatingBuffer(tile,shaft,pose,buffers.getBuffer(RenderType.solid()),light);
    }
    private static void psi(BlockState state,boolean connected,float distance,PoseStack local,java.util.function.Consumer<SuperByteBuffer> draw){
        var face=state.getValue(BlockStateProperties.FACING);
        var middle=CachedBuffers.partial(connected?PSI_LIT:PSI_MIDDLE,state);var top=CachedBuffers.partial(PSI_TOP,state);
        for(var model:java.util.List.of(middle,top)){
            // The contraption transform must wrap the facing and extension, as in Create's PSI renderer.
            if(local!=null)model.transform(local);
            model.center().rotateYDegrees(AngleHelper.horizontalAngle(face)).rotateXDegrees(face==Direction.UP?0:face==Direction.DOWN?180:90).uncenter();
        }
        middle.translate(0,distance*.5+.375,0);top.translate(0,distance,0);draw.accept(middle);draw.accept(top);
    }
    public static void movingPsi(com.simibubi.create.content.contraptions.behaviour.MovementContext context,
            com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld world,
            com.simibubi.create.content.contraptions.render.ContraptionMatrices matrices,MultiBufferSource buffer){
        var animation=com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement.getAnimation(context);
        psi(context.state,animation.settled(),animation.getValue(net.createmod.catnip.animation.AnimationTickHolder.getPartialTicks()),matrices.getModel(),
            model->model.light(LevelRenderer.getLightColor(world,context.localPos)).useLevelLight(context.world,matrices.getWorld()).renderInto(matrices.getViewProjection(),buffer.getBuffer(RenderType.cutoutMipped())));
    }
}

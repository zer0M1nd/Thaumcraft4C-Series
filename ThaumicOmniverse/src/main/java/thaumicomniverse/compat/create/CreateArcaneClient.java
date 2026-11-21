package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.client.Minecraft;

public class CreateArcaneClient {
    public static void setup(){
        thaumcraft.api.aspects.RevealingTargets.register(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:contraptions"),(player,eye,end,partial)->{
            thaumcraft.api.aspects.RevealingTargets.Target nearest=null;
            double distance=eye.distanceToSqr(end);
            for(var ref:com.simibubi.create.content.contraptions.ContraptionHandler.loadedContraptions.get(player.level()).values()){
                var entity=ref.get();if(entity==null||entity.isRemoved()||entity.getContraption()==null)continue;
                var hit=ArcaneContraptions.ray(entity,eye,end,partial).orElse(null);
                if(hit==null||eye.distanceToSqr(hit.world())>=distance)continue;
                // The closest surface occludes containers in other assemblies, too.
                distance=eye.distanceToSqr(hit.world());nearest=null;
                var structure=entity.getContraption();
                var container=CreateClientStructures.blockEntity(structure,hit.local()) instanceof tc4revival.content.RevealedAspectContainer c?c:null;
                var above=structure.getBlocks().get(hit.local().above());
                nearest=new thaumcraft.api.aspects.RevealingTargets.Target(hit.world(),hit.local(),hit.face(),above==null||above.state().isAir(),container,v->entity.toGlobalVector(v,partial));
            }
            return nearest;
        });
        dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer.builder(ArcaneCreateContent.MOBILE.get())
            .factory(com.simibubi.create.content.contraptions.render.ContraptionVisual::new)
            .neverSkipVanillaRender().apply();
        dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer.builder(ArcaneCreateContent.WAND_MOBILE.get())
            .factory(com.simibubi.create.content.contraptions.render.ContraptionVisual::new)
            .neverSkipVanillaRender().apply();
        thaumcraft.api.golems.GolemLocationVisuals.register(MovingGolemBindings.PROVIDER,(level,view,partial)->{
            if(!view.hasUUID("Structure"))return null;var id=view.getUUID("Structure");
            for(var ref:com.simibubi.create.content.contraptions.ContraptionHandler.loadedContraptions.get(level).values()){
                var entity=ref.get();if(entity==null||entity.isRemoved()||entity.getContraption()==null||entity.getContraption().disassembled||!id.equals(((ContraptionAccess)entity.getContraption()).omniverse$frameId()))continue;
                var local=net.minecraft.core.BlockPos.of(view.getLong("Local"));var side=net.minecraft.core.Direction.from3DDataValue(view.getByte("Face"));
                if(!entity.getContraption().getBlocks().containsKey(local))return null;
                var center=entity.toGlobalVector(local.getCenter(),partial);
                var normal=entity.toGlobalVector(local.getCenter().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(side.getNormal())),partial).subtract(center);
                var up=side.getAxis()==net.minecraft.core.Direction.Axis.Y?net.minecraft.world.phys.Vec3.atLowerCornerOf(net.minecraft.core.Direction.NORTH.getNormal()):new net.minecraft.world.phys.Vec3(0,1,0);
                up=entity.toGlobalVector(local.getCenter().add(up),partial).subtract(center);
                return new thaumcraft.api.golems.GolemLocationVisuals.Frame(center,normal,up);
            }return null;
        });
    }
    public static void init(){
        // This class is reached only after Create's presence has been checked.
        // Auto-subscribing the renderer itself loads its Create superclass even without Create.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(FloatingDepotRenderer::selection);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ContraptionRenderHandoff::frame);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ContraptionRenderHandoff::beforeFrame);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ContraptionRenderHandoff::logout);
        ArcaneMachineRenderers.init();
        var shift=com.simibubi.create.foundation.block.connected.CTSpriteShifter.getCT(com.simibubi.create.foundation.block.connected.AllCTTypes.OMNIDIRECTIONAL,
            net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:block/create/andesite_casing"),net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:block/create/andesite_casing_connected"));
        com.simibubi.create.CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:thaumic_casing"),model->new com.simibubi.create.foundation.block.connected.CTModel(model,new com.simibubi.create.foundation.block.connected.SimpleCTBehaviour(shift)));
        var magnetic=com.simibubi.create.foundation.block.connected.CTSpriteShifter.getCT(com.simibubi.create.foundation.block.connected.AllCTTypes.OMNIDIRECTIONAL,
            net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:block/create/thaumic_magnetic_casing"),net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:block/create/thaumic_magnetic_casing_connected"));
        com.simibubi.create.CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ArcaneCreateContent.MAGNETIC_CASING.getId(),model->new com.simibubi.create.foundation.block.connected.CTModel(model,new com.simibubi.create.foundation.block.connected.SimpleCTBehaviour(magnetic)));
    }
    public static void spoutParticle(EssentiaSpout.Tile tile){
        var mc=Minecraft.getInstance();if(mc.level==null||tile.visualAspect()==null)return;
        var pos=tile.getBlockPos();for(int i=0;i<3;i++)mc.particleEngine.add(new ArcaneEssentiaVisuals.Drop(mc.level,pos.getX()+.5,pos.getY()-.45,pos.getZ()+.5,tile.visualAspect()));
    }
    public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(ArcaneCreateContent.MOBILE.get(),com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer::new);
        event.registerEntityRenderer(ArcaneCreateContent.WAND_MOBILE.get(),com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer::new);
        ArcaneMachineRenderers.register(event);
    }
    public static void receive(CreateArcaneNetwork.Update packet){
        var level=Minecraft.getInstance().level;if(level==null||packet.tag()==null)return;
        if(!(level.getEntity(packet.entity()) instanceof AbstractContraptionEntity entity)||entity.getContraption()==null)return;
        var contraption=entity.getContraption();
        for(var actor:contraption.getActors())if(actor.right!=null&&actor.right.localPos.equals(packet.local()))actor.right.blockEntityData=packet.tag().copy();
        var tile=CreateClientStructures.blockEntity(contraption,packet.local());
        if(tile!=null)tile.handleUpdateTag(packet.tag());
        var info=contraption.getBlocks().get(packet.local());
        if(info!=null&&info.nbt()!=null){for(var key:new java.util.HashSet<>(info.nbt().getAllKeys()))info.nbt().remove(key);info.nbt().merge(packet.tag());}
    }
}

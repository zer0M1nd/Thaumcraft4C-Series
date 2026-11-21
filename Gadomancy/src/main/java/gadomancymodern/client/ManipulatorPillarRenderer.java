package gadomancymodern.client;
import gadomancymodern.node.ManipulatorPillarBlock;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
public class ManipulatorPillarRenderer implements BlockEntityRenderer<ManipulatorPillarBlock.PillarEntity> {
    protected static tc4revival.client.GroupedObjMesh mesh;
    public ManipulatorPillarRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ManipulatorPillarBlock.PillarEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(mesh==null)try(var reader=new java.io.InputStreamReader(net.minecraft.client.Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/pillar.obj")).open(),java.nio.charset.StandardCharsets.UTF_8)){mesh=tc4revival.client.GroupedObjMesh.read(reader);}catch(java.io.IOException e){throw new IllegalStateException("Cannot load original infusion pillar",e);}pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));int orientation=tile.getBlockState().getValue(ManipulatorPillarBlock.ORIENTATION);pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(orientation==3?90:orientation==4?270:orientation==5?180:0));var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("gadomancy:textures/models/manipulator_pillar.png")));var frame=pose.last();for(var group:mesh.groups().values())for(var v:group)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(255,255,255,255).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();pose.popPose();}
    @Mod.EventBusSubscriber(modid="gadomancy",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)public static class Reload {@net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.client.event.RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)resources->mesh=null);}}
}

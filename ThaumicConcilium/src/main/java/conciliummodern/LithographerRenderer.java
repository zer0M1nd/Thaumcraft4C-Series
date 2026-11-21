package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.GroupedObjMesh;

public final class LithographerRenderer implements BlockEntityRenderer<LithographerBlockEntity>{
    private static final java.util.Map<String,GroupedObjMesh> MODELS=new java.util.HashMap<>();
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicconcilium:textures/models/lithographer.png");
    private static BlockEntityWithoutLevelRenderer item;
    public LithographerRenderer(BlockEntityRendererProvider.Context context){}
    public static void reload(){MODELS.clear();}
    private static GroupedObjMesh mesh(String name){return MODELS.computeIfAbsent(name,key->{try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/"+key+".obj")).open()){
        return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
    }catch(java.io.IOException exception){throw new IllegalStateException("Cannot load lithographer model",exception);}});}
    private static void draw(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));var frame=pose.last();for(var vertices:mesh(name).groups().values())for(var vertex:vertices)
            out.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(1f,1f,1f,1f).uv(vertex.u(),vertex.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
    }
    @Override public void render(LithographerBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,0,.5);if(tile.getBlockState().getValue(LithographerBlock.AXIS)==net.minecraft.core.Direction.Axis.Z)pose.mulPose(Axis.YP.rotationDegrees(90));
        draw("lithographer",pose,buffers,light,overlay);draw("lithographer1",pose,buffers,light,overlay);
        if(tile.phase()!=0){float remaining=Math.max(0,tile.timer()-partial);float offset=tile.phase()==1?remaining/150f:tile.phase()==3?(20-remaining)/150f:0;
            for(int sector=2;sector<=4;sector++){pose.pushPose();pose.translate(0,offset*(sector*2-3),0);draw("lithographer"+sector,pose,buffers,light,overlay);pose.popPose();}}
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer(){if(item==null)item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
        @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.4f,.4f,.4f);pose.mulPose(Axis.YP.rotationDegrees(45));draw("lithographer",pose,buffers,light,overlay);draw("lithographer1",pose,buffers,light,overlay);pose.popPose();}
    };return item;}
}

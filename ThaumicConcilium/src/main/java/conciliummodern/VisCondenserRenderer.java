package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.GroupedObjMesh;

/** The author's OBJ and texture are supplied by the external resource loader. */
public final class VisCondenserRenderer implements BlockEntityRenderer<VisCondenserBlockEntity> {
    private static GroupedObjMesh mesh;
    private static BlockEntityWithoutLevelRenderer item;
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicconcilium:textures/models/vis_condenser.png");
    public VisCondenserRenderer(BlockEntityRendererProvider.Context context){}
    public static void reload(){mesh=null;}
    private static void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(mesh==null)try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/vis_condenser.obj")).open()){
            mesh=GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot load vis condenser model",exception);}
        pose.pushPose();pose.translate(.5,.5,.5);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));var frame=pose.last();
        for(var vertices:mesh.groups().values())for(var v:vertices)
            out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(1f,1f,1f,1f).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
        pose.popPose();
    }
    @Override public void render(VisCondenserBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(pose,buffers,light,overlay);}
    public static BlockEntityWithoutLevelRenderer itemRenderer(){
        if(item==null)item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(pose,buffers,light,overlay);}
        };return item;
    }
}

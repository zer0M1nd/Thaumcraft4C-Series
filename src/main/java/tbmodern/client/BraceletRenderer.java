package tbmodern.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import tbmodern.resources.ObjMesh;
import java.util.*;

public class BraceletRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation MODEL=new ResourceLocation("thaumicbases","models/bracelet/bracelet.obj");
    private static volatile ObjMesh mesh=new ObjMesh(Map.of());
    private static BraceletRenderer instance;
    public static BraceletRenderer instance() {if(instance==null)instance=new BraceletRenderer();return instance;}
    private BraceletRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static final SimplePreparableReloadListener<ObjMesh> RELOAD=new SimplePreparableReloadListener<>() {
        @Override protected ObjMesh prepare(ResourceManager manager,ProfilerFiller profiler) {
            try(var reader=manager.openAsReader(MODEL)) {return ObjMesh.read(reader);}
            catch(java.io.IOException exception) {com.mojang.logging.LogUtils.getLogger().error("Cannot load external Thaumic Bases bracelet model",exception);return new ObjMesh(Map.of());}
        }
        @Override protected void apply(ObjMesh value,ResourceManager manager,ProfilerFiller profiler) {mesh=value;}
    };
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        // Held bracelets are drawn on the wrist by BraceletHands/BraceletLayer.
        if(context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND)return;
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context==ItemDisplayContext.GUI)pose.scale(2F/3,2F/3,2F/3);
        pose.mulPose(Axis.XP.rotationDegrees(15));pose.mulPose(Axis.YP.rotationDegrees(-20));
        pose.scale(.085F,.085F,.085F);
        renderModel(stack,pose,buffers,light,overlay);
        pose.popPose();
    }
    /** Raw legacy OBJ frame; the band surrounds the Z axis and the focus sits on its upper rim. */
    public static void renderModel(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var wand=(tbmodern.Bracelet)stack.getItem();
        var texture=wand.texture();
        var output=buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation(texture)));
        for(var group:mesh.groups().values())for(var face:group) {
            vertex(output,pose,face.a(),light,overlay);vertex(output,pose,face.b(),light,overlay);vertex(output,pose,face.c(),light,overlay);vertex(output,pose,face.c(),light,overlay);
        }
        var focus=tc4revival.content.WandFocusItem.attached(stack);
        if(!focus.isEmpty()) {
            pose.pushPose();pose.translate(0,4.1,0);pose.scale(2F,2F,2F);
            Minecraft.getInstance().getItemRenderer().renderStatic(focus,ItemDisplayContext.FIXED,light,overlay,pose,buffers,Minecraft.getInstance().level,0);pose.popPose();
        }
    }
    private static void vertex(VertexConsumer output,PoseStack pose,ObjMesh.Vertex vertex,int light,int overlay) {
        var p=vertex.position();var normal=vertex.normal();var uv=vertex.uv();
        output.vertex(pose.last().pose(),p.x(),p.y(),p.z()).color(255,255,255,255).uv(uv.u(),uv.v()).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),normal.x(),normal.y(),normal.z()).endVertex();
    }
}

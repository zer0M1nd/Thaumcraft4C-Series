package tbmodern.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import tbmodern.resources.ObjMesh;

/** Original player-supplied OBJ geometry/UVs, shared by placed blocks and inventory items. */
public class NodeDeviceModels extends BlockEntityWithoutLevelRenderer {
    private static Map<String,ObjMesh> meshes=Map.of();
    private static NodeDeviceModels instance;
    public static NodeDeviceModels instance() { if(instance==null)instance=new NodeDeviceModels();return instance; }
    private NodeDeviceModels() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static final SimplePreparableReloadListener<Map<String,ObjMesh>> RELOAD=new SimplePreparableReloadListener<>() {
        @Override protected Map<String,ObjMesh> prepare(ResourceManager manager,ProfilerFiller profiler) {
            var result=new HashMap<String,ObjMesh>();
            for(String name:List.of("nodelinker","nodemanipulator"))try(var reader=manager.openAsReader(id("models/"+name+"/"+name+".obj"))) {
                result.put(name,ObjMesh.read(reader));
            }catch(java.io.IOException e){com.mojang.logging.LogUtils.getLogger().error("Cannot load external node model {}",name,e);}
            return Map.copyOf(result);
        }
        @Override protected void apply(Map<String,ObjMesh> result,ResourceManager manager,ProfilerFiller profiler) { meshes=result; }
    };
    private static ResourceLocation id(String path) { return new ResourceLocation("thaumicbases",path); }
    public static void render(boolean manipulator,String mode,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mesh=meshes.get(manipulator?"nodemanipulator":"nodelinker");if(mesh==null)return;
        pose.pushPose();pose.translate(.5,manipulator?1:0,.5);
        if(manipulator)pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(.5f,.5f,.5f);
        for(var group:mesh.groups().entrySet()) {
            boolean focus=group.getKey().equals("foci_Cube.003");
            if(manipulator&&focus&&mode.isEmpty())continue;
            String texture=manipulator?"nodemanipulator/"+(focus?"foci_"+mode:"baseuvmap"):"nodelinker/nodelinkeruv";
            var out=buffers.getBuffer(RenderType.entityCutoutNoCull(id("textures/blocks/"+texture+".png")));
            for(var face:group.getValue()) { vertex(out,pose,face.a(),light,overlay);vertex(out,pose,face.b(),light,overlay);vertex(out,pose,face.c(),light,overlay);vertex(out,pose,face.c(),light,overlay); }
        }
        pose.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack pose,ObjMesh.Vertex vertex,int light,int overlay) {
        var p=vertex.position();var n=vertex.normal();var uv=vertex.uv();
        out.vertex(pose.last().pose(),p.x(),p.y(),p.z()).color(255,255,255,255).uv(uv.u(),uv.v()).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),n.x(),n.y(),n.z()).endVertex();
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        render(stack.is(tbmodern.Content.item("node_manipulator")),"",pose,buffers,light,overlay);
    }
}

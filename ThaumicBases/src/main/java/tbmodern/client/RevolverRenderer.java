package tbmodern.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import tbmodern.api.RevolverUpgrades;
import tbmodern.resources.ObjMesh;
import java.util.*;

public class RevolverRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation MODEL=new ResourceLocation("thaumicbases","models/revolver/revolver.obj");
    private static volatile ObjMesh mesh=new ObjMesh(Map.of());
    private static tbmodern.resources.GuiModelFit guiFit=tbmodern.resources.GuiModelFit.of(mesh);
    private static RevolverRenderer instance;
    public static RevolverRenderer instance() {if(instance==null)instance=new RevolverRenderer();return instance;}
    private RevolverRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static final SimplePreparableReloadListener<ObjMesh> RELOAD=new SimplePreparableReloadListener<>() {
        @Override protected ObjMesh prepare(ResourceManager manager,ProfilerFiller profiler) {
            try(var reader=manager.openAsReader(MODEL)) {return ObjMesh.read(reader);}
            catch(java.io.IOException exception) {com.mojang.logging.LogUtils.getLogger().error("Cannot load external Thaumic Bases revolver model",exception);return new ObjMesh(Map.of());}
        }
        @Override protected void apply(ObjMesh value,ResourceManager manager,ProfilerFiller profiler) {mesh=value;guiFit=tbmodern.resources.GuiModelFit.of(value);}
    };
    private static ResourceLocation texture(ItemStack stack,String part,String fallback) {
        String path=fallback;
        if(part.equals("metal") && RevolverUpgrades.level(stack,"eldritch")>0)path="revolverdarkmetaleldritch";
        if(part.equals("barrel") && RevolverUpgrades.level(stack,"primal")>0)path="revolvergunprimaluv";
        if(part.equals("barrel") && RevolverUpgrades.level(stack,"tainted")>0)path="revolverguntainteduv";
        if(part.equals("handle") && RevolverUpgrades.level(stack,"void")>0)path="revolverhandlevoiduv";
        ResourceLocation result=new ResourceLocation("thaumicbases","textures/items/revolver/"+path+".png");
        for(var active:RevolverUpgrades.behaviors(stack)) {var replacement=active.behavior().texture(stack,active.level(),part);if(replacement!=null)result=replacement;}
        return result;
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        if(context==ItemDisplayContext.GUI) {
            var fit=guiFit;
            pose.translate(.5,.5,.5);pose.scale(fit.scale,fit.scale,fit.scale);
            pose.translate(-fit.x,-fit.y,-fit.z);
            pose.mulPose(Axis.XP.rotationDegrees(tbmodern.resources.GuiModelFit.X_ROTATION));
            pose.mulPose(Axis.YP.rotationDegrees(tbmodern.resources.GuiModelFit.Y_ROTATION));
        } else {pose.translate(.5,.2,.5);pose.scale(.1F,.1F,.1F);}
        part(stack,"handle","revolverhandleuv",pose,buffers,light,overlay,"Cylinder","Plane");
        double rotation=stack.hasTag()?stack.getTag().getDouble("BarrelRotation"):0;
        if(!Double.isFinite(rotation))rotation=0;
        rotation%=360;
        pose.pushPose();pose.translate(0,2.9,0);pose.translate(Math.sin(Math.toRadians(rotation))*2.8,-Math.cos(Math.toRadians(rotation))*2.8,0);pose.mulPose(Axis.ZP.rotationDegrees((float)rotation));
        part(stack,"cylinder","revolverbarreluv",pose,buffers,light,overlay,"rBarrel_Cube.001");pose.popPose();
        part(stack,"metal","revolverdarkmetal",pose,buffers,light,overlay,"Cube.003_Cube.004","Cube.002_Cube.003","Cube_Cube.001");
        part(stack,"barrel","revolvergunuv",pose,buffers,light,overlay,"Cylinder.002","Plane.001");
        part(stack,"trigger","revolverpressuv",pose,buffers,light,overlay,"Cube.001_Cube.005");
        pose.popPose();
    }
    private static void part(ItemStack stack,String part,String fallback,PoseStack pose,MultiBufferSource buffers,int light,int overlay,String... groups) {
        var output=buffers.getBuffer(RenderType.entityCutoutNoCull(texture(stack,part,fallback)));
        var current=mesh;
        for(String group:groups)for(var face:current.groups().getOrDefault(group,List.of())) {
            vertex(output,pose,face.a(),light,overlay);vertex(output,pose,face.b(),light,overlay);vertex(output,pose,face.c(),light,overlay);vertex(output,pose,face.c(),light,overlay);
        }
    }
    private static void vertex(VertexConsumer output,PoseStack pose,ObjMesh.Vertex vertex,int light,int overlay) {
        var p=vertex.position();var normal=vertex.normal();var uv=vertex.uv();
        output.vertex(pose.last().pose(),p.x(),p.y(),p.z()).color(255,255,255,255).uv(uv.u(),uv.v()).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),normal.x(),normal.y(),normal.z()).endVertex();
    }
}

package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

/** Reuses the original body/cap OBJ, original textures and liquid palette. */
public final class DrainageSyringeRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation LIQUID=ResourceLocation.parse("thaumicconcilium:item/syringe_liquid");
    public static final int[] COLORS={0x9700c0,0x0fd9ff,0xff0000};
    private static DrainageSyringeRenderer instance;
    private static final java.util.Map<String,GroupedObjMesh> MESHES=new java.util.HashMap<>();
    private DrainageSyringeRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static DrainageSyringeRenderer instance(){if(instance==null)instance=new DrainageSyringeRenderer();return instance;}
    public static void reload(){MESHES.clear();}
    private static GroupedObjMesh mesh(String part){return MESHES.computeIfAbsent(part,key->{try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/"+key+".obj")).open()){
        return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
    }catch(java.io.IOException failure){throw new IllegalStateException("Cannot load syringe "+key,failure);}});}
    public void draw(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(DrainageSyringe.amount(stack)>0){
            pose.pushPose();pose.translate(-1.4,0,0);pose.mulPose(Axis.ZP.rotationDegrees(60));pose.scale(.7f,.7f,.7f);pose.translate(-.5,0,-.5);
            float fraction=(float)DrainageSyringe.amount(stack)/DrainageSyringe.CAPACITY[DrainageSyringe.type(stack)];
            pose.translate(0,.0625,0);pose.scale(1,fraction,1);pose.translate(0,-.0625,0);
            var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(LIQUID),stack,light,overlay,pose,buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)));pose.popPose();
        }
        if(!DrainageSyringe.open(stack))part("cap","models/cap_"+new String[]{"vis","mana","blood"}[DrainageSyringe.type(stack)],pose,buffers,light,overlay);
        part("syringe","items/syringe",pose,buffers,light,overlay);
    }
    private static void part(String name,String texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumicconcilium:textures/"+texture+".png")));var frame=pose.last();
        for(var vertices:mesh(name).groups().values())for(var v:vertices)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(1f,1f,1f,1f).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.5f,.5f,.5f);
        if(context==ItemDisplayContext.GUI){pose.translate(.25,.25,0);pose.mulPose(Axis.ZP.rotationDegrees(-25));}
        else if(context==ItemDisplayContext.GROUND)pose.translate(0,.5,0);
        else {pose.scale(1.5f,1.5f,1.5f);pose.mulPose(Axis.YP.rotationDegrees(-30));}
        draw(stack,pose,buffers,light,overlay);pose.popPose();
    }
}

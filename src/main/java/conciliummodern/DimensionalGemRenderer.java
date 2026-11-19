package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

/** Original base/top OBJ and surface textures; the captured block is rendered as an actual block item. */
public final class DimensionalGemRenderer extends BlockEntityWithoutLevelRenderer {
    private static DimensionalGemRenderer instance;
    private static final java.util.Map<String,GroupedObjMesh> MESHES=new java.util.HashMap<>();
    private DimensionalGemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static DimensionalGemRenderer instance(){if(instance==null)instance=new DimensionalGemRenderer();return instance;}
    public static void reload(){MESHES.clear();}
    private static GroupedObjMesh mesh(String part){return MESHES.computeIfAbsent(part,key->{try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/gem_"+key+".obj")).openAsReader()){
        return GroupedObjMesh.read(reader);
    }catch(java.io.IOException failure){throw new IllegalStateException("Cannot load original dimensional gem "+key,failure);}});}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        float scale=context==ItemDisplayContext.GUI?.38f:context==ItemDisplayContext.GROUND?.25f:.4f;
        pose.scale(scale,scale,scale);
        if(context==ItemDisplayContext.GUI)pose.mulPose(Axis.XP.rotationDegrees(18));
        if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND){pose.translate(0,.8,.35);pose.mulPose(Axis.ZP.rotationDegrees(-30));}
        draw(stack,context.firstPerson(),false,pose,buffers,light,overlay);pose.popPose();
    }
    public void draw(ItemStack stack,boolean sampleVisible,boolean using,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!(stack.getItem() instanceof DimensionalGem gem))return;
        var mc=Minecraft.getInstance();float ticks=mc.level==null?0:mc.level.getGameTime()+mc.getFrameTime();
        pose.pushPose();if(gem.awakened())pose.mulPose(Axis.YP.rotationDegrees((ticks*(using?9:1))%360));
        part(gem.rift()?"base":"top",pose,buffers,light,overlay);
        var sample=gem.rift()?GemCoordinates.selected(stack):GemCoordinates.captured(stack);
        if(gem.rift()&&sample!=null){pose.pushPose();pose.translate(0,.8,0);pose.scale(.43f,.43f,.43f);part("top",pose,buffers,light,overlay);pose.popPose();}
        if(sampleVisible&&sample!=null&&!sample.display().isEmpty()){
            pose.pushPose();pose.translate(0,-.2,0);pose.scale(.55f,.55f,.55f);
            mc.getItemRenderer().renderStatic(sample.display(),ItemDisplayContext.FIXED,light,overlay,pose,buffers,mc.level,0);pose.popPose();
        }
        pose.popPose();
    }
    private static void part(String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumicconcilium:textures/models/gem_"+part+".png")));var frame=pose.last();
        for(var group:mesh(part).groups().values())for(var v:group)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(255,255,255,255).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
}

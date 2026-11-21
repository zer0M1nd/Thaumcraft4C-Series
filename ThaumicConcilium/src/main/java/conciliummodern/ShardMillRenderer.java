package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

/** Original three OBJ parts and wood textures; no original art is bundled in the modern JAR. */
public final class ShardMillRenderer extends BlockEntityWithoutLevelRenderer {
    private static ShardMillRenderer instance;
    private static final java.util.Map<String,GroupedObjMesh> MESHES=new java.util.HashMap<>();
    private final DestabilizedCrystalRenderer crystal=new DestabilizedCrystalRenderer(null);
    private ShardMillRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ShardMillRenderer instance(){if(instance==null)instance=new ShardMillRenderer();return instance;}
    public static void reload(){MESHES.clear();}
    private static GroupedObjMesh mesh(String part){return MESHES.computeIfAbsent(part,key->{try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/"+key+"_shard_mill.obj")).open()){
        return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
    }catch(java.io.IOException failure){throw new IllegalStateException("Cannot load shard mill part "+key,failure);}});}
    public void draw(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean open,float turn,float load){
        var mill=(ShardMill)stack.getItem();var texture=ResourceLocation.parse("thaumicconcilium:textures/models/"+(mill.silver()?"silverwood":"greatwood")+"_shard_mill.png");
        part("body",texture,pose,buffers,light,overlay);
        if(!open){part("cap",texture,pose,buffers,light,overlay);pose.pushPose();pose.translate(0,1.8,0);pose.mulPose(Axis.XP.rotationDegrees(-turn));pose.translate(0,-1.8,0);part("handle",texture,pose,buffers,light,overlay);pose.popPose();}
        else if(load>=0){pose.pushPose();pose.translate(-.5,1.5-(1-load)*(load>.25?2:1.5),.3);pose.scale(.18f,.48f,.16f);crystal.drawSingle(pose,buffers,light,overlay,mill.selected(stack).color());pose.popPose();}
    }
    private static void part(String name,ResourceLocation texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));var frame=pose.last();
        for(var vertices:mesh(name).groups().values())for(var v:vertices)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(1f,1f,1f,1f).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context==ItemDisplayContext.GUI){pose.scale(.4f,.4f,.4f);pose.mulPose(Axis.XP.rotationDegrees(-65));pose.translate(0,-1,0);}
        else if(context==ItemDisplayContext.GROUND){pose.translate(0,.2,0);pose.scale(.7f,.7f,.7f);}
        else {pose.mulPose(Axis.ZP.rotationDegrees(50));pose.mulPose(Axis.XP.rotationDegrees(-20));pose.mulPose(Axis.YP.rotationDegrees(-90));pose.translate(.5,-1,-1);}
        draw(stack,pose,buffers,light,overlay,false,0,-1);pose.popPose();
    }
}

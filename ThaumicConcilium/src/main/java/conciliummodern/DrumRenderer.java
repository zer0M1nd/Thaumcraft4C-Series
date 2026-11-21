package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

/** Original instrument meshes, loaded externally with the rest of the author's art. */
public final class DrumRenderer extends BlockEntityWithoutLevelRenderer {
    private static DrumRenderer instance;
    private static final GroupedObjMesh[] MESHES=new GroupedObjMesh[2];
    public DrumRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static DrumRenderer instance(){if(instance==null)instance=new DrumRenderer();return instance;}
    public static void reload(){java.util.Arrays.fill(MESHES,null);}
    public void draw(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int index=stack.getItem() instanceof ThaumaturgeDrum drum && drum.warped()?1:0;String name=index==0?"auram_thaum_drum":"warped_thaum_drum";
        if(MESHES[index]==null)try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/"+name+".obj")).open()){
            MESHES[index]=GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot load drum model",exception);}
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumicconcilium:textures/models/"+name+".png")));var frame=pose.last();
        for(var vertices:MESHES[index].groups().values())for(var v:vertices)
            out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(1f,1f,1f,1f).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context==ItemDisplayContext.GUI){pose.translate(0,-.3,0);pose.scale(.6f,.6f,.6f);}
        else if(context==ItemDisplayContext.GROUND)pose.translate(0,.5,0);
        else {pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.toRadians(40),new org.joml.Vector3f(-.5f,0,1).normalize()));pose.translate(.5,0,.5);}
        draw(stack,pose,buffers,light,overlay);pose.popPose();
    }
}

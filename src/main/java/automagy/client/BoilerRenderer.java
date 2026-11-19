package automagy.client;
import automagy.content.BoilerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
public final class BoilerRenderer implements BlockEntityRenderer<BoilerBlockEntity>{
 public BoilerRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(BoilerBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(pose,buffers,light,overlay,tile.burnTime()>0,tile.storedEssentia()>0,true);}
 public static void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean lit,boolean filled){draw(pose,buffers,light,overlay,lit,filled,false);}
 private static void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean lit,boolean filled,boolean world){pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.5f,.5f,.5f);pose.mulPose(Axis.YP.rotationDegrees(45));String texture="boiler"+(lit?"-lit":"")+(filled?"-filled":"");var location=ResourceLocation.parse("automagy:textures/models/"+texture+".png");ExternalObj.BOILER.drawAll(pose,buffers.getBuffer(world?LegacyModelRenderTypes.world(location):RenderType.entityCutout(location)),light,overlay);pose.popPose();}
}

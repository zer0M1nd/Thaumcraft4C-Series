package thmodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.plane.FiniteLightning;
public class FiniteLightningRenderer extends EntityRenderer<FiniteLightning> {
 public FiniteLightningRenderer(EntityRendererProvider.Context context){super(context);}
 @Override public void render(FiniteLightning entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  renderBolt(pose,buffers.getBuffer(RenderType.lightning()),entity.seed,entity.length());
 }
 /** Legacy finite bolts scale their sideways deviations, but not the width of their glowing layers. */
 static void renderBolt(PoseStack pose,VertexConsumer out,long seed,float length){
  float scale=length/128f,x=0,z=0;float[] pathX=new float[8],pathZ=new float[8];var path=new Random(seed);
  for(int i=7;i>=0;i--){pathX[i]=x;pathZ[i]=z;x+=(path.nextInt(11)-5)*scale;z+=(path.nextInt(11)-5)*scale;}
  for(int layer=0;layer<4;layer++){
   var random=new Random(seed);
   for(int branch=0;branch<3;branch++){
    int top=branch==0?7:7-branch,bottom=branch==0?0:top-2;
    float nextX=pathX[top]-x,nextZ=pathZ[top]-z;
    for(int segment=top;segment>=bottom;segment--){
     float oldX=nextX,oldZ=nextZ;int spread=branch==0?11:31;
     nextX+=(random.nextInt(spread)-spread/2)*scale;nextZ+=(random.nextInt(spread)-spread/2)*scale;
     float upperWidth=(.1f+layer*.2f)*(branch==0?segment*.1f+1:1);
     float lowerWidth=(.1f+layer*.2f)*(branch==0?(segment-1)*.1f+1:1);
     // Preserve the actual synchronized height. The old renderer's integer /2 expression truncated short bolts.
     float lowerY=length*segment/8f,upperY=length*(segment+1)/8f;
     for(int side=0;side<4;side++){
      int next=(side+1)%4;
      corner(pose,out,nextX,lowerY,nextZ,lowerWidth,side);corner(pose,out,oldX,upperY,oldZ,upperWidth,side);
      corner(pose,out,oldX,upperY,oldZ,upperWidth,next);corner(pose,out,nextX,lowerY,nextZ,lowerWidth,next);
     }
    }
   }
  }
 }
 private static void corner(PoseStack pose,VertexConsumer out,float x,float y,float z,float width,int corner){
  out.vertex(pose.last().pose(),x+((corner==1||corner==2)?width:-width),y,z+((corner==2||corner==3)?width:-width)).color(.45f,.45f,.5f,.3f).endVertex();
 }
 @Override public ResourceLocation getTextureLocation(FiniteLightning entity){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
}

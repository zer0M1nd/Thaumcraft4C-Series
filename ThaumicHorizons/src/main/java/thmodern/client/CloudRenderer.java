package thmodern.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.plane.CloudEntity;
public final class CloudRenderer implements BlockEntityRenderer<CloudEntity> {
 public CloudRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(CloudEntity cloud,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!cloud.raining()||cloud.getLevel()==null)return;
  String texture=cloud.type().id().equals("fire_cloud")?"thaumichorizons:textures/environment/firerain.png":cloud.warm()?"thaumichorizons:textures/environment/rain.png":"minecraft:textures/environment/snow.png";
  var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture)));int rgb=cloud.type().color();
  float offset=((cloud.getLevel().getGameTime()+partial+cloud.getBlockPos().getX()+16*cloud.getBlockPos().getZ())*.1f)%1;
  float length=Math.max(0,cloud.distance()-1);
  for(int side=0;side<4;side++)for(float top=offset*4;top<length+4;top+=4){float a=Math.max(0,top-4),b=Math.min(length,top);if(b<=a)continue;float v0=(a-top+4)/4,v1=(b-top+4)/4;
   for(int i=0;i<4;i++){float along=i==0||i==3?0:1;float x=side<2?along:side==2?0:1,z=side<2?(side==0?0:1):along,y=i<2?-a:-b;
    out.vertex(pose.last().pose(),x,y,z).color((rgb>>16)&255,(rgb>>8)&255,rgb&255,255).uv(along,i<2?v0:v1).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),side<2?0:1,0,side<2?1:0).endVertex();}
  }
 }
 @Override public boolean shouldRenderOffScreen(CloudEntity cloud){return true;}
 @Override public int getViewDistance(){return 128;}
}

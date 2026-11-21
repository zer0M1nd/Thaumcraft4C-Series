package automagy.client;
import automagy.content.NetherWispEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
public final class NetherWispRenderer extends EntityRenderer<NetherWispEntity>{
 private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/wisp.png"),PARTICLES=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
 public NetherWispRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
 @Override public ResourceLocation getTextureLocation(NetherWispEntity entity){return TEXTURE;}
 @Override public void render(NetherWispEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  if(entity.getHealth()<=0)return;
  int color=entity.aspect().color(),frame=entity.tickCount%16;
  float red=(color>>16&255)/255f,green=(color>>8&255)/255f,blue=(color&255)/255f;
  if(entity.hurtTime>0){red=1;green*=.85f;blue*=.85f;}
  pose.pushPose();pose.translate(0,.45,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());
  billboard(pose,buffers,TEXTURE,1,frame%4/4f,frame/4/4f,.25f,red,green,blue);
  billboard(pose,buffers,PARTICLES,.4f+.1f*(float)Math.sin((entity.tickCount+partial)/10),frame/16f,5/16f,1/16f,1,1,1);
  pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
 }
 private static void billboard(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float size,float u,float v,float step,float red,float green,float blue){
  var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.softParticles(texture));var matrix=pose.last().pose();float half=size/2,right=u+step-.00004f,bottom=v+step-.00004f;
  float[][] vertices={{-half,-half,right,bottom},{half,-half,u,bottom},{half,half,u,v},{-half,half,right,v}};
  for(var p:vertices)out.vertex(matrix,p[0],p[1],0).color(red,green,blue,1).uv(p[2],p[3]).uv2(LightTexture.FULL_BRIGHT).endVertex();
 }
 public static void effects(NetherWispEntity entity){
  if(entity.tickCount<=1)tc4revival.client.ClientFocusEffects.wispBirth(entity.level(),entity.position().add(0,.45,0));
  var random=entity.level().random;
  if(random.nextBoolean())tc4revival.client.ClientFocusEffects.wispMote(entity.level(),entity.position().add((random.nextFloat()-random.nextFloat())*.7,.45+(random.nextFloat()-random.nextFloat())*.7,(random.nextFloat()-random.nextFloat())*.7),entity.aspect().color());
 }
}

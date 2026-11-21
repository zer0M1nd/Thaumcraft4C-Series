package thmodern.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.plane.*;
/** Active frame skin is an overlay; the original frame blocks retain their inventory and break behavior. */
public final class GatewaySlotRenderer implements BlockEntityRenderer<KeystoneSlotEntity> {
 public GatewaySlotRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(KeystoneSlotEntity slot,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(slot.link()==null)return;pose.pushPose();if(!slot.xAligned()){pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-90));pose.translate(-.5,0,-.5);}
  for(var cell:GatewayLayout.cells()){if(cell.material().equals("minecraft:air"))continue;String texture=cell.down()==0?(cell.across()==-2?"topleft":cell.across()==2?"topright":cell.across()<0?"top":"top2"):cell.down()==4?(cell.across()==-2?"bottomleft":cell.across()==2?"bottomright":"bottom"):cell.across()<0?"left":"right";
   var out=buffers.getBuffer(RenderType.entityCutout(ResourceLocation.parse("thaumichorizons:textures/blocks/gateway_"+texture+".png")));pose.pushPose();pose.translate(cell.across(),-cell.down(),0);face(pose,out,-.002f,false,light,overlay);face(pose,out,1.002f,true,light,overlay);pose.popPose();
  }pose.popPose();
 }
 private static void face(PoseStack pose,VertexConsumer out,float z,boolean back,int light,int overlay){for(int i=0;i<4;i++){int index=back?i:3-i;float x=index==0||index==3?0:1,y=index<2?0:1;out.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv(back?1-x:x,1-y).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),0,0,back?1:-1).endVertex();}}
 @Override public boolean shouldRenderOffScreen(KeystoneSlotEntity slot){return true;}
 @Override public int getViewDistance(){return 96;}
}

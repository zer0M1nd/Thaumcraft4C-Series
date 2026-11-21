package wgmodern;
import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.entity.Entity;import com.mojang.blaze3d.vertex.PoseStack;import com.mojang.math.Axis;
public class PhotoRenderer extends BlockEntityWithoutLevelRenderer {
 private static PhotoRenderer instance;private static final LinkedHashMap<CompoundTag,Entity> ENTITIES=new LinkedHashMap<>(16,.75f,true);private static net.minecraft.world.level.Level lastLevel;private static boolean rendering;
 public PhotoRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}public static PhotoRenderer instance(){if(instance==null)instance=new PhotoRenderer();return instance;}
 public static void clear(){ENTITIES.clear();lastLevel=null;}
 public static ResourceLocation texture(ItemStack stack){return new ResourceLocation("witchinggadgets","textures/items/mat_"+(stack.is(Content.PLATE.get())?"photoplate":"developedphoto")+".png");}
 @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();if(display==ItemDisplayContext.GROUND){pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(90));pose.translate(-.5,-.5,0);extruded(stack,pose,buffers,light,overlay);subject(stack,pose,buffers,light,overlay,false);}else quad(stack,false,pose,buffers,0,0,1,1,0,0xffffff,light,overlay);pose.popPose();}
 public static void paper(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){quad(stack,false,pose,buffers,-7,-7,142,142,0,0xffffff,light,overlay);var aspects=PhotoData.aspects(PhotoData.of(stack));int color=0xffffff;if(!aspects.isEmpty()){var sorted=aspects.alphabetical();var player=Minecraft.getInstance().player;int ticks=player==null?0:player.tickCount;color=sorted.get((ticks/68)%sorted.size()).color();}quad(stack,true,pose,buffers,-7,-7,142,142,-.001f,color,light,overlay);subject(stack,pose,buffers,light,overlay,true);}
 /** Atlas UVs select the current 64x64 frame, not the complete 64x576 source sheet. */
 public static void quad(ItemStack stack,boolean overlayLayer,PoseStack pose,MultiBufferSource buffers,float x,float y,float w,float h,float z,int rgb,int light,int overlay){
  // Original int >> 32 wraps to >> 0: its blue byte is also the overlay alpha.
  int alpha=overlayLayer?rgb&255:255;var sprite=PhotoSprites.get(stack,overlayLayer);var out=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
  vertex(out,pose,x,y+h,z,sprite.getU0(),sprite.getV1(),rgb,alpha,light,overlay);
  vertex(out,pose,x+w,y+h,z,sprite.getU1(),sprite.getV1(),rgb,alpha,light,overlay);
  vertex(out,pose,x+w,y,z,sprite.getU1(),sprite.getV0(),rgb,alpha,light,overlay);
  vertex(out,pose,x,y,z,sprite.getU0(),sprite.getV0(),rgb,alpha,light,overlay);
 }
 private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int rgb,int alpha,int light,int overlay){out.vertex(pose.last().pose(),x,y,z).color(rgb>>16&255,rgb>>8&255,rgb&255,alpha).uv(u,v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),0,0,-1).endVertex();}
 /** Original renderItemIn2D: two faces and a texel-center strip at every pixel edge. */
 public static void extruded(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  var sprite=PhotoSprites.get(stack,false);
  var out=buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
  float left=sprite.getU1(),right=sprite.getU0(),top=sprite.getV0(),bottom=sprite.getV1(),back=-.0625f;
  meshVertex(out,pose,0,0,0,left,bottom,0,0,1,light,overlay);
  meshVertex(out,pose,1,0,0,right,bottom,0,0,1,light,overlay);
  meshVertex(out,pose,1,1,0,right,top,0,0,1,light,overlay);
  meshVertex(out,pose,0,1,0,left,top,0,0,1,light,overlay);
  meshVertex(out,pose,0,1,back,left,top,0,0,-1,light,overlay);
  meshVertex(out,pose,1,1,back,right,top,0,0,-1,light,overlay);
  meshVertex(out,pose,1,0,back,right,bottom,0,0,-1,light,overlay);
  meshVertex(out,pose,0,0,back,left,bottom,0,0,-1,light,overlay);
  int width=sprite.contents().width(),height=sprite.contents().height();
  for(int pixel=0;pixel<width;pixel++){
   float x=pixel/(float)width,X=(pixel+1)/(float)width,u=left+(right-left)*(pixel+.5f)/width;
   meshVertex(out,pose,x,0,back,u,bottom,-1,0,0,light,overlay);
   meshVertex(out,pose,x,0,0,u,bottom,-1,0,0,light,overlay);
   meshVertex(out,pose,x,1,0,u,top,-1,0,0,light,overlay);
   meshVertex(out,pose,x,1,back,u,top,-1,0,0,light,overlay);
   meshVertex(out,pose,X,1,back,u,top,1,0,0,light,overlay);
   meshVertex(out,pose,X,1,0,u,top,1,0,0,light,overlay);
   meshVertex(out,pose,X,0,0,u,bottom,1,0,0,light,overlay);
   meshVertex(out,pose,X,0,back,u,bottom,1,0,0,light,overlay);
  }
  for(int pixel=0;pixel<height;pixel++){
   float y=pixel/(float)height,Y=(pixel+1)/(float)height,v=bottom+(top-bottom)*(pixel+.5f)/height;
   meshVertex(out,pose,0,Y,0,left,v,0,1,0,light,overlay);
   meshVertex(out,pose,1,Y,0,right,v,0,1,0,light,overlay);
   meshVertex(out,pose,1,Y,back,right,v,0,1,0,light,overlay);
   meshVertex(out,pose,0,Y,back,left,v,0,1,0,light,overlay);
   meshVertex(out,pose,1,y,0,right,v,0,-1,0,light,overlay);
   meshVertex(out,pose,0,y,0,left,v,0,-1,0,light,overlay);
   meshVertex(out,pose,0,y,back,left,v,0,-1,0,light,overlay);
   meshVertex(out,pose,1,y,back,right,v,0,-1,0,light,overlay);
  }
 }
 private static void meshVertex(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int nx,int ny,int nz,int light,int overlay){out.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),nx,ny,nz).endVertex();}
 private static void subject(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean hand){if(rendering)return;var mc=Minecraft.getInstance();if(mc.level==null)return;var data=PhotoData.of(stack);var item=PhotoData.item(data);pose.pushPose();try{rendering=true;if(!item.isEmpty()){if(item.getItem() instanceof ScanCamera.Photograph)return;if(item.getItem() instanceof BlockItem){if(hand)pose.scale(20,20,20);else{pose.scale(.25f,-.25f,-.25f);pose.translate(-1.25,-4.5,1.625);pose.mulPose(Axis.XP.rotationDegrees(-25));}pose.mulPose(Axis.XP.rotationDegrees(-90));pose.mulPose(Axis.YP.rotationDegrees(-90));pose.mulPose(Axis.ZP.rotationDegrees(65));pose.translate(1.5,-2.4,-3.25);/* Original RenderBlocks.renderBlockAsItem rotates the centered block by 90 degrees. */pose.mulPose(Axis.YP.rotationDegrees(90));mc.getItemRenderer().renderStatic(item,ItemDisplayContext.NONE,light,overlay,pose,buffers,mc.level,0);}else{if(!mc.getItemRenderer().getModel(item,mc.level,mc.player,0).isCustomRenderer()){
 // Original ordinary-item branch followed by 1.7.10 ItemRenderer.renderItemIn2D transforms.
 if(hand)pose.scale(20,20,20);else{pose.scale(.25f,-.25f,-.25f);pose.translate(-1.25,-4.5,1.625);pose.mulPose(Axis.XP.rotationDegrees(-25));}
 pose.mulPose(Axis.XP.rotationDegrees(180));pose.mulPose(Axis.YP.rotationDegrees(-45));pose.mulPose(Axis.ZP.rotationDegrees(-35));pose.translate(3.625,-1.5,-1.75);pose.scale(-1,1,-1);
 pose.translate(0,-.3,0);pose.scale(1.5f,1.5f,1.5f);pose.mulPose(Axis.YP.rotationDegrees(50));pose.mulPose(Axis.ZP.rotationDegrees(335));pose.translate(-.9375,-.0625,0);
 // Modern generated item geometry is centered; legacy extrusion occupied [0,1] with z in [-1/16,0].
 pose.translate(.5,.5,-.03125);
 mc.getItemRenderer().renderStatic(item,ItemDisplayContext.NONE,light,overlay,pose,buffers,mc.level,0);
 }else{if(hand){pose.translate(64,64,-2);pose.scale(56,-56,.05f);}else {pose.translate(.5,.5,-.01);pose.scale(.5f,-.5f,.01f);}mc.getItemRenderer().renderStatic(item,ItemDisplayContext.GUI,light,overlay,pose,buffers,mc.level,0);}}}else {if(mc.level!=lastLevel){clear();lastLevel=mc.level;}Entity entity=ENTITIES.get(data);if(entity==null&&data.contains("entityType")){entity=PhotoData.entity(data,mc.level);if(entity!=null){if(ENTITIES.size()>=64)ENTITIES.remove(ENTITIES.keySet().iterator().next());ENTITIES.put(data.copy(),entity);}}if(entity!=null){float scale=Math.max(entity.getBbHeight()/1.8f,entity.getBbWidth()/1.5f);if(hand){pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(65,-85,2);pose.scale(20/scale,20/scale,.1f);}else{pose.translate(.5,.25,.05);pose.scale(.25f/scale,.25f/scale,.1f/scale);}if(entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon){pose.scale(2,2,1);pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(0,2,0);}mc.getEntityRenderDispatcher().setRenderShadow(false);mc.getEntityRenderDispatcher().render(entity,0,0,0,0,0,pose,buffers,light);mc.getEntityRenderDispatcher().setRenderShadow(true);}}}finally{mc.getEntityRenderDispatcher().setRenderShadow(true);rendering=false;pose.popPose();}}
}

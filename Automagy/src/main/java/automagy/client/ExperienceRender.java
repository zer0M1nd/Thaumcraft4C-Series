package automagy.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
/** Shared translucent XP surface, without global GL state changes. */
final class ExperienceRender {
 static void box(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float lo,float bottom,float hi,float top,float alpha,long time,int overlay){
  boolean atlas=texture.getPath().startsWith("block/");var sprite=atlas?net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture):null;var out=buffers.getBuffer(RenderType.entityTranslucent(atlas?net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS:texture));float red=(float)(Math.sin(time/30.0)+1)*.5f,blue=(float)(Math.sin(time/30.0+4.18879)+1)*.1f;
  float[][][] faces={{{lo,top,lo},{lo,top,hi},{hi,top,hi},{hi,top,lo}},{{lo,bottom,lo},{hi,bottom,lo},{hi,bottom,hi},{lo,bottom,hi}},{{lo,bottom,lo},{lo,top,lo},{hi,top,lo},{hi,bottom,lo}},{{hi,bottom,hi},{hi,top,hi},{lo,top,hi},{lo,bottom,hi}},{{lo,bottom,hi},{lo,top,hi},{lo,top,lo},{lo,bottom,lo}},{{hi,bottom,lo},{hi,top,lo},{hi,top,hi},{hi,bottom,hi}}};
  float[][] normals={{0,1,0},{0,-1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}},uv={{0,1},{0,0},{1,0},{1,1}};var transform=pose.last();
  for(int f=0;f<6;f++)for(int i=0;i<4;i++){var v=faces[f][i];var n=normals[f];out.vertex(transform.pose(),v[0],v[1],v[2]).color(red,1,blue,alpha).uv(sprite==null?uv[i][0]:sprite.getU(uv[i][0]*16),sprite==null?uv[i][1]:sprite.getV(uv[i][1]*16)).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(transform.normal(),n[0],n[1],n[2]).endVertex();}
 }
 static void jar(int xp,PoseStack pose,MultiBufferSource buffers,long time,int overlay){if(xp>0)box(pose,buffers,ResourceLocation.parse("thaumcraft:block/animatedglow"),.25f,.0625f,.75f,.0625f+.625f*Math.min(1,xp/(float)automagy.content.ExperienceJarBlockEntity.CAPACITY),.7f,time,overlay);}
}

package automagy.client;
import automagy.content.ThirstyTankBlockEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
public final class ThirstyTankRenderer implements BlockEntityRenderer<ThirstyTankBlockEntity>{
 private static class Surface {final automagy.logic.FluidLevelAnimation animation=new automagy.logic.FluidLevelAnimation();FluidStack fluid=FluidStack.EMPTY;}
 private final java.util.Map<ThirstyTankBlockEntity,Surface> surfaces=new java.util.WeakHashMap<>();
 public ThirstyTankRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(ThirstyTankBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  int[] glyphs=new int[6];for(var d:Direction.values())glyphs[d.ordinal()]=tile.glyph(d);
  var state=surfaces.computeIfAbsent(tile,key->new Surface());var current=tile.tank.getFluid();if(!current.isEmpty())state.fluid=current.copy();
  double tick=tile.getLevel().getGameTime()+partial;float ratio=state.animation.sample(tick,tile.tank.getFluidAmount()/(float)tile.tank.getCapacity());
  if(ratio<.0001f)state.fluid=FluidStack.EMPTY;
  float wave=(float)Math.sin(tick*.12+(tile.getBlockPos().asLong()&255))*.002f*Math.min(1,ratio*20);
  draw(state.fluid,ratio,wave,glyphs,pose,buffers,light,overlay);
 }
 public static void draw(FluidStack fluid,int capacity,int[] glyphs,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  draw(fluid,fluid.getAmount()/(float)capacity,0,glyphs,pose,buffers,light,overlay);
 }
 private static void draw(FluidStack fluid,float ratio,float wave,int[] glyphs,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!fluid.isEmpty()){
   var ext=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));int color=ext.getTintColor(fluid);
   var output=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));float lo=.15f,hi=.85f,bottom=.116f,top=bottom+.8f*Math.min(1,ratio)+wave;
   quad(pose,output,light,overlay,color,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new float[][]{{lo,top,lo},{lo,top,hi},{hi,top,hi},{hi,top,lo}},0,1,0);
   quad(pose,output,light,overlay,color,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new float[][]{{lo,bottom,lo},{hi,bottom,lo},{hi,bottom,hi},{lo,bottom,hi}},0,-1,0);
   quad(pose,output,light,overlay,color,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new float[][]{{lo,bottom,lo},{lo,top,lo},{hi,top,lo},{hi,bottom,lo}},0,0,-1);
   quad(pose,output,light,overlay,color,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new float[][]{{hi,bottom,hi},{hi,top,hi},{lo,top,hi},{lo,bottom,hi}},0,0,1);
   quad(pose,output,light,overlay,color,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new float[][]{{lo,bottom,hi},{lo,top,hi},{lo,top,lo},{lo,bottom,lo}},-1,0,0);
   quad(pose,output,light,overlay,color,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new float[][]{{hi,bottom,lo},{hi,top,lo},{hi,top,hi},{hi,bottom,hi}},1,0,0);
  }
  for(var side:Direction.values()){int glyph=glyphs[side.ordinal()];if(glyph<1||glyph>9)continue;pose.pushPose();pose.translate(.5+side.getStepX()*.376,.5+side.getStepY()*.501,.5+side.getStepZ()*.376);pose.mulPose(side.getRotation());var output=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("automagy:textures/block/tank_glyph_"+glyph+".png")));quad(pose,output,light,overlay,0xffffffff,1,0,0,1,new float[][]{{-.3f,0,-.3f},{-.3f,0,.3f},{.3f,0,.3f},{.3f,0,-.3f}},0,1,0);pose.popPose();}
 }
 private static void quad(PoseStack pose,VertexConsumer out,int light,int overlay,int color,float u0,float v0,float u1,float v1,float[][] points,float nx,float ny,float nz){var transform=pose.last();float[][] uv={{u0,v1},{u0,v0},{u1,v0},{u1,v1}};for(int i=0;i<4;i++)out.vertex(transform.pose(),points[i][0],points[i][1],points[i][2]).color((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255).uv(uv[i][0],uv[i][1]).overlayCoords(overlay).uv2(light).normal(transform.normal(),nx,ny,nz).endVertex();}
}

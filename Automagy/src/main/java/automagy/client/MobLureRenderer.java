package automagy.client;
import automagy.content.MobLureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
public final class MobLureRenderer implements BlockEntityRenderer<MobLureBlockEntity>{
 private static final int[] COLORS={0xffff7e,0xff5a01,0x3cd4fc,0x56c000,0xd5d4ec,0x404040};
 public MobLureRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(MobLureBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!tile.enabled())return;renderGlow(tile.getLevel()==null?0:tile.getLevel().getGameTime(),partial,pose,buffers,overlay);}
 public static void renderGlow(long ticks,float partial,PoseStack pose,MultiBufferSource buffers,int overlay){int index=(int)(ticks/250%6);float t=(ticks%250+partial)/250f;int a=COLORS[index],b=COLORS[(index+1)%6];float red=((a>>16&255)*(1-t)+(b>>16&255)*t)/255,green=((a>>8&255)*(1-t)+(b>>8&255)*t)/255,blue=((a&255)*(1-t)+(b&255)*t)/255;
  var out=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.resources.ResourceLocation.parse("automagy:textures/block/mob_lure_overlay.png")));var m=pose.last();float l=-.001f,h=1.001f;float[][][] faces={{{l,h,l},{l,h,h},{h,h,h},{h,h,l}},{{l,l,l},{h,l,l},{h,l,h},{l,l,h}},{{l,l,l},{l,h,l},{h,h,l},{h,l,l}},{{h,l,h},{h,h,h},{l,h,h},{l,l,h}},{{l,l,h},{l,h,h},{l,h,l},{l,l,l}},{{h,l,l},{h,h,l},{h,h,h},{h,l,h}}};float[][] normals={{0,1,0},{0,-1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}},uv={{0,1},{0,0},{1,0},{1,1}};for(int f=0;f<6;f++)for(int i=0;i<4;i++){var v=faces[f][i];out.vertex(m.pose(),v[0],v[1],v[2]).color(red,green,blue,1).uv(uv[i][0],uv[i][1]).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(m.normal(),normals[f][0],normals[f][1],normals[f][2]).endVertex();}
 }
}

package automagy.client;
import automagy.content.InfinityJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
public final class InfinityJarRenderer implements BlockEntityRenderer<InfinityJarBlockEntity> {
 public InfinityJarRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(InfinityJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
    if(jar.aspect()==null||jar.amount()<=0)return;
    float bottom=.0725f,top=bottom+.625f*jar.amount()/64f;
    var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/animatedglow"));
    var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.ESSENTIA_LIQUID);int color=jar.aspect().color();
    float[][] vertices={{.25f,bottom,.25f},{.75f,bottom,.25f},{.75f,bottom,.75f},{.25f,bottom,.75f},{.25f,top,.25f},{.75f,top,.25f},{.75f,top,.75f},{.25f,top,.75f}};
    int[][] faces={{0,1,2,3},{7,6,5,4},{1,0,4,5},{3,2,6,7},{0,3,7,4},{2,1,5,6}};
    for(int f=0;f<6;f++)for(int index:faces[f]){var v=vertices[index];float u=f<4?v[0]:v[2],w=f<2?v[2]:1-v[1];out.vertex(pose.last().pose(),v[0],v[1],v[2]).color(color>>16&255,color>>8&255,color&255,255).uv(sprite.getU(u*16),sprite.getV(w*16)).uv2(Math.max(200,light)).endVertex();}
 }
}

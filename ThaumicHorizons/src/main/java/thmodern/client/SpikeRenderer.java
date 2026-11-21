package thmodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import thmodern.plane.*;
public final class SpikeRenderer implements BlockEntityRenderer<PlanarDecorations.SpikeEntity> {
 public SpikeRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(PlanarDecorations.SpikeEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){render(entity.getBlockState(),pose,buffers,light,overlay);}
 public static void render(BlockState state,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!(state.getBlock() instanceof SpikeBlock spike))return;
  pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(state.getValue(SpikeBlock.FACING).getRotation());pose.scale(.35f,.35f,.35f);pose.translate(0,-1.45,0);
  ExternalMesh.render(ResourceLocation.parse("thaumichorizons:models/spike.obj"),ResourceLocation.parse("thaumichorizons:textures/models/"+spike.kind()+"spike.png"),pose,buffers,light,overlay);pose.popPose();
 }
}

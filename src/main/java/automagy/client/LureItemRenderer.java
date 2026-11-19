package automagy.client;
import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
public final class LureItemRenderer extends BlockEntityWithoutLevelRenderer {
 public LureItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!(stack.getItem() instanceof LureBlockItem item))return;
  var client=Minecraft.getInstance();client.getBlockRenderer().renderSingleBlock(item.getBlock().defaultBlockState(),pose,buffers,light,overlay);
  if(item.getBlock() instanceof MobLureBlock)MobLureRenderer.renderGlow(client.level==null?0:client.level.getGameTime(),client.getFrameTime(),pose,buffers,overlay);
  else DimensionLureRenderer.renderGlow(pose,buffers,overlay,1);
 }
}

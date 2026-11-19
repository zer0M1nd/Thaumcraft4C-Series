package automagy.client;
import automagy.content.Content;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
public final class ThirstyTankItemRenderer extends BlockEntityWithoutLevelRenderer {
 public ThirstyTankItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Content.THIRSTY_TANK.get().defaultBlockState(),pose,buffers,light,overlay);
  var tag=stack.getTagElement("BlockEntityTag");if(tag==null)return;int[] glyphs=new int[6];var saved=tag.getIntArray("Glyphs");for(int i=0;i<Math.min(6,saved.length);i++)glyphs[i]=saved[i];
  ThirstyTankRenderer.draw(net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(tag.getCompound("Tank")),automagy.logic.TankGlyphs.from(glyphs).capacity(),glyphs,pose,buffers,light,overlay);
 }
}

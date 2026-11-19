package automagy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class InventariumItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final InventariumRenderer renderer=new InventariumRenderer(null);
    public InventariumItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof automagy.content.EssentiaLocusBlock)renderer.draw(0,false,net.minecraft.resources.ResourceLocation.parse("automagy:textures/models/brain_water.png"),.4f,pose,buffers,light,overlay);else renderer.draw(0,false,pose,buffers,light,overlay);}
}

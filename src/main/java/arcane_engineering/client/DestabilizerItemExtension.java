package arcane_engineering.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class DestabilizerItemExtension implements IClientItemExtensions {
    private BlockEntityWithoutLevelRenderer renderer;
    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if(renderer==null) renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            private final ModelPart model=DestabilizerModel.createBodyLayer().bakeRoot();
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) { DestabilizerRenderer.draw(model,false,0,pose,buffers,light,overlay); }
        };
        return renderer;
    }
}

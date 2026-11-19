package gadomancymodern.item;
import net.minecraft.world.item.*;
import net.minecraft.client.renderer.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public class InfusionClawItem extends BlockItem {
    public InfusionClawItem(){super(gadomancymodern.Gadomancy.CLAW.get(),new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){protected BlockEntityWithoutLevelRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=net.minecraft.client.Minecraft.getInstance();var model=new gadomancymodern.client.InfusionClawRenderer(null);renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){@Override public void renderByItem(ItemStack stack,ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){model.draw(null,0,pose,buffers,light,overlay);}};}return renderer;}});}
}

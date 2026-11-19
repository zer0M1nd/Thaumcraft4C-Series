package gadomancymodern.item;
import gadomancymodern.Gadomancy;
import net.minecraft.world.item.*;
public class ManipulationFocusItem extends BlockItem {
    public final boolean portal;public ManipulationFocusItem(boolean portal){super(portal?Gadomancy.PORTAL_FOCUS.get():Gadomancy.RANDOMIZATION_FOCUS.get(),new Properties());this.portal=portal;}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=net.minecraft.client.Minecraft.getInstance();var model=new gadomancymodern.client.ManipulationFocusRenderer(null);renderer=new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){@Override public void renderByItem(ItemStack stack,ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){model.draw(portal,pose,buffers,light,overlay);}};}return renderer;}});}
}

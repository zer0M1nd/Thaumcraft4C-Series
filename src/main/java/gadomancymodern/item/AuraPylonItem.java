package gadomancymodern.item;
import gadomancymodern.Gadomancy;
import net.minecraft.world.item.*;
import net.minecraft.client.renderer.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public class AuraPylonItem extends BlockItem {
    protected final boolean cap;
    public AuraPylonItem(boolean cap){super(cap?Gadomancy.PYLON_CAP.get():Gadomancy.PYLON.get(),new Properties().rarity(Rarity.EPIC));this.cap=cap;}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){private BlockEntityWithoutLevelRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=net.minecraft.client.Minecraft.getInstance();var model=new gadomancymodern.client.AuraPylonRenderer(null);renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){@Override public void renderByItem(ItemStack stack,ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){model.draw(cap,!cap,pose,buffers,light,overlay);}};}return renderer;}});}
}

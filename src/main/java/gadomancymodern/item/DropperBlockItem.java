package gadomancymodern.item;
import gadomancymodern.Gadomancy;
import net.minecraft.world.item.*;
import net.minecraft.client.renderer.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public class DropperBlockItem extends BlockItem {
    public DropperBlockItem(){super(Gadomancy.DROPPER.get(),new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){
        private BlockEntityWithoutLevelRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
            if(renderer==null){var mc=net.minecraft.client.Minecraft.getInstance();var model=new gadomancymodern.client.DropperRenderer(null);
                renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){model.draw(net.minecraft.core.Direction.DOWN,false,pose,buffers,light,overlay);}
                };
            }return renderer;
        }
    });}
}

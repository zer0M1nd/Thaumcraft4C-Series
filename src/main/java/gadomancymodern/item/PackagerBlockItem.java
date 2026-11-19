package gadomancymodern.item;

import gadomancymodern.Gadomancy;
import gadomancymodern.client.PackagerRenderer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class PackagerBlockItem extends BlockItem {
    public PackagerBlockItem(){super(Gadomancy.PACKAGER.get(),new Item.Properties());}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null){var client=Minecraft.getInstance();renderer=new BlockEntityWithoutLevelRenderer(client.getBlockEntityRenderDispatcher(),client.getEntityModels()){
                    private final PackagerRenderer model=new PackagerRenderer(null);
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay){model.renderModel(-1,270,pose,buffers,light,overlay);}
                };}return renderer;
            }
        });
    }
}

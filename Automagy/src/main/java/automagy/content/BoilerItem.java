package automagy.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class BoilerItem extends BlockItem {
    public BoilerItem(Block block) { super(block,new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new automagy.client.BoilerItemRenderer();return renderer;
            }
        });
    }
}

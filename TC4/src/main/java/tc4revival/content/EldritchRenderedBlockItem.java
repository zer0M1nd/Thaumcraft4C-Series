package tc4revival.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public  class EldritchRenderedBlockItem extends BlockItem {
    public EldritchRenderedBlockItem(Block block) { super(block,new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.EldritchItemRenderer.instance(); }
        });
    }
}

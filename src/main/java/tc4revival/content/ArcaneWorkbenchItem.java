package tc4revival.content;

import net.minecraft.world.item.BlockItem;

public  class ArcaneWorkbenchItem extends LegacyWoodBlockItem {
    public ArcaneWorkbenchItem() { super(TCArcane.BLOCK.get(), new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.ArcaneWorkbenchRenderer.itemRenderer(); }
        });
    }
}

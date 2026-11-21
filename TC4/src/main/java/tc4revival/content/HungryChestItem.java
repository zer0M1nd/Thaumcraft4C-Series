package tc4revival.content;

public  class HungryChestItem extends LegacyWoodBlockItem {
    public HungryChestItem(net.minecraft.world.level.block.Block block,Properties properties) { super(block,properties); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.HungryChestRenderer.itemRenderer(); }
        });
    }
}

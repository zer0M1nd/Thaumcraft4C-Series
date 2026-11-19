package tc4revival.content;

public  class CrystalClusterItem extends net.minecraft.world.item.BlockItem {
    public CrystalClusterItem(CrystalClusterBlock block) { super(block,new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.CrystalClusterRenderer.itemRenderer(); }
        });
    }
}

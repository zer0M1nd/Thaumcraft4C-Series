package tbmodern;

public class NodeDeviceItem extends net.minecraft.world.item.BlockItem {
    public NodeDeviceItem(net.minecraft.world.level.block.Block block) { super(block,new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tbmodern.client.NodeDeviceModels.instance(); }
        });
    }
}

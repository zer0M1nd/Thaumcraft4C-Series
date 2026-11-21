package tc4revival.content;

public  class VisRelayItem extends net.minecraft.world.item.BlockItem {
    public VisRelayItem(boolean charger) { super(charger ? TCArcane.WORKBENCH_CHARGER.get() : TCArcane.VIS_RELAY.get(), new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return tc4revival.client.VisRelayRenderer.itemRenderer();
            }
        });
    }
}

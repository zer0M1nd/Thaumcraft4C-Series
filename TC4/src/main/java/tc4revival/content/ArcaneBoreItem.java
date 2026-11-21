package tc4revival.content;

public  class ArcaneBoreItem extends LegacyWoodBlockItem {
    public ArcaneBoreItem(boolean base) { super(base?TCArcane.ARCANE_BORE_BASE.get():TCArcane.ARCANE_BORE.get(),new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.ArcaneBoreItemRenderer.instance(); }
        });
    }
}

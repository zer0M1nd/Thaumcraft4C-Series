package thaumicomniverse.content;

public class ConvenientTubeItem extends tc4revival.content.EssentiaTubeItem {
    public ConvenientTubeItem(ConvenientTubeBlock block) { super(block); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return thaumicomniverse.client.ConvenientTubeRenderer.itemRenderer();
            }
        });
    }
}

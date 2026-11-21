package tc4revival.content;

import java.util.function.Consumer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** The picked research variant retains its old icon, but places a plain table as TC4 did. */
public  class TableItem extends LegacyWoodBlockItem {
    private final boolean research;
    public TableItem(boolean research) { super(TCTables.TABLE.get(), new Properties()); this.research = research; }
    public boolean research() { return research; }
    @Override public void registerBlocks(java.util.Map<net.minecraft.world.level.block.Block, net.minecraft.world.item.Item> map,
            net.minecraft.world.item.Item item) {
        if (!research) super.registerBlocks(map, item);
    }
    @Override public String getDescriptionId() { return research ? "block.thaumcraft.research_table" : "block.thaumcraft.table"; }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return tc4revival.client.TableItemRenderer.instance();
            }
        });
    }
}

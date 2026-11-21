package tc4revival.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Separate modern item identities preserve the five old metadata variants. */
public  class TaintFibreItem extends BlockItem {
    private final int variant;
    public TaintFibreItem(int variant) { super(TCArcane.TAINT_FIBRES.get(),new Properties()); this.variant=variant; }
    @Override public String getDescriptionId() {
        return variant==0 ? super.getDescriptionId() : "item.thaumcraft."+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
    }
    @Override public void registerBlocks(java.util.Map<net.minecraft.world.level.block.Block,net.minecraft.world.item.Item> map,net.minecraft.world.item.Item item) {
        if (variant==0) super.registerBlocks(map,item);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        var state=super.getPlacementState(context); return state==null ? null : state.setValue(TaintFibreBlock.VARIANT,variant);
    }
}

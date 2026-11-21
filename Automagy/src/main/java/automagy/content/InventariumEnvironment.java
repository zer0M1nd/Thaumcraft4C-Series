package automagy.content;

import automagy.api.InventariumShelf;
import automagy.logic.InventariumStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Data-pack extensible structure materials; never reads block states through unloaded chunks. */
public final class InventariumEnvironment implements InventariumStructure.View {
    public static final TagKey<Block> SHELVES=tag("inventarium_shelves");
    public static final TagKey<Block> AMBER=tag("inventarium_amber");
    public static final TagKey<Block> ORDER_CRYSTALS=tag("inventarium_order_crystals");
    public static final TagKey<Block> MEMORY=tag("inventarium_memory");
    private static final InventariumStructure.Cell UNKNOWN=new InventariumStructure.Cell(false,false,false,false,0,false);
    private final Level level;
    private final BlockPos origin;
    public InventariumEnvironment(Level level,BlockPos origin){this.level=level;this.origin=origin.immutable();}
    @Override public InventariumStructure.Cell at(int x,int y,int z){
        BlockPos position=origin.offset(x,y,z);
        if(level.isOutsideBuildHeight(position))return new InventariumStructure.Cell(true,false,false,false,0,false);
        if(!level.hasChunkAt(position))return UNKNOWN;
        var state=level.getBlockState(position);
        int slots=state.getBlock() instanceof InventariumShelf shelf?shelf.inventariumSlots(state,level,position)
                :state.is(SHELVES)||automagy.AutomagyConfig.matches(state,automagy.AutomagyConfig.EXTRA_SHELVES)?81:0;
        return new InventariumStructure.Cell(true,state.isAir(),state.is(AMBER),state.is(ORDER_CRYSTALS),slots,state.is(MEMORY));
    }
    private static TagKey<Block> tag(String name){return TagKey.create(Registries.BLOCK,new ResourceLocation("automagy",name));}
}

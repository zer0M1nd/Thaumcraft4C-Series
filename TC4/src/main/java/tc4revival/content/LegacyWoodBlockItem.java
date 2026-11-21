package tc4revival.content;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

/** TC4 wooden block items took vanilla 1.7.10's material-based 300-tick fuel branch. */
public class LegacyWoodBlockItem extends BlockItem {
    public LegacyWoodBlockItem(Block block,Properties properties) {super(block,properties);}
    @Override public int getBurnTime(ItemStack stack,RecipeType<?> recipeType) {return 300;}
}

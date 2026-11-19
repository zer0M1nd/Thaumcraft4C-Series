package ttmodern;

import net.minecraft.world.level.block.state.BlockState;

/** Shared awakened modes without replacing the native axe's enchantment category. */
public interface AwakenedMiningTool {
    String kind();
    default boolean effective(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":awakened_"+kind()+"_mineable")));
    }
}

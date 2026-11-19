package thaumicfutures.compat.bees;

import forestry.api.climate.IClimateProvider;
import forestry.api.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class FeatureMutationCondition implements IMutationCondition {
    @Override public float modifyChance(Level level, BlockPos pos, IMutation<?> mutation,
            IGenome first, IGenome second, IClimateProvider climate, float chance) {
        return thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()?chance:0;
    }
    @Override public Component getDescription() { return Component.translatable("omniverse.bees.feature_required"); }
}

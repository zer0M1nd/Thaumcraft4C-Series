package thaumicfutures.compat.bees;

import forestry.api.climate.IClimateProvider;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.IMutationCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Inclusive cyclic moon intervals; the legacy bee calendar changes at noon. */
public record MoonMutationCondition(int first, int last, float bonus, boolean required) implements IMutationCondition {
    public MoonMutationCondition {
        if (first < 0 || first > 7 || last < 0 || last > 7 || !Float.isFinite(bonus) || bonus < 0)
            throw new IllegalArgumentException("Invalid lunar mutation condition");
    }
    public static MoonMutationCondition require(int first, int last) {
        return new MoonMutationCondition(first, last, 1, true);
    }
    public static int phase(long time) {
        // Java truncation matches the original around the initial morning, while floorMod handles negative dates.
        long days = time / 24000L;
        long remainder = time % 24000L;
        if (time >= 24000L && remainder < 6000L) --days;
        else if (time < 0 && remainder <= -18000L) --days;
        return (int) Math.floorMod(days, 8L);
    }
    public boolean includes(int phase) {
        return first <= last ? phase >= first && phase <= last : phase >= first || phase <= last;
    }
    @Override public float modifyChance(Level level, BlockPos pos, IMutation<?> mutation,
            IGenome firstGenome, IGenome secondGenome, IClimateProvider climate, float chance) {
        return includes(phase(level.getDayTime())) ? chance * bonus : required ? 0 : chance;
    }
    @Override public Component getDescription() {
        return Component.translatable(required ? "omniverse.bees.moon_required" : "omniverse.bees.moon_bonus",
                Component.translatable("omniverse.bees.moon." + first),
                Component.translatable("omniverse.bees.moon." + last), bonus);
    }
}

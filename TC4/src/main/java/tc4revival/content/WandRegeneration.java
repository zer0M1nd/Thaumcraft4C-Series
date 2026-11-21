package tc4revival.content;

import java.util.Optional;
import java.util.function.IntUnaryOperator;
import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;

/** Called by a future wand's server inventory tick using player ticks and world RNG. */
public  class WandRegeneration {
    private WandRegeneration() {}
    public static Optional<String> tick(CompoundTag nbt, int playerTicks, AspectCatalog aspects, IntUnaryOperator random) {
        return tick(nbt,playerTicks,aspects,random,WandParts.rod(nbt),WandParts.capacity(nbt));
    }
    public static Optional<String> tick(CompoundTag nbt,int playerTicks,AspectCatalog aspects,IntUnaryOperator random,WandParts.Rod rod,int capacity) {
        if (!rod.rechargeable) return Optional.empty();
        Aspect selected;
        if (rod.regenerates != null) {
            if (playerTicks % rod.regenerationInterval != 0) return Optional.empty();
            selected = aspects.require(rod.regenerates);
            if (WandVis.stored(nbt, selected) >= capacity / 10) return Optional.empty();
        } else if (rod.regenerateAll) {
            if (playerTicks % rod.regenerationInterval != 0) return Optional.empty();
            var candidates = aspects.all().stream().filter(Aspect::isPrimal).filter(a -> WandVis.stored(nbt, a) < capacity / 10).toList();
            if (candidates.isEmpty()) return Optional.empty();
            selected = candidates.get(random.applyAsInt(candidates.size()));
        } else return Optional.empty();
        WandVis.add(nbt, selected, Math.min(100,capacity/10-WandVis.stored(nbt,selected)), capacity, false, true);
        return Optional.of(selected.tag());
    }
}

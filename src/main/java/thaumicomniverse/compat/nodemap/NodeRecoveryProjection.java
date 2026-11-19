package thaumicomniverse.compat.nodemap;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.*;
import tc4revival.content.AuraNodeModifier;

/** Bounds, not invented random outcomes: the core chooses an aspect on the next loaded tick. */
public final class NodeRecoveryProjection {
    public record Range(int minimum, int maximum, int capacity) {}
    public record Projection(long total, long capacity, Map<Aspect, Range> aspects) {}
    public static Projection project(CompoundTag state, int lock, long now, boolean offline) {
        var stored = AspectNbt.read(state, TCAspects.catalog()).amounts();
        var base = AspectNbt.read(state, "AspectsBase", TCAspects.catalog()).amounts();
        long missing = 0;
        for (var aspect : stored.aspects()) missing += Math.max(0, base.amount(aspect) - stored.amount(aspect));
        var modifier = AuraNodeModifier.fromId(state.getByte("modifier"));
        int period = modifier == null ? 600 : switch (modifier) { case BRIGHT -> 400; case PALE -> 900; case FADING -> 0; };
        long elapsed = Math.max(0, now - state.getLong("lastActive")), recovery = 0;
        if (offline && period > 0 && state.getLong("lastActive") > 0 && elapsed > period * 75L) {
            long interval = period * (lock == 1 ? 2L : lock == 2 ? 20L : 1L) * 75;
            recovery = Math.min(missing, Math.min(base.total(), elapsed / interval));
        }
        var ranges = new LinkedHashMap<Aspect, Range>();
        for (var aspect : base.aspects()) {
            int before = stored.amount(aspect), room = stored.contains(aspect) ? Math.max(0, base.amount(aspect) - before) : 0;
            int min = before + (int) Math.max(0, recovery - (missing - room));
            int max = before + (int) Math.min(recovery, room);
            ranges.put(aspect, new Range(min, max, base.amount(aspect)));
        }
        return new Projection(stored.total() + recovery, base.total(), ranges);
    }
}

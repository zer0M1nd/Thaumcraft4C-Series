package temodern.essentia;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;
import thaumcraft.api.aspects.IEssentiaStorage;

/** The supplied handle re-resolves the container and side on every call. */
public final class EssentiaStorage implements MEStorage {
    private final IEssentiaStorage storage;
    private final boolean extractableOnly;
    private final Runnable changed;
    public EssentiaStorage(IEssentiaStorage storage, boolean extractableOnly, Runnable changed) {
        this.storage = storage; this.extractableOnly = extractableOnly; this.changed = changed;
    }
    @Override public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        if (!(what instanceof EssentiaKey key) || key.aspect() == null) return 0;
        int moved = storage.insert(key.aspect(), (int) Math.min(Integer.MAX_VALUE, amount), mode == Actionable.SIMULATE);
        if (moved > 0 && mode == Actionable.MODULATE) changed.run();
        return moved;
    }
    @Override public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        if (!(what instanceof EssentiaKey key) || key.aspect() == null) return 0;
        int moved = storage.extract(key.aspect(), (int) Math.min(Integer.MAX_VALUE, amount), mode == Actionable.SIMULATE);
        if (moved > 0 && mode == Actionable.MODULATE) changed.run();
        return moved;
    }
    @Override public void getAvailableStacks(KeyCounter out) {
        storage.contents().view().forEach((aspect, amount) -> {
            int visible = extractableOnly ? storage.extract(aspect, Math.max(0, amount), true) : amount;
            if (visible > 0) out.add(EssentiaKey.of(aspect), visible);
        });
    }
    @Override public Component getDescription() { return Component.translatable("gui.thaumicenergistics.container"); }
}

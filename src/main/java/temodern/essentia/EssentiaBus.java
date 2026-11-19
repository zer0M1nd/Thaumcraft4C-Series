package temodern.essentia;

import appeng.api.behaviors.*;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumcraft.api.aspects.IEssentiaStorage;

/** Additional strategy for AE2's ordinary buses, including filters, operation budgets and power. */
public final class EssentiaBus implements StackImportStrategy, StackExportStrategy {
    private final ServerLevel level;
    private final BlockPos target;
    private final Direction side;
    private final IEssentiaStorage container;
    public EssentiaBus(ServerLevel level, BlockPos target, Direction side) {
        this.level = level; this.target = target.immutable(); this.side = side;
        container = EssentiaStorageApi.atInventory(level, target);
    }
    public static void register() {
        EssentiaStorageApi.register(new net.minecraft.resources.ResourceLocation(temodern.TE.ID, "reservoir"),
                (entity, side) -> entity instanceof tc4revival.content.EssentiaReservoirBlockEntity reservoir
                        ? new ReservoirStorage(reservoir) : null);
        StackImportStrategy.register(EssentiaKeyType.INSTANCE, EssentiaBus::new);
        StackExportStrategy.register(EssentiaKeyType.INSTANCE, EssentiaBus::new);
        ExternalStorageStrategy.register(EssentiaKeyType.INSTANCE, (level, target, side) ->
                (extractableOnly, changed) -> new EssentiaStorage(EssentiaStorageApi.atInventory(level, target), extractableOnly, changed));
    }
    @Override public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(EssentiaKeyType.INSTANCE) || !context.hasOperationsLeft()) return false;
        var data = BusEscrow.get(level);
        var ledger = data.at(target, side, false);
        boolean moved = false;
        // Already extracted resources must be accounted for before extracting anything else.
        for (String tag : ledger.snapshot().keySet()) {
            var key = EssentiaKey.parse(tag);
            if (key == null || !context.hasOperationsLeft()) continue;
            long inserted = ledger.deliver(tag, n -> StorageHelper.poweredInsert(context.getEnergySource(),
                    context.getInternalStorage().getInventory(), key, Math.min(n, context.getOperationsRemaining()), context.getActionSource()));
            if (inserted > 0) { context.reduceOperationsRemaining(inserted); data.setDirty(); moved = true; }
        }
        if (!ledger.snapshot().isEmpty()) return moved;
        for (var aspect : container.contents().aspects()) {
            if (!context.hasOperationsLeft()) break;
            var key = EssentiaKey.of(aspect);
            if (context.isInFilter(key) == context.isInverted()) continue;
            long accepted = StorageHelper.poweredInsert(context.getEnergySource(), context.getInternalStorage().getInventory(),
                    key, context.getOperationsRemaining(), context.getActionSource(), Actionable.SIMULATE);
            int taken = container.extract(aspect, (int) accepted, false);
            if (taken == 0) continue;
            ledger.retain(key.tag(), taken); data.setDirty();
            long inserted = ledger.deliver(key.tag(), n -> StorageHelper.poweredInsert(context.getEnergySource(),
                    context.getInternalStorage().getInventory(), key, n, context.getActionSource()));
            ledger.deliver(key.tag(), n -> container.insert(aspect, (int) n, false));
            data.setDirty();
            context.reduceOperationsRemaining(Math.max(1, inserted)); moved = true;
            if (!ledger.snapshot().isEmpty()) break;
        }
        return moved;
    }
    @Override public long transfer(StackTransferContext context, AEKey what, long maxAmount) {
        if (!(what instanceof EssentiaKey key) || key.aspect() == null || maxAmount <= 0
                || !context.isKeyTypeEnabled(EssentiaKeyType.INSTANCE)) return 0;
        long budget = Math.min(maxAmount, context.getOperationsRemaining());
        if (budget <= 0) return 0;
        var data = BusEscrow.get(level);
        var ledger = data.at(target, side, true);
        long delivered = ledger.deliver(key.tag(), n -> push(key, Math.min(budget, n), Actionable.MODULATE));
        if (delivered > 0) data.setDirty();
        if (!ledger.snapshot().isEmpty()) return delivered;
        long fit = push(key, budget - delivered, Actionable.SIMULATE);
        long taken = StorageHelper.poweredExtraction(context.getEnergySource(), context.getInternalStorage().getInventory(),
                key, fit, context.getActionSource());
        if (taken == 0) return delivered;
        ledger.retain(key.tag(), taken); data.setDirty();
        delivered += ledger.deliver(key.tag(), n -> push(key, n, Actionable.MODULATE));
        ledger.deliver(key.tag(), n -> context.getInternalStorage().getInventory().insert(key, n, Actionable.MODULATE, context.getActionSource()));
        data.setDirty();
        return delivered;
    }
    @Override public long push(AEKey what, long maxAmount, Actionable mode) {
        if (!(what instanceof EssentiaKey key) || key.aspect() == null || maxAmount <= 0) return 0;
        return container.insert(key.aspect(), (int) Math.min(Integer.MAX_VALUE, maxAmount), mode == Actionable.SIMULATE);
    }
}

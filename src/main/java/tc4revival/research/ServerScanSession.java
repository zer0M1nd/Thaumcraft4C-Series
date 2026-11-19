package tc4revival.research;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.research.PlayerKnowledge.ScanMode;

/** Internal lifecycle for one player. The tool must select a real visible target and cancel on release. */
public  class ServerScanSession {
    public record Update(ScanProgress.State state, Optional<ScanRules.Result> result, Optional<String> clue) {
        public Update(ScanProgress.State state, Optional<ScanRules.Result> result) { this(state, result, Optional.empty()); }
    }
    private final ServerPlayer player;
    private final Supplier<Optional<ScanTarget>> resolve;
    private final ScanProgress progress;

    public ServerScanSession(ServerPlayer player) { this(player, () -> ScanTargetSelector.resolve(player)); }

    public ServerScanSession(ServerPlayer player, Supplier<Optional<ScanTarget>> resolve) {
        this(player,resolve,ScanProgress.DURATION);
    }
    public ServerScanSession(ServerPlayer player, Supplier<Optional<ScanTarget>> resolve,int duration) {
        progress=new ScanProgress(duration, ResearchCatalogs.rules());
        this.player = java.util.Objects.requireNonNull(player);
        this.resolve = java.util.Objects.requireNonNull(resolve);
    }
    public void cancel() { requireThread(); progress.cancel(); }
    public boolean active() { requireThread(); return progress.active(); }
    public ScanProgress.State begin() {
        requireThread();
        progress.cancel();
        if (!available()) return ScanProgress.State.IDLE;
        var data = KnowledgeSavedData.get(player.getServer());
        data.ensure(player.getUUID(), player.getRandom()::nextInt);
        return progress.begin(player.level().getGameTime(), dimension(), resolve.get().orElse(null), data.snapshot(player.getUUID()).orElseThrow());
    }
    public Update tick() {
        requireThread();
        if (!progress.active()) return new Update(ScanProgress.State.IDLE, Optional.empty());
        if (!available()) {
            progress.cancel();
            return new Update(ScanProgress.State.INTERRUPTED, Optional.empty());
        }
        var data = KnowledgeSavedData.get(player.getServer());
        var knowledge = data.snapshot(player.getUUID()).orElseThrow();
        var target = resolve.get().orElse(null);
        var state = progress.observe(player.level().getGameTime(), dimension(), target, knowledge);
        if (state != ScanProgress.State.COMPLETED) return new Update(state, Optional.empty());
        // The final observation supplies current entity state/NBT. No captured start values are rewarded.
        var result = data.update(player.getUUID(), player.getRandom()::nextInt,
                next -> target.completeWithClue(next, TCAspects.catalog(), ScanMode.THAUMOMETER, TCConfig.SERVER.aspectPoolSoftCap.get(),
                        ResearchCatalogs.rules(), player.level().random::nextInt));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new thaumcraft.api.research.ScanEvent(player,target,result.scan()));
        return new Update(state, Optional.of(result.scan()), result.clue());
    }
    private boolean available() { return player.isAlive() && !player.isRemoved() && !player.isSpectator(); }
    private String dimension() { return player.level().dimension().location().toString(); }
    private void requireThread() {
        if (player.getServer() == null || !player.getServer().isSameThread()) throw new IllegalStateException("Scan sessions must run on the owning server thread");
    }
}

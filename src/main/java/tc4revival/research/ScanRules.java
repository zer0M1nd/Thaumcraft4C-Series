package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;

/** Server-side rules after the server has resolved the real target, identity and aspect data. */
public  class ScanRules {
    public enum Status { SUCCESS, ALREADY_SCANNED, NO_ASPECTS, MISSING_PARENT }
    public record Reward(Aspect aspect, int gained, short pool, boolean discovery) {}
    public record Result(Status status, Optional<Aspect> missingParent, List<Reward> rewards) {
        public Result { rewards = List.copyOf(rewards); }
        public boolean succeeded() { return status == Status.SUCCESS; }
    }

    private ScanRules() {}

    public static Result apply(PlayerKnowledge player, AspectCatalog catalog, ScanKind kind, String identity,
                               ScanMode mode, AspectAmounts target, int softCap) {
        var extension=thaumcraft.api.research.ScanProgressRules.apply(player,new thaumcraft.api.research.ScanProgressRules.Context(catalog,kind,identity,mode,target,softCap));
        if(extension.isPresent())return extension.get();
        // Unlike the old packet path, this guard is authoritative and occurs before any reward.
        if (!player.canScan(kind, identity, mode)) return rejected(Status.ALREADY_SCANNED);
        if (target == null || target.isEmpty()) {
            player.rememberScan(kind,mode.key(identity));
            return rejected(Status.NO_ASPECTS);
        }
        for (Aspect aspect : target.aspects()) {
            for (String parentTag : aspect.components()) {
                var parent = catalog.require(parentTag);
                if (!player.knows(parent)) return new Result(Status.MISSING_PARENT, Optional.of(parent), List.of());
            }
        }
        boolean upgrade = mode == ScanMode.ENHANCED && player.scanned(kind, identity);
        var rewards = new ArrayList<Reward>();
        for (var entry : target.view().entrySet()) {
            int amount = upgrade ? 0 : entry.getValue();
            if (mode == ScanMode.ENHANCED && amount<Integer.MAX_VALUE) amount++;
            rewards.add(grant(player, entry.getKey(), amount, softCap));
        }
        player.rememberScan(kind, mode.key(identity));
        return new Result(Status.SUCCESS, Optional.empty(), rewards);
    }

    public static Reward grant(PlayerKnowledge player, Aspect aspect, int amount, int softCap) {
        boolean discovery = !player.knows(aspect);
        amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) amount) + (discovery ? 2 : 0));
        int before = player.poolAmount(aspect);
        if (before >= softCap) amount = (int) Math.sqrt(amount);
        if (amount > 1 && (float) before >= (float) softCap * 1.25f) amount = 1;
        player.discover(aspect);
        player.changePool(aspect, amount);
        int recorded = player.poolAmount(aspect) - before;
        return new Reward(aspect, recorded, player.pool(aspect), discovery);
    }

    private static Result rejected(Status reason) { return new Result(reason, Optional.empty(), List.of()); }
}

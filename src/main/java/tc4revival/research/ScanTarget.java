package tc4revival.research;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;

/** A server-resolved observation. Continuity is distinct from the saved legacy scan hash. */
public record ScanTarget(Continuity continuity, ScanKind kind, Optional<String> identity, AspectAmounts aspects,
                         Optional<ResearchRules.Clue> clue) {
    public record Completion(ScanRules.Result scan, Optional<String> clue) {}
    public ScanTarget(Continuity continuity, ScanKind kind, Optional<String> identity, AspectAmounts aspects) {
        this(continuity, kind, identity, aspects, Optional.empty());
    }
    public sealed interface Continuity permits Block, Entity, Phenomenon {}
    public record Block(String item, int metadata) implements Continuity {
        public Block { Objects.requireNonNull(item); }
    }
    public record Entity(UUID uuid) implements Continuity {
        public Entity { Objects.requireNonNull(uuid); }
    }
    public record Phenomenon(String id) implements Continuity {
        public Phenomenon { Objects.requireNonNull(id); }
    }

    public ScanTarget {
        Objects.requireNonNull(continuity);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(identity);
        Objects.requireNonNull(clue);
        aspects = aspects.copy();
        if (identity.isEmpty() && !aspects.isEmpty()) throw new IllegalArgumentException("Rewarding targets require an identity");
    }
    @Override public AspectAmounts aspects() { return aspects.copy(); }

    // Empty aspects and missing parents do not interrupt aiming: old TC4 reports them on completion.
    public boolean canTrack(PlayerKnowledge knowledge, ScanMode mode) {
        return identity.map(id -> knowledge.canScan(kind, id, mode)).orElse(true)||thaumcraft.api.research.ScanProgressRules.canTrack(this,knowledge,mode);
    }
    /** A known observation can reveal a newly installed research's explicit trigger without paying points again. */
    public boolean canTrack(PlayerKnowledge knowledge, ScanMode mode, ResearchRules research) {
        return canTrack(knowledge, mode) || research != null && clue.isPresent()
                && !research.clueCandidates(knowledge, clue.get(), java.util.Map.of()).isEmpty();
    }
    public ScanRules.Result complete(PlayerKnowledge knowledge, AspectCatalog catalog, ScanMode mode, int softCap) {
        return identity.map(id -> ScanRules.apply(knowledge, catalog, kind, id, mode, aspects, softCap))
                .orElseGet(() -> new ScanRules.Result(ScanRules.Status.NO_ASPECTS, Optional.empty(), List.of()));
    }

    /** Called inside the same knowledge transaction as the scan itself. */
    public Completion completeWithClue(PlayerKnowledge knowledge, AspectCatalog catalog, ScanMode mode, int softCap,
                                      ResearchRules research, java.util.function.IntUnaryOperator random) {
        var result = complete(knowledge, catalog, mode, softCap);
        if (result.status() == ScanRules.Status.ALREADY_SCANNED && clue.isPresent()) {
            var discovered = research.createClue(knowledge, clue.get(), java.util.Map.of(), random);
            // Success here is clue-only: no pool changes, discoveries, or repeat reward events.
            if (discovered.isPresent()) return new Completion(
                    new ScanRules.Result(ScanRules.Status.SUCCESS, Optional.empty(), List.of()), discovered);
        }
        if (!result.succeeded() || clue.isEmpty()) return new Completion(result, Optional.empty());
        var rewards = new java.util.LinkedHashMap<String, Integer>();
        result.rewards().stream().filter(reward -> reward.gained() > 0)
                .forEach(reward -> rewards.put(reward.aspect().tag(), reward.gained()));
        return new Completion(result, research.createClue(knowledge, clue.get(), rewards, random));
    }
}

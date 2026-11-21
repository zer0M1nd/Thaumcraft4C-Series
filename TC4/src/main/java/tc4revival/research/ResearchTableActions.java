package tc4revival.research;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;

/** Transaction proposals for the real table adapter: rejected actions never alter their inputs. */
public  class ResearchTableActions {
    @FunctionalInterface public interface RandomFloat { float next(); }
    public enum Rejection { NO_NOTE, COMPLETE_NOTE, NO_INK, INVALID_CELL, UNKNOWN_ASPECT, NO_POINTS,
        NOT_COMPLETE, NO_DUPLICATION_RESEARCH, NO_MATERIALS, COPY_LIMIT, UNKNOWN_RESEARCH }
    public record State(Optional<ResearchNote> note, int noteCount, int ink, Set<String> bonuses) {
        public State {
            Objects.requireNonNull(note);
            if (ink < 0 || noteCount < 0 || note.isPresent() != (noteCount > 0)) {
                throw new IllegalArgumentException("Invalid table contents");
            }
            bonuses = java.util.Collections.unmodifiableSet(new LinkedHashSet<>(bonuses));
        }
    }
    /** paper/inkSac are inventory costs. Orb pitch also records the extra legacy random draw. */
    public record Change(State table, PlayerKnowledge knowledge, int paper, int inkSac,
            Optional<ScanRules.Reward> reward, Optional<Float> orbPitch, boolean completedNow) {}
    public record Result(Optional<Change> change, Optional<Rejection> rejection) {
        public boolean accepted() { return change.isPresent(); }
    }
    private final AspectCatalog aspects;
    private final ResearchCatalog catalog;
    private final Predicate<ResearchDefinition> enabled;
    private final ResearchRules research;
    private final ResearchNotes notes;

    public ResearchTableActions(AspectCatalog aspects, ResearchCatalog catalog, Predicate<ResearchDefinition> enabled) {
        this.aspects = aspects;
        this.catalog = catalog;
        this.enabled = enabled;
        this.research = new ResearchRules(catalog, enabled);
        this.notes = new ResearchNotes(aspects);
    }

    /** Null aspect erases a placed cell; anchors and nonexistent cells can never be overwritten. */
    public Result place(State table, PlayerKnowledge player, ResearchHex hex, String tag, RandomFloat random) {
        if (table.note().isEmpty()) return reject(Rejection.NO_NOTE);
        var note = table.note().orElseThrow();
        if (note.complete()) return reject(Rejection.COMPLETE_NOTE);
        if (table.ink() == 0) return reject(Rejection.NO_INK);
        if (catalog.find(note.key()).filter(enabled).isEmpty()) return reject(Rejection.UNKNOWN_RESEARCH);
        var cell = note.grid().get(hex);
        boolean erase = tag == null;
        if (cell == null || (erase ? cell.type() != 2 : cell.type() != 0)) return reject(Rejection.INVALID_CELL);
        Aspect aspect = aspects.find(erase ? cell.aspect() : tag).orElse(null);
        if (aspect == null || (!erase && !player.knows(aspect))) return reject(Rejection.UNKNOWN_ASPECT);
        // Check actual resources before rolling the research discount. This also prevents request spam for free ink.
        if (!erase && !available(table, player, aspect)) return reject(Rejection.NO_POINTS);
        boolean first = research.completed(player, "RESEARCHER1");
        boolean second = research.completed(player, "RESEARCHER2");
        var nextPlayer = player.copy();
        var bonuses = new LinkedHashSet<>(table.bonuses());
        Optional<Float> orb = Optional.empty();
        if (erase) {
            float roll = roll(random); // TC4 draws even without either research upgrade.
            if ((first && roll < 0.25f) || (second && roll < 0.5f)) {
                orb = Optional.of(0.9f + roll(random) * 0.2f);
                nextPlayer.changePool(aspect, (short) 1);
            }
        } else if (second && roll(random) < 0.1f) {
            orb = Optional.of(0.9f + roll(random) * 0.2f);
        } else {
            consume(nextPlayer, bonuses, aspect);
        }
        var grid = new LinkedHashMap<>(note.grid());
        grid.put(hex, erase ? ResearchNote.Cell.blank() : new ResearchNote.Cell(2, tag));
        var changed = new ResearchNote(note.key(), note.color(), false, note.copies(), grid, note.layoutSize());
        var solved = notes.complete(changed, known -> nextPlayer.knows(aspects.require(known)));
        var nextTable = new State(Optional.of(solved.orElse(changed)), table.noteCount(), table.ink() - 1, bonuses);
        return accept(new Change(nextTable, nextPlayer, 0, 0, Optional.empty(), orb, solved.isPresent()));
    }

    /** Combining works without a note or ink, and unsuccessful combinations still spend both inputs. */
    public Result combine(State table, PlayerKnowledge player, String first, String second, int softCap) {
        var a = aspects.find(first).orElse(null);
        var b = aspects.find(second).orElse(null);
        if (a == null || b == null || !player.knows(a) || !player.knows(b)) return reject(Rejection.UNKNOWN_ASPECT);
        if (!available(table, player, a) || !available(table, player, b)) return reject(Rejection.NO_POINTS);
        var nextPlayer = player.copy();
        var bonuses = new LinkedHashSet<>(table.bonuses());
        consume(nextPlayer, bonuses, a);
        if (nextPlayer.poolAmount(b) == 0 && !bonuses.contains(b.tag())) return reject(Rejection.NO_POINTS);
        consume(nextPlayer, bonuses, b);
        var reward = aspects.combine(a, b).map(output -> ScanRules.grant(nextPlayer, output, 1, softCap));
        return accept(new Change(new State(table.note(), table.noteCount(), table.ink(), bonuses), nextPlayer,
                0, 0, reward, Optional.empty(), false));
    }

    public Result synthesize(State table, PlayerKnowledge player, String tag, int softCap) {
        var output = aspects.find(tag).orElse(null);
        if (output == null || output.isPrimal() || !player.knows(output) || !research.completed(player, "RESEARCHER2")) return reject(Rejection.UNKNOWN_ASPECT);
        var plan = AspectSynthesis.plan(output, aspects, player::poolAmount, table.bonuses());
        if (!plan.affordable()) return reject(Rejection.NO_POINTS);
        var next = player.copy();
        var bonuses = new LinkedHashSet<>(table.bonuses());
        for (var cost : plan.costs().entrySet()) {
            int pool = Math.min(cost.getValue(), next.poolAmount(cost.getKey()));
            if (pool > 0) next.changePool(cost.getKey(), -pool);
            if (pool < cost.getValue() && !bonuses.remove(cost.getKey().tag())) throw new IllegalStateException("Unreserved synthesis bonus");
        }
        var reward = ScanRules.grant(next, output, 1, softCap);
        return accept(new Change(new State(table.note(), table.noteCount(), table.ink(), bonuses), next, 0, 0, Optional.of(reward), Optional.empty(), false));
    }

    /** A completed stack is enlarged in place, retaining TC4's shared incremented copies field. */
    public Result duplicate(State table, PlayerKnowledge player, int paperAvailable, int inkSacsAvailable) {
        if (table.note().isEmpty()) return reject(Rejection.NO_NOTE);
        var note = table.note().orElseThrow();
        if (!note.complete()) return reject(Rejection.NOT_COMPLETE);
        if (!research.completed(player, "RESEARCHDUPE")) return reject(Rejection.NO_DUPLICATION_RESEARCH);
        if (paperAvailable < 1 || inkSacsAvailable < 1) return reject(Rejection.NO_MATERIALS);
        var definition = catalog.find(note.key()).filter(enabled);
        if (definition.isEmpty()) return reject(Rejection.UNKNOWN_RESEARCH);
        if (table.noteCount() >= 64 || note.copies() == Integer.MAX_VALUE) return reject(Rejection.COPY_LIMIT);
        var costs = new LinkedHashMap<Aspect, Integer>();
        for (var entry : definition.orElseThrow().aspects().entrySet()) {
            long cost = (long) entry.getValue() + note.copies();
            if (cost < 0 || cost > Integer.MAX_VALUE) return reject(Rejection.COPY_LIMIT);
            var aspect = aspects.require(entry.getKey());
            if (player.poolAmount(aspect) < cost) return reject(Rejection.NO_POINTS);
            costs.put(aspect, (int) cost);
        }
        var nextPlayer = player.copy();
        costs.forEach((aspect, cost) -> nextPlayer.changePool(aspect, -cost.intValue()));
        var copied = new ResearchNote(note.key(), note.color(), true, note.copies() + 1, note.grid(), note.layoutSize());
        var nextTable = new State(Optional.of(copied), table.noteCount() + 1, table.ink(), table.bonuses());
        return accept(new Change(nextTable, nextPlayer, 1, 1, Optional.empty(), Optional.empty(), false));
    }

    private static boolean available(State table, PlayerKnowledge player, Aspect aspect) {
        return player.poolAmount(aspect) > 0 || table.bonuses().contains(aspect.tag());
    }
    private static void consume(PlayerKnowledge player, Set<String> bonuses, Aspect aspect) {
        if (player.poolAmount(aspect) > 0) player.changePool(aspect, -1);
        else bonuses.remove(aspect.tag());
    }
    private static float roll(RandomFloat random) {
        float result = random.next();
        if (!(result >= 0 && result < 1)) throw new IllegalArgumentException("Invalid float roll");
        return result;
    }
    private static Result reject(Rejection reason) { return new Result(Optional.empty(), Optional.of(reason)); }
    private static Result accept(Change change) { return new Result(Optional.of(change), Optional.empty()); }
}

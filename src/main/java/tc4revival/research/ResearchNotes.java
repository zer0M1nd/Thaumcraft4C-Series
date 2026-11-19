package tc4revival.research;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import tc4revival.aspect.AspectCatalog;

/** Note generation and graph rules, independent of inventory, rendering and packets. */
public  class ResearchNotes {
    private final AspectCatalog aspects;
    public ResearchNotes(AspectCatalog aspects) { this.aspects = aspects; }

    public Optional<ResearchNote> generate(ResearchDefinition research, IntUnaryOperator random) {
        return generate(research,random,false);
    }

    public Optional<ResearchNote> generate(ResearchDefinition research, IntUnaryOperator random, boolean randomize) {
        if(research.mirror() || research.tutorial())return Optional.empty();
        if (research.noteLayout().isPresent()) return Optional.of(generateLayout(research));
        research = ResearchNoteTestSeeds.apply(research, aspects, random);
        String primary = null;
        int highest = 0;
        for (var amount : research.aspects().entrySet()) {
            if (amount.getValue() > highest) {
                highest = amount.getValue();
                primary = amount.getKey();
            }
        }
        if (primary == null && !research.noteSeeds().inner().isEmpty()) primary = research.noteSeeds().inner().get(0);
        if (primary == null) return Optional.empty();
        var outer = new java.util.ArrayList<>(research.aspects().keySet());
        // Count all positive anchors, including authored inner cells. Negatives are obstacles,
        // not a second endpoint. Put the extra copy on the rim without displacing inner seeds.
        if (outer.size() + research.noteSeeds().inner().size() == 1)
            outer.add(outer.isEmpty() ? research.noteSeeds().inner().get(0) : outer.get(0));
        int radius = ResearchNoteTestSeeds.radius(research);
        var ring = ResearchHex.ring(radius);
        if (outer.size() > ring.size()) throw new IllegalArgumentException("Too many research anchors");
        // Stable insertion order makes layout independent of JVM hash iteration.
        var positions = new LinkedHashMap<String, ResearchHex>();
        var center = new ResearchHex(0, 0);
        positions.put(center.legacyKey(), center);
        for (int distance = 1; distance <= radius; distance++) {
            for (var hex : ResearchHex.ring(distance)) positions.put(hex.legacyKey(), hex);
        }
        int start = randomize ? random.applyAsInt(ring.size()) : 0;
        var cells = new HashMap<ResearchHex, ResearchNote.Cell>();
        for (var hex : positions.values()) {
            cells.put(hex, ResearchNote.Cell.blank());
        }
        if (research.noteSeeds().empty()) {
        float spacing = (float) ring.size() / outer.size();
        float position = start;
        if (randomize) shuffle(outer, random);
        for (String aspect : outer) {
            aspects.require(aspect);
            var hex = ring.get(Math.round(position) % ring.size());
            cells.put(hex, new ResearchNote.Cell(1, aspect));
            position += spacing;
        }
        } else placeExtendedSeeds(research, outer, radius, cells, random, randomize, start);
        var grid = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        positions.values().forEach(hex -> grid.put(hex, cells.get(hex)));
        ResearchNoteHoles.carve(grid, research.complexity() > 1 ? research.complexity() * 2 : 0, random);
        return Optional.of(new ResearchNote(research.key(), aspects.require(primary).color(), false, 0, grid));
    }

    private ResearchNote generateLayout(ResearchDefinition research) {
        var layout = research.noteLayout().orElseThrow();
        var grid = layout.initialGrid();
        grid.values().stream().filter(ResearchNote.Cell::fixed).forEach(cell -> aspects.require(cell.aspect()));
        String primary = grid.values().stream().filter(ResearchNote.Cell::anchor).map(ResearchNote.Cell::aspect)
                .findFirst().orElseGet(() -> grid.values().stream().filter(ResearchNote.Cell::negative)
                        .map(ResearchNote.Cell::aspect).findFirst().orElse(null));
        return new ResearchNote(research.key(), primary == null ? 0xffffff : aspects.require(primary).color(),
                false, 0, grid, layout.size());
    }

    private record Seed(ResearchNote.Cell cell, boolean inner) {}
    private static <T> void shuffle(java.util.List<T> values, IntUnaryOperator random) {
        for (int i=values.size()-1;i>0;i--) java.util.Collections.swap(values,i,random.applyAsInt(i+1));
    }

    /** Keep placement constraints with each seed while shuffling all types together. */
    private void placeExtendedSeeds(ResearchDefinition research, java.util.List<String> outer, int radius,
            HashMap<ResearchHex, ResearchNote.Cell> cells, IntUnaryOperator random, boolean randomize, int start) {
        var seeds = new java.util.ArrayList<Seed>();
        outer.forEach(tag -> seeds.add(new Seed(new ResearchNote.Cell(1, tag), false)));
        research.noteSeeds().inner().forEach(tag -> seeds.add(new Seed(new ResearchNote.Cell(1, tag), true)));
        research.noteSeeds().negative().forEach(tag -> seeds.add(new Seed(new ResearchNote.Cell(3, tag), true)));
        int outerCount = outer.size();
        int innerCapacity = 1 + 3 * radius * (radius - 1);
        if (seeds.size() - outerCount > innerCapacity)
            throw new IllegalArgumentException("Too many inner research seeds for radius " + radius);
        var ring = ResearchHex.ring(radius);
        if (randomize) shuffle(seeds, random);
        var innerIndices = new java.util.ArrayList<Integer>();
        var usedInner = new java.util.ArrayList<ResearchHex>();
        for (int i = 0; i < seeds.size(); i++) {
            var seed = seeds.get(i).cell();
            aspects.require(seed.aspect());
            boolean inner = seeds.get(i).inner();
            if (inner) innerIndices.add(i);
            var target = ring.get((start + Math.round(i * (float) ring.size() / seeds.size())) % ring.size());
            // Prefer nonadjacent inner cells, then the cell nearest its allocated angular sector.
            // Stable coordinate ties make this independent of HashMap iteration and reload order.
            var selected = cells.keySet().stream()
                    .filter(hex -> !cells.get(hex).occupied() && (inner ? hex.radius() < radius : hex.radius() == radius))
                    .min(java.util.Comparator.<ResearchHex>comparingInt(hex -> inner
                            ? (int) usedInner.stream().filter(other -> hex.distance(other) == 1).count() : 0)
                            .thenComparingInt(hex -> hex.distance(target))
                            .thenComparingInt(ResearchHex::q).thenComparingInt(ResearchHex::r))
                    .orElseThrow(() -> new IllegalArgumentException("No position for research seed"));
            cells.put(selected, seed);
            if (inner) usedInner.add(selected);
        }
        // A locally nearest greedy choice can unnecessarily force later seeds next
        // to each other. The three independent colour classes of this small axial
        // hexagon provide a complete nonadjacent layout whenever one can fit.
        if(usedInner.stream().anyMatch(a->usedInner.stream().anyMatch(b->a.distance(b)==1))) {
            java.util.List<ResearchHex> best=null;int bestDistance=Integer.MAX_VALUE;
            for(int colour=0;colour<3;colour++) {
                int selectedColour=colour;
                var available=new java.util.ArrayList<>(cells.keySet().stream().filter(h->h.radius()<radius
                        && Math.floorMod(h.q()-h.r(),3)==selectedColour).toList());
                if(available.size()<usedInner.size())continue;
                var planned=new java.util.ArrayList<ResearchHex>();int distance=0;
                for(int i:innerIndices) {
                    var target=ring.get((start+Math.round(i*(float)ring.size()/seeds.size()))%ring.size());
                    available.sort(java.util.Comparator.comparingInt((ResearchHex h)->h.distance(target))
                            .thenComparingInt(ResearchHex::q).thenComparingInt(ResearchHex::r));
                    var hex=available.remove(0);planned.add(hex);distance+=hex.distance(target);
                }
                if(distance<bestDistance){best=planned;bestDistance=distance;}
            }
            if(best!=null) {
                usedInner.forEach(h->cells.put(h,ResearchNote.Cell.blank()));
                for(int i=0;i<best.size();i++)cells.put(best.get(i),seeds.get(innerIndices.get(i)).cell());
            }
        }
    }

    /** Repair the old zero-complexity seven-cell board without losing any placed or fixed aspect. */
    public static ResearchNote expandUndersized(ResearchNote note) {
        if(note.layoutSize()>0 || note.complete() || note.grid().size()!=7 || note.grid().keySet().stream().anyMatch(hex->hex.radius()>1))return note;
        var expanded=new LinkedHashMap<>(note.grid());
        for(var hex:ResearchHex.ring(2))expanded.put(hex,ResearchNote.Cell.blank());
        return new ResearchNote(note.key(),note.color(),false,note.copies(),expanded);
    }

    /** Failure leaves the note unchanged. Only direct aspect components create links. */
    public Optional<ResearchNote> complete(ResearchNote note, Predicate<String> discovered) {
        var anchors = note.grid().entrySet().stream().filter(entry -> entry.getValue().anchor())
                .map(java.util.Map.Entry::getKey).toList();
        if (anchors.isEmpty()) return Optional.empty();
        var reached = new HashSet<ResearchHex>();
        var pending = new ArrayDeque<ResearchHex>();
        if (!anchors.isEmpty()) { pending.add(anchors.get(0)); reached.add(anchors.get(0)); }
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            for (int direction = 0; direction < 6; direction++) {
                var next = current.neighbor(direction);
                var cell = note.grid().get(next);
                if (cell == null || !cell.occupied() || reached.contains(next)) continue;
                String first = note.grid().get(current).aspect();
                String second = cell.aspect();
                if (!discovered.test(first) || !discovered.test(second)) continue;
                var a = aspects.find(first);
                var b = aspects.find(second);
                if (a.isPresent() && b.isPresent() && aspects.areRelated(a.get(), b.get())) {
                    if (cell.negative()) return Optional.empty();
                    reached.add(next);
                    pending.addLast(next);
                }
            }
        }
        if (!reached.containsAll(anchors)) return Optional.empty();
        var connected = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        note.grid().forEach((hex, cell) -> { if (cell.fixed() || reached.contains(hex)) connected.put(hex, cell); });
        // A single anchor is a valid trivial puzzle; an empty note is never complete.
        return Optional.of(new ResearchNote(note.key(), note.color(), true, note.copies(), connected, note.layoutSize()));
    }
}

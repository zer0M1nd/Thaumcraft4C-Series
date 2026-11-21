package tc4revival.research;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;

/** One player's knowledge. World ownership and persistence are handled separately by UUID. */
public  class PlayerKnowledge {
    public enum ScanKind { OBJECT, ENTITY, PHENOMENON }
    public enum ScanMode {
        THAUMOMETER("@"), ENHANCED("#");
        private final String prefix;
        ScanMode(String prefix) { this.prefix = prefix; }
        public String key(String identity) { return prefix + identity; }
    }
    public enum WarpKind { PERMANENT, TEMPORARY, STICKY }

    private final AspectCatalog catalog;
    private final LinkedHashMap<Aspect, Integer> known = new LinkedHashMap<>();
    private final LinkedHashSet<String> research = new LinkedHashSet<>();
    private final EnumMap<ScanKind, LinkedHashSet<String>> scans = new EnumMap<>(ScanKind.class);
    private final EnumMap<WarpKind, Integer> warp = new EnumMap<>(WarpKind.class);
    private final LinkedHashMap<String,String> guidance = new LinkedHashMap<>();
    public Map<String,String> guidance() { return Collections.unmodifiableMap(guidance); }
    public void rememberGuidance(String target, String condition) { guidance.put(Objects.requireNonNull(target), Objects.requireNonNull(condition)); }
    private int warpCounter;
    private ResearchPreferences researchPreferences=ResearchPreferences.DEFAULT;
    public ResearchPreferences researchPreferences() { return researchPreferences; }
    public void researchPreferences(ResearchPreferences value) { researchPreferences=java.util.Objects.requireNonNull(value); }

    public PlayerKnowledge(AspectCatalog catalog) {
        this.catalog = catalog;
        for (ScanKind kind : ScanKind.values()) scans.put(kind, new LinkedHashSet<>());
        for (WarpKind kind : WarpKind.values()) warp.put(kind, 0);
        // PlayerKnowledge's lazy initialization order differs from Aspect's declaration order.
        for (String tag : List.of("aer", "ignis", "terra", "aqua", "ordo", "perditio")) {
            known.put(catalog.require(tag), 0);
        }
    }

    public static PlayerKnowledge firstLogin(AspectCatalog catalog, IntUnaryOperator randomBelowBound) {
        var knowledge = new PlayerKnowledge(catalog);
        for (Aspect primal : thaumcraft.api.aspects.AspectSelection.select(catalog,thaumcraft.api.aspects.AspectSelection.INITIAL_KNOWLEDGE)) {
            int roll = randomBelowBound.applyAsInt(5);
            if (roll < 0 || roll >= 5) throw new IllegalArgumentException("Invalid random result");
            knowledge.setPool(primal, (short) (15 + roll));
        }
        return knowledge;
    }

    public PlayerKnowledge copy() {
        var result = new PlayerKnowledge(catalog);
        result.known.clear();
        result.known.putAll(known);
        result.research.addAll(research);
        result.guidance.putAll(guidance);
        scans.forEach((kind, entries) -> result.scans.get(kind).addAll(entries));
        result.warp.putAll(warp);
        result.warpCounter = warpCounter;
        result.researchPreferences=researchPreferences;
        return result;
    }

    /** A detached presentation ledger; all authoritative knowledge and scan markers stay intact. */
    public PlayerKnowledge aspectView(java.util.function.Predicate<Aspect> visible){
        var result=copy();result.known.keySet().removeIf(aspect->!visible.test(aspect));return result;
    }
    public Map<Aspect, Integer> knownAspects() { return Collections.unmodifiableMap(known); }
    public boolean knows(Aspect aspect) { return known.containsKey(aspect); }
    /** Compatibility view for older addons. Use poolAmount for the full balance. */
    public short pool(Aspect aspect) { return (short) Math.min(Short.MAX_VALUE, poolAmount(aspect)); }
    public int poolAmount(Aspect aspect) { return known.getOrDefault(aspect, 0); }
    public boolean knowsParents(Aspect aspect) {
        return aspect != null && aspect.components().stream().allMatch(tag -> knows(catalog.require(tag)));
    }

    public boolean discover(Aspect aspect) { return known.putIfAbsent(Objects.requireNonNull(aspect), 0) == null; }
    public void setPool(Aspect aspect, short amount) { setPool(aspect, (int) amount); }
    public void setPool(Aspect aspect, int amount) { known.put(Objects.requireNonNull(aspect), Math.max(0, amount)); }

    public boolean changePool(Aspect aspect, short delta) { return changePool(aspect, (int) delta); }
    /** Debits are atomic; additions saturate instead of wrapping or crashing. */
    public boolean changePool(Aspect aspect, int delta) {
        if (aspect == null || delta == 0) return false;
        int current = poolAmount(aspect);
        long next = (long) current + delta;
        if (next < 0 || next == current || current == Integer.MAX_VALUE && delta > 0) return false;
        known.put(aspect, (int) Math.min(Integer.MAX_VALUE, next));
        return true;
    }

    public Set<String> researchMarkers() { return Collections.unmodifiableSet(research); }
    public boolean rememberResearch(String key) { return research.add(Objects.requireNonNull(key)); }
    public boolean forgetResearch(String key) { return research.remove(key); }

    public Set<String> scannedKeys(ScanKind kind) { return Collections.unmodifiableSet(scans.get(kind)); }
    /** Forget normal and enhanced scans, without changing research, aspect pools or warp. */
    public int clearScans() {
        int count = scans.values().stream().mapToInt(Set::size).sum();
        scans.values().forEach(Set::clear);
        return count;
    }
    public boolean scanned(ScanKind kind, String identity) {
        return scans.get(kind).contains(ScanMode.THAUMOMETER.key(identity))
                || scans.get(kind).contains(ScanMode.ENHANCED.key(identity));
    }

    public boolean canScan(ScanKind kind, String identity, ScanMode mode) {
        if (scans.get(kind).contains(ScanMode.ENHANCED.key(identity))) return false;
        return mode == ScanMode.ENHANCED || !scans.get(kind).contains(mode.key(identity));
    }

    public void rememberScan(ScanKind kind, String prefixedIdentity) {
        var entries = scans.get(kind);
        if (entries.add(prefixedIdentity) && prefixedIdentity.startsWith("#")) {
            entries.remove("@" + prefixedIdentity.substring(1));
        }
    }

    public int warp(WarpKind kind) { return warp.get(kind); }
    public int totalWarp() { return warp.values().stream().mapToInt(Integer::intValue).sum(); }
    public int warpCounter() { return warpCounter; }
    public void setWarpCounter(int amount) { warpCounter = amount; }
    public void setWarp(WarpKind kind, int amount) { warp.put(kind, Math.max(0, amount)); }
    public void changeWarp(WarpKind kind, int amount) { setWarp(kind, warp(kind) + amount); }

    public void wipe() {
        known.clear();
        for (String tag : List.of("aer", "ignis", "terra", "aqua", "ordo", "perditio")) known.put(catalog.require(tag), 0);
        research.clear();
        guidance.clear();
        scans.values().forEach(Set::clear);
        warp.replaceAll((kind, value) -> 0);
        warpCounter = 0;
    }
}

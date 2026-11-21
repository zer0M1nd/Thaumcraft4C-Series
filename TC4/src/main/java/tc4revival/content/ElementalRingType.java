package tc4revival.content;

import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Legacy primal registration order, which differs from shard metadata order. */
public enum ElementalRingType {
    AIR(3, "air", "aer"), EARTH(4, "earth", "terra"), FIRE(5, "fire", "ignis"),
    WATER(6, "water", "aqua"), ORDER(7, "order", "ordo"), ENTROPY(8, "entropy", "perditio");

    private final int metadata;
    private final String path, aspectTag;
    ElementalRingType(int metadata, String element, String aspectTag) {
        this.metadata = metadata;
        this.path = element + "_ring";
        this.aspectTag = aspectTag;
    }
    public int metadata() { return metadata; }
    public String path() { return path; }
    public String aspectTag() { return aspectTag; }
    public tc4revival.aspect.LegacyAspect definition() { return tc4revival.aspect.LegacyAspect.valueOf(name()); }
    public Aspect aspect() { return TCAspects.get(definition()); }
}

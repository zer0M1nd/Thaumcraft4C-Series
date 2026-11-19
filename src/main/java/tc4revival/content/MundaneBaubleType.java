package tc4revival.content;

/** The uninfused accessories, before elemental rings and other magical upgrades. */
public enum MundaneBaubleType {
    AMULET(0, "mundane_amulet", "bauble_amulet", "necklace", 1),
    RING(1, "mundane_ring", "bauble_ring", "ring", 2),
    BELT(2, "mundane_belt", "bauble_belt", "belt", 1);

    private final int metadata, slots;
    private final String path, texture, slot;
    MundaneBaubleType(int metadata, String path, String texture, String slot, int slots) {
        this.metadata = metadata; this.path = path; this.texture = texture; this.slot = slot; this.slots = slots;
    }
    public int metadata() { return metadata; }
    public String path() { return path; }
    public String texture() { return texture; }
    public String slot() { return slot; }
    public int slots() { return slots; }
}

package tc4revival.content;

/** Ordinals are part of the legacy node NBT format. */
public enum AuraNodeType {
    NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY, PURE;
    public static AuraNodeType fromId(int id) { return id >= 0 && id < values().length ? values()[id] : NORMAL; }
}

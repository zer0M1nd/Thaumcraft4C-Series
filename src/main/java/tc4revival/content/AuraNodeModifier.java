package tc4revival.content;

public enum AuraNodeModifier {
    BRIGHT, PALE, FADING;
    public static AuraNodeModifier fromId(int id) { return id >= 0 && id < values().length ? values()[id] : null; }
}

package dev.tc4.updater;

import java.util.Locale;

public enum UpdateMode {
    YES("是"), NO("否"), ASK("询问");
    private final String label;
    UpdateMode(String label) { this.label = label; }
    public String label() { return label; }
    public String configValue() { return name().toLowerCase(Locale.ROOT); }
    public static UpdateMode parse(Object value) {
        if (!(value instanceof String text)) throw new IllegalArgumentException("Expected 是/否/询问 (yes/no/ask)");
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "是", "yes" -> YES;
            case "否", "no" -> NO;
            case "询问", "ask" -> ASK;
            default -> throw new IllegalArgumentException("Expected 是/否/询问 (yes/no/ask): " + text);
        };
    }
    public static UpdateMode combine(UpdateMode channel, UpdateMode major) {
        if (channel == NO || major == NO) return NO;
        return channel == ASK || major == ASK ? ASK : YES;
    }
}

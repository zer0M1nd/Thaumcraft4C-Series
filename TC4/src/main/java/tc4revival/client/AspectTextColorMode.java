package tc4revival.client;

/** Brightness variants for the name portion of an inline aspect component. */
public enum AspectTextColorMode {
    DEFAULT,
    LIGHT,
    DARK;

    public int color(int baseColor) {
        int red = baseColor >> 16 & 255;
        int green = baseColor >> 8 & 255;
        int blue = baseColor & 255;
        float brightness = (0.2126f * red + 0.7152f * green + 0.0722f * blue) / 255;
        return switch (this) {
            case DEFAULT -> baseColor;
            case LIGHT -> blend(baseColor, 0xFFFFFF, (1 - brightness) / (2 - brightness));
            case DARK -> blend(baseColor, 0x000000, 1.5f * brightness / (1 + 1.5f * brightness));
        };
    }

    private static int blend(int baseColor, int targetColor, float amount) {
        float remaining = 1 - amount;
        int red = Math.round((baseColor >> 16 & 255) * remaining + (targetColor >> 16 & 255) * amount);
        int green = Math.round((baseColor >> 8 & 255) * remaining + (targetColor >> 8 & 255) * amount);
        int blue = Math.round((baseColor & 255) * remaining + (targetColor & 255) * amount);
        return red << 16 | green << 8 | blue;
    }
}

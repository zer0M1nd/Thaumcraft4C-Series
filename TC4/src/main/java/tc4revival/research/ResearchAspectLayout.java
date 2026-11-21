package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Book recipe aspect positions, including the legacy partial-row alignment rule. */
public  class ResearchAspectLayout {
    public record Glyph(String aspect, int amount, int x, int y) {}
    private ResearchAspectLayout() {}

    public static List<Glyph> grid(Map<String, Integer> aspects, int columns, int x, int y) {
        if (columns < 1) throw new IllegalArgumentException("Positive column count required");
        int rows = (aspects.size() - 1) / columns;
        int shift = (columns - aspects.size() % columns) * 10;
        var result = new ArrayList<Glyph>();
        int index = 0;
        for (var entry : aspects.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            boolean shifted = index / columns >= rows && (rows > 1 || aspects.size() < columns);
            result.add(new Glyph(entry.getKey(), entry.getValue(),
                    x + index % columns * 20 + (shifted ? shift : 0), y - rows * 10 + index / columns * 20));
            index++;
        }
        return List.copyOf(result);
    }
}

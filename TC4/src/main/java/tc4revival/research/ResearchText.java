package tc4revival.research;

import java.util.ArrayList;
import java.util.List;

/** Parses the original book markup without interpreting it as HTML or using replacement regexes. */
public  class ResearchText {
    private ResearchText() {}
    /** Translate legacy IMG references with the same mapping used to expose the external assets. */
    public static String remapPictures(String text, java.util.Map<String,String> resources) {
        var out = new StringBuilder(); int cursor = 0;
        while (true) {
            int start = text.indexOf("<IMG>", cursor);
            if (start < 0) { out.append(text, cursor, text.length()); break; }
            int end = text.indexOf("</IMG>", start + 5);
            if (end < 0) { out.append(text, cursor, text.length()); break; }
            out.append(text, cursor, start + 5);
            String body = text.substring(start + 5, end);
            int namespace = body.indexOf(':'), geometry = body.indexOf(':', namespace + 1);
            if (namespace > 0 && geometry > namespace) {
                String path = body.substring(0, geometry);
                out.append(resources.getOrDefault(path, path)).append(body.substring(geometry));
            } else out.append(body);
            out.append("</IMG>"); cursor = end + 6;
        }
        return out.toString();
    }
    public sealed interface Part permits Text, Break, Rule, Picture {}
    public record Text(String value) implements Part {}
    public record Break() implements Part {}
    public record Rule() implements Part {
        public int xOffset(int width) { return width / 2 - 48; }
        public int yOffset() { return 2; }
    }
    /** UVs deliberately use the old 256-unit GUI coordinate space, regardless of PNG dimensions. */
    public record Picture(String texture, int u, int v, int width, int height, float scale) implements Part {
        public Picture {
            // 1.7.10 permitted camel-case asset paths (e.g. ttinkerer netherShard.png).
            texture=texture.trim().toLowerCase(java.util.Locale.ROOT);
            if (!texture.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || texture.contains("..")
                    || width <= 0 || height <= 0 || !Float.isFinite(scale) || scale <= 0)
                throw new IllegalArgumentException("Invalid research picture");
        }
        public float xOffset(int textWidth) { return -3 + textWidth / 2 - (width / 2) * scale; }
        public int nextY(int y, int fontHeight) { return (int) (y + (height * scale - fontHeight)) + fontHeight; }
    }
    public static List<Part> parse(String source) {
        return parse(source,false);
    }
    /** A malformed translation must not terminate the render loop; retain the surrounding prose. */
    public static List<Part> parseForDisplay(String source) { return parse(source,true); }
    private static List<Part> parse(String source,boolean tolerant) {
        var result = new ArrayList<Part>(); var text = new StringBuilder();
        for (int cursor = 0; cursor < source.length();) {
            String marker = null;
            for (String candidate : List.of("<BR>", "<BR/>", "<LINE>", "<LINE/>", "<IMG>")) {
                if (source.startsWith(candidate, cursor)) { marker = candidate; break; }
            }
            if (marker == null && source.charAt(cursor) != '\n') { text.append(source.charAt(cursor++)); continue; }
            flush(result, text);
            if (marker == null) { result.add(new Break()); cursor++; continue; }
            cursor += marker.length();
            if (marker.startsWith("<BR")) result.add(new Break());
            else {
                result.add(new Break());
                if (marker.startsWith("<LINE")) result.add(new Rule());
                else {
                    int end = source.indexOf("</IMG>", cursor);
                    if (end < 0) {
                        if(!tolerant)throw new IllegalArgumentException("Unclosed research image");
                        result.add(new Text("[Invalid image]"));text.append(source.substring(cursor));break;
                    }
                    try { result.add(picture(source.substring(cursor, end))); }
                    catch(IllegalArgumentException error){if(!tolerant)throw error;result.add(new Text("[Invalid image]"));}
                    cursor = end + "</IMG>".length();
                }
                result.add(new Break());
            }
        }
        flush(result, text);
        return List.copyOf(result);
    }
    private static void flush(List<Part> result, StringBuilder text) {
        if (!text.isEmpty()) { result.add(new Text(text.toString())); text.setLength(0); }
    }
    private static Picture picture(String value) {
        var fields = value.split(":", -1);
        if (fields.length != 7) throw new IllegalArgumentException("Invalid research image fields");
        try {
            return new Picture(fields[0] + ":" + fields[1], Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                    Integer.parseInt(fields[4]), Integer.parseInt(fields[5]), Float.parseFloat(fields[6]));
        } catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid research image numbers", failure); }
    }
}

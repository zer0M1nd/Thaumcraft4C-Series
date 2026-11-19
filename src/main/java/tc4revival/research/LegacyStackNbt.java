package tc4revival.research;

import java.util.ArrayDeque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/** Reads the 1.7.10 observer's NBT text while leaving type/value parsing to modern Minecraft. */
public  class LegacyStackNbt {
    private LegacyStackNbt() {}
    public static CompoundTag read(String source) {
        try { return TagParser.parseTag(normalize(source)); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) {
            throw new IllegalArgumentException("Invalid legacy item NBT", failure);
        }
    }
    private static  class Container {
        final char opener;
        boolean start = true;
        Boolean indexed;
        int nextIndex;
        Container(char opener) { this.opener = opener; }
    }
    /** Strip only sequential list-position labels, never numeric compound keys or quoted string contents. */
    static String normalize(String source) {
        java.util.Objects.requireNonNull(source);
        var result = new StringBuilder(source.length());
        var containers = new ArrayDeque<Container>();
        char quote = 0; boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quote != 0) {
                result.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
                continue;
            }
            var container = containers.peek();
            if (container != null && container.opener == '[' && container.start && !Character.isWhitespace(c) && c != ']') {
                int end = i;
                while (end < source.length() && source.charAt(end) >= '0' && source.charAt(end) <= '9') end++;
                int colon = end;
                while (colon < source.length() && Character.isWhitespace(source.charAt(colon))) colon++;
                boolean indexed = end > i && colon < source.length() && source.charAt(colon) == ':';
                if (container.indexed != null && container.indexed != indexed)
                    throw new IllegalArgumentException("Mixed indexed and unindexed legacy NBT list");
                container.indexed = indexed; container.start = false;
                if (indexed) {
                    int index;
                    try { index = Integer.parseInt(source.substring(i, end)); }
                    catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid legacy NBT list position", failure); }
                    if (index != container.nextIndex++) throw new IllegalArgumentException("Nonsequential legacy NBT list position");
                    i = colon;
                    continue;
                }
            }
            if (c == '"' || c == '\'') quote = c;
            else if (c == '{' || c == '[') containers.push(new Container(c));
            else if (c == '}' || c == ']') {
                if (containers.isEmpty() || containers.pop().opener != (c == '}' ? '{' : '['))
                    throw new IllegalArgumentException("Unbalanced legacy NBT container");
            } else if (c == ',' && container != null && container.opener == '[') container.start = true;
            result.append(c);
        }
        return result.toString();
    }
}

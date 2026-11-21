package tc4revival.client;

import java.util.*;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import tc4revival.research.ChineseBookTypography;

/** Preserve native wrapping, but keep a final Chinese punctuation mark with its word. */
final class BookLineWrapping {
    private record Glyph(int code, Style style) {}
    private static final String CLOSING = "，。！？；：、）】”’》〉」』";
    private BookLineWrapping() {}

    static List<FormattedText> split(StringSplitter splitter, FormattedText text, int width) {
        var lines = new ArrayList<FormattedText>();
        splitter.splitLines(text, width, Style.EMPTY, (line, continuation) -> {
            String visible = line.getString().replace("\u200b", "").strip();
            if (continuation && !lines.isEmpty() && !visible.isEmpty()
                    && visible.codePoints().allMatch(c -> CLOSING.indexOf(c) >= 0)) {
                var previous = glyphs(lines.get(lines.size() - 1));
                int last = previous.size() - 1;
                while (last >= 0 && structural(previous.get(last))) last--;
                if (last > 0 && ChineseBookTypography.han(previous.get(last).code())) {
                    var tail = component(previous.subList(last, previous.size())).append(component(glyphs(line)));
                    if (splitter.stringWidth(tail) <= width) {
                        lines.set(lines.size() - 1, component(previous.subList(0, last)));
                        line = tail;
                    }
                }
            }
            lines.add(line);
        });
        return lines;
    }

    private static boolean structural(Glyph glyph) {
        String insertion = glyph.style().getInsertion();
        return Character.isWhitespace(glyph.code())
                || insertion != null && insertion.startsWith(BookFlow.ANCHOR);
    }

    private static List<Glyph> glyphs(FormattedText text) {
        var glyphs = new ArrayList<Glyph>();
        text.visit((style, part) -> {
            StringDecomposer.iterateFormatted(part, style, (offset, s, code) -> {
                glyphs.add(new Glyph(code, s)); return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        return glyphs;
    }

    private static MutableComponent component(List<Glyph> glyphs) {
        var result = Component.empty();
        for (var glyph : glyphs) result.append(Component.literal(new String(Character.toChars(glyph.code()))).withStyle(glyph.style()));
        return result;
    }
}

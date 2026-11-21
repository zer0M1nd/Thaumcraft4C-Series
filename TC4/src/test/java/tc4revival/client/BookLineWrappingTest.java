package tc4revival.client;

import java.util.*;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookLineWrappingTest {
    private final StringSplitter splitter = new StringSplitter((code, style) -> code == 0x200b ? 0 : 6);

    @Test void automaticPunctuationOnlyLineKeepsTheLastChineseGlyphAndItsLink() {
        var click = new ClickEvent(ClickEvent.Action.CHANGE_PAGE, BookComponents.LINK + "research/INFUSION#");
        var style = Style.EMPTY.withUnderlined(true).withClickEvent(click);
        var text = Component.literal("需要").append(Component.literal("注魔").withStyle(style))
                .append(BookFlow.marker("end")).append("。");
        var lines = BookLineWrapping.split(splitter, text, 24);
        assertEquals(List.of("需要注", "魔\u200b。"), lines.stream().map(FormattedText::getString).toList());
        lines.get(1).visit((s, part) -> { if (part.equals("魔")) assertEquals(style, s); return Optional.empty(); }, Style.EMPTY);
        assertTrue(lines.stream().allMatch(line -> splitter.stringWidth(line) <= 24));
    }

    @Test void explicitNewlineAndEnglishWordAreNotRewritten() {
        var explicit = BookLineWrapping.split(splitter, Component.literal("注魔\n。"), 24);
        assertEquals(List.of("注魔", "。"), explicit.stream().map(FormattedText::getString).toList());
        var english = Component.literal("word。");
        assertEquals(splitter.splitLines(english, 24, Style.EMPTY).stream().map(FormattedText::getString).toList(),
                BookLineWrapping.split(splitter, english, 24).stream().map(FormattedText::getString).toList());
    }

    @Test void ordinaryLongChineseLinesRetainNativeWrapping() {
        var text = Component.literal("药水可以延长时效并提高等级");
        assertEquals(splitter.splitLines(text, 30, Style.EMPTY).stream().map(FormattedText::getString).toList(),
                BookLineWrapping.split(splitter, text, 30).stream().map(FormattedText::getString).toList());
    }
}

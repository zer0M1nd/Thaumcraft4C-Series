package tc4revival.client;

import java.util.*;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookTypographyTest {
    @Test void chineseListMarkersStayWithTheirFirstWordWhenWrapped() {
        var link=Component.literal("药水液体").withStyle(s->s.withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,BookComponents.LINK+"research/IB_FLUID_POTION#")));
        var input=Component.literal("· ").append(link).append("：需要注魔，并能继续延长时效与提升药效。\n· 下一项");
        var normalized=BookTypography.normalize(input,"zh_cn");
        var splitter=new net.minecraft.client.StringSplitter((code,style)->6);
        var lines=splitter.splitLines(normalized,78,Style.EMPTY).stream().map(FormattedText::getString).toList();
        assertTrue(lines.get(0).startsWith("·\u00a0药水液体"),lines.toString());
        assertTrue(lines.get(lines.size()-1).startsWith("·\u00a0下一项"),lines.toString());
        assertFalse(lines.stream().anyMatch(s->s.strip().equals("·")),lines.toString());
        assertEquals(input,BookTypography.normalize(input,"en_us"));
        normalized.visit((style,text)->{if(text.contains("药水液体"))assertEquals(link.getStyle(),style);return Optional.empty();},Style.EMPTY);
    }
    @Test void hiddenResearchPlaceholderIsNotChinesePunctuation() {
        var hidden=Component.literal("???").withStyle(s->s.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,BookComponents.LINK+"research/HIDDEN#")));
        assertEquals("未知???研究？",BookTypography.normalize(Component.literal("未知").append(hidden).append("研究?"),"zh_cn").getString());
    }
    @Test void componentStylesAndEventPayloadsSurviveAcrossSiblingBoundaries() {
        var hover=new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal("hover英文,保持.").withStyle(s->s.withColor(0x123456)));
        var click=new ClickEvent(ClickEvent.Action.OPEN_URL,"https://example.org/a,b?x=1");
        var style=Style.EMPTY.withColor(0x998877).withBold(true).withUnderlined(true).withHoverEvent(hover).withClickEvent(click).withInsertion("literal, insertion.");
        var input=Component.literal("中文").append(Component.literal("AE2").withStyle(style)).append("正文.");
        var output=BookTypography.normalize(input,"zh_cn");
        assertEquals("中文 AE2 正文。",output.getString());
        output.visit((s,text)->{if(text.contains("AE2"))assertEquals(style,s);if(text.equals(" ")){assertNull(s.getClickEvent());assertFalse(s.isUnderlined());}return Optional.empty();},Style.EMPTY);
        assertEquals("hover英文,保持.",hover.getValue(HoverEvent.Action.SHOW_TEXT).getString());
        assertEquals("中文AE2正文.",input.getString());
    }
    @Test void anchorInsideAWordDoesNotCreateAFalseBoundaryOrDisappear() {
        var input=Component.literal("中").append(BookFlow.marker("entry/target")).append("AE2文");
        var out=BookTypography.normalize(input,"zh_tw");
        assertEquals("中 AE2 文",out.getString().replace("\u200b",""));
        var anchors=new ArrayList<String>();out.visit((s,t)->{if(s.getInsertion()!=null)anchors.add(s.getInsertion());return Optional.empty();},Style.EMPTY);
        assertEquals(List.of(BookFlow.ANCHOR+"entry/target"),anchors);
    }
    @Test void aspectIconKeepsItsFontAndHasNoSpaceBeforeItsName() {
        var event=new ClickEvent(ClickEvent.Action.CHANGE_PAGE,BookComponents.LINK+"aspect/aer");
        var iconStyle=Style.EMPTY.withInsertion(BookComponents.ICON+"aer").withFont(BookComponents.ICON_FONT).withClickEvent(event);
        var aspect=Component.empty().withStyle(s->s.withClickEvent(event)).append(Component.literal("\uE000").withStyle(iconStyle)).append(Component.literal("Aer").withStyle(s->s.withUnderlined(true)));
        var out=BookTypography.normalize(Component.literal("含").append(aspect).append("要素。"),"zh_cn");
        assertEquals("含 \uE000Aer 要素。",out.getString());
        out.visit((s,t)->{if(t.contains("\uE000"))assertEquals(iconStyle,s);return Optional.empty();},Style.EMPTY);
        assertEquals(out.getString(),BookTypography.normalize(out,"zh_cn").getString());
    }
    @Test void legacyFormattingAndStructuralHeadingGapsRemain() {
        var out=BookTypography.normalize(Component.literal("§n标题§r 正文,按§cshift§r继续."),"zh_cn");
        assertEquals("标题 正文，按 shift 继续。",out.getString());
        assertEquals("属性：正文。",BookTypography.normalize(Component.literal("§n属性:§r  正文."),"zh_cn").getString());
        out.visit((s,t)->{if(t.contains("shift"))assertEquals(0xff5555,s.getColor().getValue());return Optional.empty();},Style.EMPTY);
        var other=Component.literal("中文English, unchanged.");assertSame(other,BookTypography.normalize(other,"en_us"));
    }
}

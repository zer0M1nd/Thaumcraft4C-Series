package tc4revival.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookTextAmendmentsTest {
    private JsonObject rule(String id,String find,String replacement,int count) {
        var r=JsonParser.parseString("""
            {"research":"A","provider":"example","provider_name":"Example","kind":"continuation",
             "keys":["page.a"],"translations":{"en_us":{}}}
            """).getAsJsonObject();r.addProperty("id",id);
        var t=r.getAsJsonObject("translations").getAsJsonObject("en_us");
        t.addProperty("find",find);t.add("replace",new JsonPrimitive(replacement));t.addProperty("count",count);return r;
    }
    private List<Component> apply(List<Component> in,JsonArray rules,List<String> warnings) {
        return BookTextAmendments.apply(in,rules,"page.a","en_us",BookComponents::decode,(id,why)->warnings.add(id+":"+why));
    }
    @Test void editsUseOriginalCoordinatesAndPreserveStylesAndLinks() {
        var click=new ClickEvent(ClickEvent.Action.CHANGE_PAGE,"target");
        var original=Component.literal("Cost ").append(Component.literal("8 XP").withStyle(s->s.withBold(true).withClickEvent(click))).append("; then 9.");
        var rules=new JsonArray();rules.add(rule("one","8 XP","9 XP",1));rules.add(rule("two","9","280",1));
        var warnings=new ArrayList<String>();var out=apply(List.of(original),rules,warnings).get(0);
        assertEquals("Cost 8 XP9 XP; then 9280.",out.getString());assertTrue(warnings.isEmpty());
        out.visit((s,t)->{if(t.equals("8 XP")){assertTrue(s.isStrikethrough());assertTrue(s.isBold());assertEquals(click,s.getClickEvent());}
            if(t.equals("9 XP")){assertFalse(s.isStrikethrough());assertEquals(0x00aa00,s.getColor().getValue());assertNotNull(s.getHoverEvent());}return Optional.empty();},Style.EMPTY);
        assertEquals("Cost 8 XP; then 9.",original.getString());
        assertSame(out,apply(List.of(out),rules,new ArrayList<>()).get(0),"Already amended text is not amended again");
    }
    @Test void hiddenAndHighlightedReplacementsRetainLinksAndExposeTheRequestedText() {
        var click=new ClickEvent(ClickEvent.Action.CHANGE_PAGE,"target");
        var original=Component.literal("Cost ").append(Component.literal("8 XP").withStyle(s->s.withClickEvent(click)));
        var rules=new JsonArray();rules.add(rule("one","8 XP","9 XP",1));
        for (var mode:List.of(tc4revival.config.TCConfig.LegacyTextMode.HIDDEN,tc4revival.config.TCConfig.LegacyTextMode.HIGHLIGHT)) {
            var output=BookTextAmendments.apply(List.of(original),rules,"page.a","en_us",BookComponents::decode,(a,b)->fail(b),mode).get(0);
            assertEquals("Cost 9 XP",output.getString());
            var style=output.visit((s,t)->t.equals("9 XP")?Optional.of(s):Optional.empty(),Style.EMPTY).orElseThrow();
            assertEquals(click,style.getClickEvent());assertFalse(style.isStrikethrough());
            if (mode==tc4revival.config.TCConfig.LegacyTextMode.HIDDEN) {
                assertNull(style.getColor());assertNull(style.getHoverEvent());
            } else {
                assertEquals(0x00aa00,style.getColor().getValue());
                var tooltip=style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT);
                assertTrue(tooltip.getString().contains("\n"));
                assertTrue(Component.Serializer.toJson(tooltip).contains("8 XP"));
            }
        }
    }
    @Test void countsCoverAllLegacyFragmentsAtomicallyAndMismatchKeepsOriginal() {
        var input=List.<Component>of(Component.literal("8"),Component.literal("8"));
        var rules=new JsonArray();rules.add(rule("one","8","280",1));var warnings=new ArrayList<String>();
        var unchanged=apply(input,rules,warnings);assertSame(input.get(0),unchanged.get(0));assertEquals(1,warnings.size());
        rules.set(0,rule("two","8","280",2));assertEquals(List.of("8280","8280"),apply(input,rules,new ArrayList<>()).stream().map(Component::getString).toList());
    }
    @Test void overlappingRulesAreRejectedAsAWholeAndUnicodeIsPreserved() {
        var input=List.<Component>of(Component.literal("🪄 30 levels"));var rules=new JsonArray();
        rules.add(rule("whole","30 levels","280 XP",1));rules.add(rule("part","30","99",1));var warnings=new ArrayList<String>();
        assertEquals("🪄 30 levels280 XP",apply(input,rules,warnings).get(0).getString());assertEquals(1,warnings.size());
    }
    @Test void otherLanguagesAndKeysNeverReceiveEnglishCorrections() {
        var input=List.<Component>of(Component.literal("8"));var rules=new JsonArray();rules.add(rule("one","8","280",1));
        assertSame(input.get(0),BookTextAmendments.apply(input,rules,"page.a","fr_fr",BookComponents::decode,(a,b)->fail()).get(0));
        assertSame(input.get(0),BookTextAmendments.apply(input,rules,"another","en_us",BookComponents::decode,(a,b)->fail()).get(0));
    }
}

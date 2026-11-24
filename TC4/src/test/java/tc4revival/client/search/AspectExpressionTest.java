package tc4revival.client.search;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import static org.junit.jupiter.api.Assertions.*;

class AspectExpressionTest {
    private static Aspect aspect(String name) { return new Aspect(name, 0, List.of(), "", 0); }
    private final Aspect ordo = aspect("ordo"), aer = aspect("aer"), aeris = aspect("aeris");
    private final Map<String, Aspect> aspects = Map.of("ordo", ordo, "aer", aer, "aeris", aeris);
    private final AspectExpression.Values order = values(3, 0);
    private final AspectExpression.Values both = values(3, 2);

    private AspectExpression.Values values(int order, int air) {
        return AspectExpression.Values.of(new AspectAmounts().add(ordo, order).add(aer, air));
    }

    private boolean match(String text, AspectExpression.Values v) {
        return AspectExpression.compile(text, s -> aspects.containsKey(s) ? List.of(aspects.get(s)) : List.of()).test(v);
    }

    @Test void comparisonsAndTotals() {
        for (String expr : List.of("！OrDo", "!ordo>=3", "!3=ordo", "!1<=OrDo<=5", "!5>ordo>2",
                "!ordo<4", "!ordo&types=1", "!sum=3", "!-1<=ordo", "!ordo>-1"))
            assertTrue(match(expr, order), expr);
        assertFalse(match("!ordo&types=1", both));
        assertFalse(match("!3<ordo<=5", order));
        assertTrue(match("!sum=5&types=2", both));
        assertTrue(match("!aer=0&types=0&sum=0", AspectExpression.Values.EMPTY));
    }

    @Test void precedenceAndNegation() {
        assertTrue(match("!aer | ordo & -aeris", order));
        assertFalse(match("!-(aer|ordo)", order));
        assertTrue(match("！ --(aer | (ordo & -aeris))", order));
        assertFalse(match("!(aer|ordo)&types=2", order));
        assertTrue(match("!(aer|ordo)&types=2", both));
    }

    @Test void invalidQueriesHaveNoMatchesEvenUnderNegationOrOr() {
        for (String text : List.of("!", "！  ", "!()", "!(ordo", "!ordo)", "!ordo|", "!&ordo",
                "!-bad", "!ordo|bad", "!ordo==3", "!ordo< =3", "!1<=ordo<=", "!3", "!4<=5",
                "!ordo>=1>=0", "!ordo & -", "!ordo = aer", "!ordo=999999999999999999999",
                "!"+ "(".repeat(100) + "aer" + ")".repeat(100))) {
            assertSame(AspectExpression.INVALID, AspectExpression.compile(text,
                    s -> aspects.containsKey(s) ? List.of(aspects.get(s)) : List.of()), text);
        }
    }

    @Test void aliasesRespectExactPriorityAndIntervalsUseOneAspect() {
        var names = new AspectNames(List.of(aeris, ordo, aer),
                a -> a == aer ? "风，气" : a == aeris ? "风暴" : "秩序, 规则", a -> "", s -> null);
        assertEquals(List.of(aer), names.find("aer"));
        assertEquals(List.of(aeris, aer), names.find("ae"));
        assertEquals(List.of(aer), names.find("风"));
        assertEquals(List.of(ordo), names.find("规则"));
        assertTrue(names.find("eri").isEmpty());
        var alternatives = AspectExpression.compile("!2<=ae<=4", names::find);
        assertFalse(alternatives.test(AspectExpression.Values.of(new AspectAmounts().add(aer, 1).add(aeris, 5))));
    }

    @Test void pinyinSubstringsCanSpanSyllablesAndPolyphonicReadings() {
        var reading = new JecPronunciation(List.of(List.of("zhong", "chong"), List.of("xin")));
        assertTrue(reading.matches("chongxin", true));
        assertTrue(reading.matches("ongxi", false));
        assertTrue(reading.matches("hon", false));
        assertFalse(reading.matches("ongxi", true));
        assertFalse(reading.matches("zhong", true));
        var names = new AspectNames(List.of(aer, ordo), a -> "", a -> a.tag(),
                s -> s.equals("aer") ? reading : new JecPronunciation(List.of(List.of("zhong"))));
        assertEquals(List.of(ordo), names.find("zhong"));
    }
}

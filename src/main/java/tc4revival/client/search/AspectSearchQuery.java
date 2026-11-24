package tc4revival.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import tc4revival.client.search.AspectExpression.Values;

/** Keep the viewer in charge of native search terms, including its prefixes and quoted phrases. */
public record AspectSearchQuery(String nativeText, Predicate<Values> aspects) {
    public static final AspectSearchQuery INVALID = new AspectSearchQuery("", AspectExpression.INVALID);

    public static boolean hasAspectTerm(String text) {
        return nextMarker(text, 0) >= 0;
    }

    private static int nextMarker(String text, int start) {
        if (text.indexOf('!', start) < 0 && text.indexOf('！', start) < 0) return -1;
        boolean quoted = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') quoted = !quoted;
            if (!quoted && (c == '!' || c == '！') && (i == 0 || Character.isWhitespace(text.charAt(i - 1)))) {
                return i;
            }
        }
        return -1;
    }

    static AspectSearchQuery compile(String text, Function<String, Predicate<Values>> compileClause) {
        var nativeText = new StringBuilder();
        Predicate<Values> combined = null;
        int nativeStart = 0;
        int marker = nextMarker(text, 0);
        while (marker >= 0) {
            int next = nextMarker(text, marker + 1);
            int limit = next < 0 ? text.length() : next;
            var ends = possibleEnds(text, marker, limit);
            // Prefer a complete multi-word aspect name or spaced expression over a shorter prefix.
            int end = marker;
            var clause = AspectExpression.INVALID;
            for (int i = ends.size() - 1; i >= 0 && clause == AspectExpression.INVALID; i--) {
                end = ends.get(i);
                clause = compileClause.apply(text.substring(marker, end));
            }
            if (clause == AspectExpression.INVALID) return INVALID;
            nativeText.append(text, nativeStart, marker);
            nativeStart = end;
            combined = combined == null ? clause : combined.and(clause);
            marker = next;
        }
        nativeText.append(text, nativeStart, text.length());
        return new AspectSearchQuery(nativeText.toString().strip(), combined == null ? values -> true : combined);
    }

    private static List<Integer> possibleEnds(String text, int marker, int limit) {
        var ends = new ArrayList<Integer>();
        int depth = 0;
        int i = marker + 1;
        while (i < limit) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                if (c == '(') depth++;
                if (c == ')') depth--;
                i++;
                continue;
            }
            int space = i;
            while (i < limit && Character.isWhitespace(text.charAt(i))) i++;
            if (i == limit) {
                limit = space;
                break;
            }
            if (depth != 0 || "(!！&|<>=".indexOf(text.charAt(space - 1)) >= 0) continue;
            char right = text.charAt(i);
            if ("()|<>=".indexOf(right) >= 0) continue;
            // A separate &word can also be JEI's resource-location filter. A standalone & stays an operator.
            if (right == '&' && (i + 1 == limit || Character.isWhitespace(text.charAt(i + 1))
                    || "-(!！&|<>=".indexOf(text.charAt(i + 1)) >= 0)) continue;
            ends.add(space);
            if ("@#$%*^\"".indexOf(right) >= 0) return ends;
        }
        ends.add(limit);
        return ends;
    }
}

package automagy.logic;

import java.util.List;
import java.util.Locale;

/** Registry-independent rules shared by tally boxes and logistics. */
public final class FilterRules {
    public record Count(long found, int required) {}
    public static boolean tally(List<Count> counts, boolean blacklist, boolean useCount,
                                boolean requireAll, boolean hasName, boolean nameMatches) {
        if (counts.isEmpty() && !hasName) return false;
        boolean all = blacklist ? !requireAll : requireAll;
        boolean qualified = false;
        for (Count count : counts) {
            boolean match = useCount ? count.found >= count.required : count.found > 0;
            if (blacklist) match = !match;
            if (match) {
                if (!all) return true;
                qualified = true;
            } else if (all) return false;
        }
        return hasName ? nameMatches != blacklist : qualified;
    }

    /** Only '*' is special; punctuation is literal, and matching is case-insensitive. */
    public static boolean nameMatches(String pattern, String value) {
        pattern = pattern.trim().toLowerCase(Locale.ROOT);
        value = value.toLowerCase(Locale.ROOT);
        int p = 0, v = 0, star = -1, retry = 0;
        while (v < value.length()) {
            if (p < pattern.length() && pattern.charAt(p) != '*' && pattern.charAt(p) == value.charAt(v)) { p++; v++; }
            else if (p < pattern.length() && pattern.charAt(p) == '*') { star = p++; retry = v; }
            else if (star >= 0) { p = star + 1; v = ++retry; }
            else return false;
        }
        while (p < pattern.length() && pattern.charAt(p) == '*') p++;
        return p == pattern.length();
    }
    private FilterRules() {}
}

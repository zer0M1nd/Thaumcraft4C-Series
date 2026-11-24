package tc4revival.client.search;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

/** Compiled once per query. A failed operand invalidates even an enclosing OR or negation. */
public final class AspectExpression {
    public record Values(Map<Aspect, Integer> amounts, long sum) {
        public static final Values EMPTY = new Values(Map.of(), 0);

        public static Values of(AspectAmounts source) {
            if (source.isEmpty()) return EMPTY;
            var counts = new HashMap<Aspect, Integer>();
            long total = 0;
            for (var entry : source.view().entrySet()) {
                if (entry.getValue() > 0) {
                    counts.put(entry.getKey(), entry.getValue());
                    total += entry.getValue();
                }
            }
            return counts.isEmpty() ? EMPTY : new Values(Map.copyOf(counts), total);
        }
    }

    public static final Predicate<Values> INVALID = values -> false;

    public static boolean prefixed(String input) {
        int index = 0;
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        return index < input.length() && (input.charAt(index) == '!' || input.charAt(index) == '！');
    }

    public static Predicate<Values> compile(String input, Function<String, List<Aspect>> names) {
        if (!prefixed(input)) return INVALID;
        var parser = new AspectExpression(input.strip().substring(1), names);
        try {
            var result = parser.or(0);
            return parser.end() ? result : INVALID;
        } catch (IllegalArgumentException invalid) {
            return INVALID;
        }
    }

    private final String text;
    private final Function<String, List<Aspect>> names;
    private int cursor;

    private AspectExpression(String text, Function<String, List<Aspect>> names) {
        this.text = text;
        this.names = names;
    }

    private Predicate<Values> or(int depth) {
        var result = and(depth);
        while (take('|')) result = result.or(and(depth));
        return result;
    }

    private Predicate<Values> and(int depth) {
        var result = unary(depth);
        while (take('&')) result = result.and(unary(depth));
        return result;
    }

    private Predicate<Values> unary(int depth) {
        if (depth > 64) throw new IllegalArgumentException();
        end();
        if (cursor < text.length() && text.charAt(cursor) == '-'
                && (cursor + 1 >= text.length() || !Character.isDigit(text.charAt(cursor + 1)))) {
            cursor++;
            return unary(depth + 1).negate();
        }
        if (take('(')) {
            var result = or(depth + 1);
            if (!take(')')) throw new IllegalArgumentException();
            return result;
        }
        return comparison();
    }

    private Predicate<Values> comparison() {
        String first = operand();
        Long left = integer(first);
        String leftOp = left == null ? null : operator();
        if (left != null && leftOp == null) throw new IllegalArgumentException();
        String name = left == null ? first : operand();
        String rightOp = operator();
        Long right = rightOp == null ? null : integer(operand());
        if ((rightOp != null && right == null) || integer(name) != null) throw new IllegalArgumentException();
        java.util.function.LongPredicate range = value ->
                (left == null || compare(left, leftOp, value)) && (right == null || compare(value, rightOp, right))
                        && (left != null || right != null || value >= 1);
        if (name.equals("sum")) return v -> range.test(v.sum());
        if (name.equals("types")) return v -> range.test(v.amounts().size());
        var aspects = names.apply(name);
        if (aspects.isEmpty()) throw new IllegalArgumentException();
        return values -> {
            for (int i = 0; i < aspects.size(); i++) {
                if (range.test(values.amounts().getOrDefault(aspects.get(i), 0))) return true;
            }
            return false;
        };
    }

    private static boolean compare(long a, String op, long b) {
        return switch (op) {
            case "=" -> a == b;
            case "<" -> a < b;
            case ">" -> a > b;
            case "<=" -> a <= b;
            case ">=" -> a >= b;
            default -> throw new IllegalArgumentException();
        };
    }

    private String operand() {
        end();
        int start = cursor;
        while (cursor < text.length() && "()|&<>!=！".indexOf(text.charAt(cursor)) < 0) cursor++;
        String value = text.substring(start, cursor).strip().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException();
        return value;
    }

    private static Long integer(String value) {
        int first = value.startsWith("-") || value.startsWith("+") ? 1 : 0;
        if (first == value.length()) return null;
        for (int i = first; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') return null;
        }
        return Long.parseLong(value);
    }

    private String operator() {
        if (end() || "<>=".indexOf(text.charAt(cursor)) < 0) return null;
        int start = cursor++;
        if (text.charAt(start) != '=' && cursor < text.length() && text.charAt(cursor) == '=') cursor++;
        return text.substring(start, cursor);
    }

    private boolean take(char expected) {
        if (end() || text.charAt(cursor) != expected) return false;
        cursor++;
        return true;
    }

    private boolean end() {
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++;
        return cursor == text.length();
    }
}

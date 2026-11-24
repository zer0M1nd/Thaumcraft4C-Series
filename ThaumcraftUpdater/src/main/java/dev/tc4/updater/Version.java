package dev.tc4.updater;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/** Workspace w.x[.y[.z]] versions, compared numerically without overflow. */
public record Version(List<BigInteger> parts) implements Comparable<Version> {
    public static Version parse(String text) {
        if (text == null || !text.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)(?:\\.(?:0|[1-9][0-9]*)(?:\\.[1-9][0-9]*)?)?")) {
            throw new IllegalArgumentException("Invalid release version: " + text);
        }
        var parsed = Arrays.stream(text.split("\\.")).map(BigInteger::new).toList();
        return new Version(parsed.size() == 2 ? List.of(parsed.get(0), parsed.get(1), BigInteger.ZERO) : parsed);
    }

    public boolean stable() { return parts.size() == 3; }

    @Override public int compareTo(Version other) {
        for (int i = 0; i < 4; i++) {
            int compared = (i < parts.size() ? parts.get(i) : BigInteger.ZERO)
                    .compareTo(i < other.parts.size() ? other.parts.get(i) : BigInteger.ZERO);
            if (compared != 0) return compared;
        }
        return 0;
    }
}

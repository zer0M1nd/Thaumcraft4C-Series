package temodern.storage;

import java.util.List;

public record CellTier(String id, long capacity, int types, double idle, boolean creative) {
    public static final List<CellTier> ALL = List.of(
            new CellTier("1k", 8192, 12, .5, false), new CellTier("4k", 32768, 12, 1, false),
            new CellTier("16k", 131072, 12, 1.5, false), new CellTier("64k", 524288, 12, 2, false),
            new CellTier("256k", 2097152, 24, 2.5, false), new CellTier("1024k", 8388608, 36, 3, false),
            new CellTier("4096k", 33554432, 48, 3.5, false), new CellTier("16384k", 134217728, 60, 4, false),
            new CellTier("quantum", (Integer.MAX_VALUE / 16L) * 8, 1, 8, false),
            new CellTier("singularity", Long.MAX_VALUE / 4, 1, 16, false),
            new CellTier("creative", Long.MAX_VALUE / 4, 63, 0, true));
}

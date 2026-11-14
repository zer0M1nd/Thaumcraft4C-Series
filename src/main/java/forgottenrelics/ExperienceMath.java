package forgottenrelics;

/** Point totals use the modern vanilla level curve, never Player.totalExperience (lifetime XP). */
public final class ExperienceMath {
    private ExperienceMath() {}

    public static long atLevel(int level) {
        long n = Math.max(0, level);
        if (n <= 16) return n * n + 6 * n;
        if (n <= 31) return (5 * n * n - 81 * n + 720) / 2;
        // Saturating arithmetic for malformed or administrator-supplied extreme levels.
        if (n > 1_000_000_000L) return Long.MAX_VALUE;
        return (9 * n * n - 325 * n + 4440) / 2;
    }

    public static int level(long points) {
        long xp = Math.max(0, points);
        int lo = 0, hi = 1_000_000_000;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            if (atLevel(mid) <= xp) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    public static int toNext(int level) {
        return level >= 30 ? 9 * level - 158 : level >= 15 ? 5 * level - 38 : 2 * level + 7;
    }
}

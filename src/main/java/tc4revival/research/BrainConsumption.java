package tc4revival.research;

import net.minecraft.util.RandomSource;

/** Brain-specific warp roll; the original food path does not consult wuss mode. */
public  class BrainConsumption {
    private BrainConsumption() {}
    public record Gain(PlayerKnowledge.WarpKind kind, int amount) {
        public Gain {
            if (!(kind == PlayerKnowledge.WarpKind.STICKY && amount == 1
                    || kind == PlayerKnowledge.WarpKind.TEMPORARY && amount >= 1 && amount <= 3))
                throw new IllegalArgumentException("Invalid brain warp gain");
        }
    }
    public static Gain roll(RandomSource random) {
        return random.nextFloat() < 0.1f ? new Gain(PlayerKnowledge.WarpKind.STICKY, 1)
                : new Gain(PlayerKnowledge.WarpKind.TEMPORARY, 1 + random.nextInt(3));
    }
    public static void apply(PlayerKnowledge knowledge, Gain gain) {
        knowledge.changeWarp(gain.kind(), gain.amount());
        knowledge.setWarpCounter(knowledge.totalWarp());
    }
}

package gadomancymodern;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue PARTICLE_DISTANCE;
    static {
        var builder = new ForgeConfigSpec.Builder();
        PARTICLE_DISTANCE = builder.comment("粒子渲染最远距离（方块）；初始 100，范围 5..1000；图形品质和特效自身范围仍可进一步限制；客户端重载配置后生效。",
                "Maximum particle render distance in blocks; default 100, range 5..1000; graphics quality and effect-specific limits may restrict it further; applies after client config reload.")
                .defineInRange("particleRenderDistance", 100, 5, 1000);
        SPEC = builder.build();
    }
    public static double limitSquared(double effectLimit) {
        int configured = SPEC.isLoaded() ? PARTICLE_DISTANCE.get() : PARTICLE_DISTANCE.getDefault();
        return Math.min(effectLimit, (double) configured * configured);
    }
    public static double distanceSquared() { return limitSquared(Double.MAX_VALUE); }
    private ClientConfig() {}
}

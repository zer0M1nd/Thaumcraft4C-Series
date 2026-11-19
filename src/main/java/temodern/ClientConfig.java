package temodern;

import net.minecraftforge.common.ForgeConfigSpec;

/** Original author client rendering choice. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DISABLE_GEARBOX_MODEL;
    static {
        var builder = new ForgeConfigSpec.Builder();
        DISABLE_GEARBOX_MODEL = builder.comment("禁用齿轮箱动态轴与齿轮模型，保留普通方块外壳；布尔值，默认 false；客户端即时生效。",
                "Disable the animated gearbox shaft/gear model, retaining its ordinary block casing. Boolean; default false; client rendering updates immediately.")
                .define("disable_gearbox_model", false);
        SPEC = builder.build();
    }
}

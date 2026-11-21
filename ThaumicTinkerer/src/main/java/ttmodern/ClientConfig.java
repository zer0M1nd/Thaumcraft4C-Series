package ttmodern;

import net.minecraftforge.common.ForgeConfigSpec;

/** Original local display preferences; no gameplay profile or research dependency. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue INDICATORS, MIRROR_PREVIEW;
    static {
        var builder = new ForgeConfigSpec.Builder();
        INDICATORS = builder.comment("魔导手册中的 [TT] 来源提示；初始 true，可选 true/false；客户端重载配置后生效。",
                "[TT] origin indicator in Thauminomicon tooltips; default true, true/false; applies after client config reload.")
                .define("tooltipIndicators.enabled", true);
        MIRROR_PREVIEW = builder.comment("放置之镜的方块位置预览；初始 true，可选 true/false；只影响显示，客户端重载配置后生效。",
                "Placement Mirror block-position preview; default true, true/false; display only; applies after client config reload.")
                .define("placementMirror.blocks.show", true);
        SPEC = builder.build();
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID, value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Events {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void researchTooltip(thaumcraft.api.research.ResearchTooltipEvent event) {
            if (INDICATORS.get() && !event.locked() && event.research().startsWith("thaumic_tinkerer:"))
                event.setDescription(net.minecraft.network.chat.Component.literal("[TT] ")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE).append(event.description()));
        }
    }
    private ClientConfig() {}
}

package emtmodern.item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD)
public class HammerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MJOLNIR,DISABLE_SUPERCHARGED;
    public static final ForgeConfigSpec.IntValue BROKEN_WEIGHT;
    static{var builder=new ForgeConfigSpec.Builder();DISABLE_MJOLNIR=builder.comment("禁用雷神之锤研究", "设置说明 / Setting: Disable Mjolnir research", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("disable_mjolnir_research",false);DISABLE_SUPERCHARGED=builder.comment("禁用超载雷神之锤研究；基础研究依赖仍生效", "设置说明 / Setting: Disable Supercharged Mjolnir research; base research prerequisites still apply", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("disable_supercharged_mjolnir_research",false);BROKEN_WEIGHT=builder.comment("污染雷神之锤的战利品权重；0 禁用", "设置说明 / Setting: Tainted Mjolnir loot weight; 0 disables", "初始值 / Default: " + String.valueOf(25) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("tainted_mjolnir_spawning_weight",25,0,Integer.MAX_VALUE);SPEC=builder.build();}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){event.enqueueWork(()->{
        thaumcraft.api.research.ResearchCategories.registerEnablement("emt:mjolnir",()->!DISABLE_MJOLNIR.get());
        thaumcraft.api.research.ResearchCategories.registerEnablement("emt:supercharged_mjolnir",()->!DISABLE_MJOLNIR.get()&&!DISABLE_SUPERCHARGED.get());
    });}
}

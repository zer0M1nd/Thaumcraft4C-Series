package emtmodern.item;
import net.minecraftforge.common.ForgeConfigSpec;
public class LegacyToolConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue TOOLS_IN_BORE;
    static{var b=new ForgeConfigSpec.Builder();TOOLS_IN_BORE=b.comment("Original tools-for-Arcane-Bore option: larger charge durability and 1 EU/block for drills and omnitools.").comment("允许电力工具用于奥术钻探机", "设置说明 / Setting: Allow electric tools in the Arcane Bore", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("tools_for_arcane_bore",false);SPEC=b.build();}
}

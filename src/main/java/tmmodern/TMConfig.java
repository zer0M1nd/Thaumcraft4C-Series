package tmmodern;
import net.minecraftforge.common.ForgeConfigSpec;
public class TMConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue MACE_DAMAGE,WARP_REFRESH;
    public static final ForgeConfigSpec.BooleanValue RESEARCH_TAGS,CUSTOM_BACKGROUND;
    static {var b=new ForgeConfigSpec.Builder();MACE_DAMAGE=b.comment("法师之锤基础攻击伤害", "设置说明 / Setting: Mage Mace base attack damage", "初始值 / Default: " + String.valueOf(8.0) + "; 范围 / Range: " + String.valueOf(1.0) + ".." + String.valueOf(100.0), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("wands.mageMaceDamage",8.0,1.0,100.0);WARP_REFRESH=b.comment("扭曲木法杖补充间隔基数", "设置说明 / Setting: Warpwood wand recharge interval base", "初始值 / Default: " + String.valueOf(10000.0) + "; 范围 / Range: " + String.valueOf(100.0) + ".." + String.valueOf(100000.0), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("wands.warpwoodRefreshBase",10000.0,100.0,100000.0);RESEARCH_TAGS=b.comment("显示研究来源标记", "设置说明 / Setting: Show research origin indicators", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("research.tags",true);CUSTOM_BACKGROUND=b.comment("使用污秽魔法自定义研究背景", "设置说明 / Setting: Use Tainted Magic's custom research background", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("research.customBackground",false);SPEC=b.build();}
}

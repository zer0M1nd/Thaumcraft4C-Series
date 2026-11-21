package emtmodern.energy;
import net.minecraftforge.common.ForgeConfigSpec;
public class EmtFocusConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CHARGE_OUTPUT,WAND_CHARGE_COST;
    public static final ForgeConfigSpec.DoubleValue STATION_COST;
    static {var b=new ForgeConfigSpec.Builder();CHARGE_OUTPUT=b.comment("充电核心每 tick 提供的 EU", "设置说明 / Setting: EU supplied per tick by the charging focus", "初始值 / Default: " + String.valueOf(64) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("charge_focus_production",64,0,Integer.MAX_VALUE);WAND_CHARGE_COST=b.comment("法杖充能核心的 EU 消耗", "设置说明 / Setting: EU cost of the wand charging focus", "初始值 / Default: " + String.valueOf(40000) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("wand_charge_focus_cost",40000,0,Integer.MAX_VALUE);STATION_COST=b.comment("法杖充能机的 EU 消耗", "设置说明 / Setting: EU consumption of the wand charging station", "初始值 / Default: " + String.valueOf(50000d) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Double.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("wand_charger_consumption",50000d,0,Double.MAX_VALUE);SPEC=b.build();}
}

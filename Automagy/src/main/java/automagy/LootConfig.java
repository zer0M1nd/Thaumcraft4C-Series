package automagy;
import net.minecraftforge.common.ForgeConfigSpec;
/** Common config loads before datapack loot tables; server-per-world config is too late for injection. */
public final class LootConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.IntValue RING,PHIAL,SMITH_PHIAL;
 static{var builder=new ForgeConfigSpec.Builder();builder.push("loot");
  RING=builder.comment("Weight in normal dungeon item pools; 0 disables. Applied when loot tables load.").comment("普通地牢战利品池中次级末影分离戒指的权重；0 禁用", "设置说明 / Setting: Lesser Disjunction Ring weight in dungeon loot; 0 disables", "初始值 / Default: " + String.valueOf(1) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(10000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("lesserDisjunctionWeight",1,0,10000);
  PHIAL=builder.comment("普通地牢战利品池中浓缩经验瓶的权重；0 禁用", "设置说明 / Setting: XP phial weight in dungeon loot; 0 disables", "初始值 / Default: " + String.valueOf(3) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(10000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("experiencePhialWeight",3,0,10000);
  SMITH_PHIAL=builder.comment("村庄铁匠战利品池中浓缩经验瓶的权重；0 禁用", "设置说明 / Setting: XP phial weight in village smith loot; 0 disables", "初始值 / Default: " + String.valueOf(3) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(10000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("blacksmithPhialWeight",3,0,10000);
  builder.pop();SPEC=builder.build();
 }
 private LootConfig(){}
}

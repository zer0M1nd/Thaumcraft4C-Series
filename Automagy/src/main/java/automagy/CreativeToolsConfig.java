package automagy;
import net.minecraftforge.common.ForgeConfigSpec;
public final class CreativeToolsConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.BooleanValue LIST_JAR,LIST_CAP;
 static{var b=new ForgeConfigSpec.Builder();LIST_JAR=b.comment("在创造物品栏和配方浏览器中显示无限源质罐", "设置说明 / Setting: Show the Infinity Jar in creative inventory and recipe viewers", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("listInfinityJar",true);LIST_CAP=b.comment("在创造物品栏和配方浏览器中显示思考之帽", "设置说明 / Setting: Show the Thinking Cap in creative inventory and recipe viewers", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("listThinkingCap",true);SPEC=b.build();}
 private CreativeToolsConfig(){}
}

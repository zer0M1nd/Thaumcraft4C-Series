package tbmodern;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server config is synchronized by Forge; registry entries remain stable when modules are disabled. */
public final class Configuration {
    public static final ForgeConfigSpec SPEC, RECIPE_SPEC;
    public static final ForgeConfigSpec.BooleanValue TOBACCO, REVOLVER, NODE_TOOLS, BRACELETS, FLORA, BLOODY, TT_COMPAT, PEARL, ALUMENTUM;
    public static final ForgeConfigSpec.IntValue SHARDS, FURNACE_SPEED, POWDER_MIN, POWDER_MAX;
    static {
        var builder=new ForgeConfigSpec.Builder();builder.push("modules");
        TOBACCO=builder.comment("启用烟草内容", "设置说明 / Setting: Enable tobacco content", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("allowTobacco",true);REVOLVER=builder.comment("启用左轮手枪内容", "设置说明 / Setting: Enable revolver content", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enableRevolver",true);
        NODE_TOOLS=builder.comment("启用节点工具", "设置说明 / Setting: Enable node tools", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enableNodeTools",true);BRACELETS=builder.comment("启用手镯内容", "设置说明 / Setting: Enable bracelet content", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enableBracelets",true);
        FLORA=builder.comment("启用原有魔法植物", "设置说明 / Setting: Enable the original magical flora", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enableFlora",true);BLOODY=builder.comment("启用血腥套装内容", "设置说明 / Setting: Enable Bloody Mess content", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enableBloodyMess",true);
        TT_COMPAT=builder.comment("启用神秘工匠联动；仍需目标模组", "设置说明 / Setting: Enable Thaumic Tinkerer compatibility; requires that mod", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enableThaumicTinkererCompatibility",true);builder.pop();
        var recipes=new ForgeConfigSpec.Builder();recipes.comment("Loaded before server data packs. Recipe and book data are sent by the server; client values are not authoritative.").push("recipes");
        PEARL=recipes.comment("明亮核心的合成需要元始珍珠", "设置说明 / Setting: Bright focus crafting requires a primordial pearl", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("brightFociRequiresPrimordialPearl",true);SHARDS=recipes.comment("每块蕴魔石的碎片产量", "设置说明 / Setting: Shard output per infused stone", "初始值 / Default: " + String.valueOf(8) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(64), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("shardsFromOre",8,1,64);
        recipes.pop();RECIPE_SPEC=recipes.build();
        builder.push("furnace");FURNACE_SPEED=builder.comment("炼金炉加速倍率；0 不加速", "设置说明 / Setting: Alchemy furnace acceleration multiplier; 0 disables acceleration", "初始值 / Default: " + String.valueOf(2) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(128), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("speedMultiplier",2,0,128);
        ALUMENTUM=builder.comment("炼金炉加速要求使用炼金煤", "设置说明 / Setting: Require alumentum for alchemy furnace acceleration", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("requireAlumentumForAcceleration",true);builder.pop();builder.push("pyrofluid");
        POWDER_MIN=builder.comment("炽焱流体的粉末产出下限", "设置说明 / Setting: Minimum powder output from pyrofluid", "初始值 / Default: " + String.valueOf(5) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(4096), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("minimumPowder",5,0,4096);
        POWDER_MAX=builder.comment("Exclusive upper bound; if not above minimum, use the minimum without a random range.").comment("炽焱流体粉末产出上界（不含）；不大于下限时固定取下限", "设置说明 / Setting: Exclusive maximum pyrofluid powder output; use the minimum if maximum is not larger", "初始值 / Default: " + String.valueOf(37) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(4096), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("maximumPowder",37,0,4096);
        builder.pop();SPEC=builder.build();
    }
    public static int powderCount(net.minecraft.util.RandomSource random) {
        int minimum=POWDER_MIN.get(),maximum=POWDER_MAX.get();return maximum<=minimum?minimum:minimum+random.nextInt(maximum-minimum);
    }
    private Configuration() {}
}

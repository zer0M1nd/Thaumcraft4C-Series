package fmmodern;

import net.minecraftforge.common.ForgeConfigSpec;
import thaumcraft.api.research.ResearchCategories;

/** Legacy global settings; ConfigSync supplies the effective server values to clients. */
public final class ForbiddenConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue TAG_RESEARCH,NO_LUST,SILVERFISH_EMERALDS,GREEDY,EMERALD_TRANS,WRATH_CAGE,WRATH_CRAZY,ENCHANTING,CROSS_MOD,CROSS_WAND,BOTANIA,BLOOD_MAGIC,TINKERER,TWILIGHT,SPECIAL_MOBS,SPORK;
    public static final ForgeConfigSpec.IntValue WRATH_COST,WRATH_EFF,GLUTTONY,MANA_VIS,BLOOD_VIS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> GARBAGE;
    static {
        var b=new ForgeConfigSpec.Builder();b.push("general");
        TAG_RESEARCH=b.comment("在研究提示中显示禁忌魔法来源标记", "设置说明 / Setting: Show Forbidden Magic origin indicators in research tooltips", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("tagResearch",true);
        GARBAGE=b.comment("吞噬附魔删除的掉落物：物品 ID 或 #物品标签。默认使用内置垃圾标签；空列表不删除。旧矿物词典名改用标签；重启世界生效。",
                "Drops consumed by Consuming: item IDs or #item tags. Default [#forbidden_magic:consuming_trash]; an empty list consumes nothing. Modern tags replace ore-dictionary names. Restart the world.")
                .defineListAllowEmpty("garbageItems",java.util.List.of("#forbidden_magic:consuming_trash"),v->v instanceof String s&&net.minecraft.resources.ResourceLocation.tryParse(s.startsWith("#")?s.substring(1):s)!=null);
        NO_LUST=b.comment("Disable acquisition and research of lust-related content; registry IDs remain reserved.").comment("禁用欲望相关内容的获取和研究，保留注册及存档", "设置说明 / Setting: Disable acquisition and research of lust content; preserve registrations and saves", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("noLust",false);
        SPORK=b.comment("Use the legacy Spork of Doom name and appearance for the diabolist fork.").comment("恶魔之叉使用末日叉勺的旧版名称与外观", "设置说明 / Setting: Use the legacy Spork of Doom name and appearance for the diabolist fork", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("sporkOfDoom",false);
        SILVERFISH_EMERALDS=b.comment("蠹虫可掉落绿宝石碎片", "设置说明 / Setting: Silverfish may drop emerald fragments", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("silverfishEmeralds",true);GREEDY=b.comment("启用贪婪附魔；仍依赖附魔总开关", "设置说明 / Setting: Enable Greedy; still requires the enchantment master option", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("capitalist",true);EMERALD_TRANS=b.comment("启用绿宝石嬗变", "设置说明 / Setting: Enable emerald transmutation", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("emeraldTransmutation",true);
        WRATH_CAGE=b.comment("启用愤怒牢笼及相关研究", "设置说明 / Setting: Enable Wrath Cage and its research", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("wrathCage",true);WRATH_CRAZY=b.comment("Allow any non-boss mob, bypassing the curated cage list.").comment("愤怒牢笼允许非首领生物，绕过内置候选列表", "设置说明 / Setting: Allow non-boss mobs in Wrath Cages beyond the curated list", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("wrathCrazy",false);
        WRATH_COST=b.comment("Essentia per fuel purchase; zero makes spawning free.").comment("每次购买牢笼燃料消耗的源质；0 免费", "设置说明 / Setting: Essentia per cage fuel purchase; 0 is free", "初始值 / Default: " + String.valueOf(5) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(64), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("wrathCost",5,0,64);
        WRATH_EFF=b.comment("Successful spawns per fuel purchase.").comment("每份牢笼燃料允许的成功生成次数", "设置说明 / Setting: Successful spawns per cage fuel purchase", "初始值 / Default: " + String.valueOf(4) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(100000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("wrathEfficiency",4,1,100000);
        ENCHANTING=b.comment("启用禁忌魔法原有附魔", "设置说明 / Setting: Enable the original Forbidden Magic enchantments", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enchantments",true);GLUTTONY=b.comment("0 enabled, 1 disables food research, 2 legacy hardcore mode.").comment("暴食模式：0 正常，1 禁用食物研究，2 旧版硬核模式", "设置说明 / Setting: Gluttony mode: 0 normal, 1 disables food research, 2 legacy hardcore", "初始值 / Default: " + String.valueOf(0) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(2), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("gluttony",0,0,2);
        b.pop().push("compatibility");CROSS_MOD=b.comment("原有跨模组联动总开关；具体联动还需目标模组与子开关", "设置说明 / Setting: Original cross-mod integration master option; each integration also requires its mod and sub-option", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enabled",true);CROSS_WAND=b.comment("允许转换能量的联动法杖；仍需相应联动", "设置说明 / Setting: Enable energy-converting wands; relevant integration is also required", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("conversionWands",true);BOTANIA=b.comment("原有植物魔法联动；需安装 Botania", "设置说明 / Setting: Original Botania integration; requires Botania", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("botania",true);BLOOD_MAGIC=b.comment("原有血魔法联动；需安装 Blood Magic", "设置说明 / Setting: Original Blood Magic integration; requires Blood Magic", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("bloodMagic",true);TINKERER=b.comment("原有神秘工匠联动；需安装神秘工匠", "设置说明 / Setting: Original Thaumic Tinkerer integration; requires that mod", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("thaumicTinkerer",true);TWILIGHT=b.comment("原有暮色森林联动；需安装暮色森林", "设置说明 / Setting: Original Twilight Forest integration; requires that mod", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("twilightForest",true);SPECIAL_MOBS=b.comment("原有特殊生物联动；需安装对应模组", "设置说明 / Setting: Original Special Mobs integration; requires that mod", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("specialMobs",true);
        MANA_VIS=b.comment("每百分之一点 vis 所需魔力", "设置说明 / Setting: Mana per centi-vis", "初始值 / Default: " + String.valueOf(8) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(1000000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("manaPerCentiVis",8,1,1000000);BLOOD_VIS=b.comment("每百分之一点 vis 所需生命源质", "设置说明 / Setting: LP per centi-vis", "初始值 / Default: " + String.valueOf(5) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(1000000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("lpPerCentiVis",5,1,1000000);b.pop();WandConfiguration.define(b);SPEC=b.build();
    }
    public static boolean enchantment(String id){return value(ENCHANTING)&&(!id.equals("greedy")||value(GREEDY));}
    public static boolean garbage(net.minecraft.world.item.ItemStack stack) {
        for(String rule:SPEC.isLoaded()?GARBAGE.get():GARBAGE.getDefault()) {
            var id=net.minecraft.resources.ResourceLocation.parse(rule.startsWith("#")?rule.substring(1):rule);
            if(rule.startsWith("#")?stack.is(net.minecraft.tags.ItemTags.create(id)):id.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())))return true;
        }
        return false;
    }
    public static void setup(){
        WandConfiguration.setup();
        thaumcraft.api.ThaumcraftApi.addWarpToItem(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"wrath_cage"),5);
        thaumcraft.api.ThaumcraftApi.addWarpToItem(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"taint_sapling"),1);
        for(String key:new String[]{"CONSUMING","EDUCATIONAL","WRATH","CORRUPTING","CLUSTER","IMPACT","VOIDTOUCHED"})gate(key,()->value(ENCHANTING));
        gate("GREEDY",()->value(ENCHANTING)&&value(GREEDY));gate("TRANSEMERALD",()->value(EMERALD_TRANS));
        gate("SUBCOLLAR",()->!value(NO_LUST));gate("RIDINGCROP",()->!value(NO_LUST));
        gate("RINGFOOD",()->value(GLUTTONY)!=1);gate("ARCANECAKE",()->value(GLUTTONY)!=1);
        gate("FORK",()->value(WRATH_CAGE));gate("WRATHCAGE",()->value(WRATH_CAGE));
        gate("SCHOOLS",()->value(CROSS_MOD)&&(value(BOTANIA)&&net.minecraftforge.fml.ModList.get().isLoaded("botania")||value(BLOOD_MAGIC)&&net.minecraftforge.fml.ModList.get().isLoaded("bloodmagic")));
    }
    private static void gate(String key,java.util.function.BooleanSupplier enabled){ResearchCategories.registerEnablement("forbidden_magic:"+key,enabled);}
    public static java.util.List<ForgeConfigSpec.ConfigValue<?>> settings(){return java.util.List.of(TAG_RESEARCH,NO_LUST,SILVERFISH_EMERALDS,GREEDY,EMERALD_TRANS,WRATH_CAGE,WRATH_CRAZY,ENCHANTING,CROSS_MOD,CROSS_WAND,BOTANIA,BLOOD_MAGIC,TINKERER,TWILIGHT,SPECIAL_MOBS,SPORK,WRATH_COST,WRATH_EFF,GLUTTONY,MANA_VIS,BLOOD_VIS);}
    @SuppressWarnings("unchecked") public static <T> T value(ForgeConfigSpec.ConfigValue<T> setting){
        Object override=ConfigSync.override(setting);
        return override!=null?(T)override:SPEC.isLoaded()?setting.get():setting.getDefault();
    }
    private ForbiddenConfig(){}
}

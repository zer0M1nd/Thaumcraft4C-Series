package thaumicomniverse.compat;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.function.Predicate;

/** Localizes the comments emitted by Mekanism's MaterialCreator without modifying its API or config values. */
final class MaterialConfigBuilder extends ForgeConfigSpec.Builder {
    @Override public ForgeConfigSpec.Builder comment(String english) {
        String chinese = english;
        for (var entry : new String[][] {
                {"Material Settings for ","材料设置："},{"Base attack damage of ","基础攻击伤害："},
                {"Maximum durability of ","最大耐久度："},{"Attack damage modifier of ","攻击伤害修正："},
                {"Attack speed of ","攻击速度："},{"Efficiency of ","挖掘效率："},
                {"Natural enchantability factor of ","附魔能力系数："},{"Base armor toughness value of ","基础护甲韧性："},
                {"Base armor knockback resistance value of ","基础护甲击退抗性："},{"Protection value of ","护甲值："},
                {"thaumium","神秘锭"},{"void","虚空金属"},{"shields","盾牌"},{"swords","剑"},
                {"shovels","铲"},{"pickaxes","镐"},{"paxels","多用工具"},{"axes","斧"},{"hoes","锄"},
                {"boots","靴子"},{"leggings","护腿"},{"chestplates","胸甲"},{"helmets","头盔"},
                {"items","物品"},{"tools","工具"},{"armor","护甲"}}) chinese = chinese.replace(entry[0],entry[1]);
        if (!chinese.equals(english)) super.comment(chinese);
        return super.comment(english);
    }
    private void metadata(Object initial,String range) {
        super.comment("初始值 / Default: "+initial+"; 取值 / Allowed: "+range,
                "数值独立于模式；对应功能需启用神秘万象通用机械工具组。修改后重启世界或服务器。",
                "Values are profile-independent; their gameplay requires the Omniverse Mekanism tools group. Restart the world/server after editing.");
    }
    @Override public ForgeConfigSpec.IntValue defineInRange(String path,int initial,int min,int max) {
        metadata(initial,min+".."+max);return super.defineInRange(path,initial,min,max);
    }
    @Override public ForgeConfigSpec.DoubleValue defineInRange(String path,double initial,double min,double max) {
        metadata(initial,min+".."+max);return super.defineInRange(path,initial,min,max);
    }
    @Override public <T> ForgeConfigSpec.ConfigValue<T> define(String path,T initial) {
        metadata(initial,"与初始值相同类型 / same type as default");return super.define(path,initial);
    }
    @Override public <T> ForgeConfigSpec.ConfigValue<T> define(String path,T initial,Predicate<Object> validator) {
        metadata(initial,"Mekanism 合法伤害范围 / Mekanism valid damage range");return super.define(path,initial,validator);
    }
}

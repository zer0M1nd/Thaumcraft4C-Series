package emtmodern.item;

import net.minecraftforge.common.ForgeConfigSpec;

public class RingConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue ARMOR_OUTPUT, INVENTORY_OUTPUT, ONE_RING_WEIGHT;
    public static final ForgeConfigSpec.BooleanValue DISABLE_ONE_RING;
    static {
        var builder=new ForgeConfigSpec.Builder();
        ARMOR_OUTPUT=builder.comment("护甲充能戒指提供的 EU", "设置说明 / Setting: EU supplied by the armor charging ring", "初始值 / Default: " + String.valueOf(32) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("armor_charging_ring_production",32,0,Integer.MAX_VALUE);
        INVENTORY_OUTPUT=builder.comment("物品充能戒指提供的 EU", "设置说明 / Setting: EU supplied by the inventory charging ring", "初始值 / Default: " + String.valueOf(32) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("inventory_charging_ring_production",32,0,Integer.MAX_VALUE);
        DISABLE_ONE_RING=builder.comment("禁止至尊魔戒出现在地牢战利品中", "设置说明 / Setting: Disable The One Ring in dungeon loot", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("disable_one_ring_in_dungeon_chests",false);
        ONE_RING_WEIGHT=builder.comment("至尊魔戒战利品权重；上方禁用开关优先，0 也禁用", "设置说明 / Setting: The One Ring loot weight; the disable switch wins, 0 also disables", "初始值 / Default: " + String.valueOf(15) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("one_ring_spawning_weight",15,0,Integer.MAX_VALUE);
        SPEC=builder.build();
    }
}

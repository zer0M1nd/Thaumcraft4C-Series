package technomancy;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TechConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CONDENSER, BIOME, CONSUMER, BELLOWS, FUSOR, MANA_FAB, BLOOD_FAB, EXCHANGER;
    public static final ForgeConfigSpec.BooleanValue TREASURE_VILLAGERS, SMELTING_BONUS, HUD, FANCY;
    static {
        var b=new ForgeConfigSpec.Builder();
        b.push("energy");
        CONDENSER=b.defineInRange("condenser",1000000,1,10000000);
        BIOME=b.defineInRange("biomeTransmuter",20000,1,10000000);
        CONSUMER=b.defineInRange("eldritchConsumer",20000,1,10000000);
        BELLOWS=b.defineInRange("galvanizedBellows",500,1,1000000);
        FUSOR=b.defineInRange("essentiaFusor",1000,1,10000000);
        MANA_FAB=b.defineInRange("manaFabricator",1000000,1,10000000);
        BLOOD_FAB=b.defineInRange("bloodFabricator",1000000,1,10000000);
        EXCHANGER=b.defineInRange("manaExchanger",1000,1,10000000);
        b.pop();
        TREASURE_VILLAGERS=b.comment("Original optional treasure villagers can cause destructive world effects. Disabled in 0.12.5 by default.").define("treasureVillagers",false);
        SMELTING_BONUS=b.define("infernalFurnaceOreBonus",true);
        HUD=b.define("showExistenceHud",true);
        FANCY=b.define("fancyEffects",true);
        SPEC=b.build();
    }
    private TechConfig() {}
}

package conciliummodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Legacy mill numbers are retained in saves; item IDs replace old metadata subtypes. */
public enum PowderType {
    AIR("thaumcraft:air_shard","aer"),FIRE("thaumcraft:fire_shard","ignis"),WATER("thaumcraft:water_shard","aqua"),
    EARTH("thaumcraft:earth_shard","terra"),ORDER("thaumcraft:order_shard","ordo"),ENTROPY("thaumcraft:entropy_shard","perditio"),
    BALANCED("thaumcraft:balanced_shard",null),WRATH("forbidden_magic:wrath_shard","forbidden_magic:ira"),
    ENVY("forbidden_magic:envy_shard","forbidden_magic:invidia"),TAINT("forbidden_magic:taint_shard","vitium"),
    PRIDE("forbidden_magic:pride_shard","forbidden_magic:superbia"),LUST("forbidden_magic:lust_shard","forbidden_magic:luxuria"),
    SLOTH("forbidden_magic:sloth_shard","forbidden_magic:desidia"),GREED("forbidden_magic:greed_shard","lucrum"),
    GLUTTONY("forbidden_magic:gluttony_shard","forbidden_magic:gula");
    private final ResourceLocation item;
    private final String aspect;
    PowderType(String item,String aspect){this.item=ResourceLocation.parse(item);this.aspect=aspect;}
    public ItemStack shard(){var value=ForgeRegistries.ITEMS.getValue(item);return value==null?ItemStack.EMPTY:new ItemStack(value);}
    public int color(){return aspect==null?0xffffff:tc4revival.aspect.TCAspects.catalog().find(aspect).map(tc4revival.aspect.Aspect::color).orElse(0xffffff);}
    public boolean silver(){return ordinal()>=7;}
    public static PowderType byId(int id){return values()[net.minecraft.util.Mth.clamp(id,0,values().length-1)];}
}

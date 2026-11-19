package ttmodern;

import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.*;

public final class TinkererEnchantments {
    public static final DeferredRegister<Enchantment> REGISTRY=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,Tinkerer.ID);
    private static final Map<String,RegistryObject<Enchantment>> TYPES=new LinkedHashMap<>();
    static{
        add("ascent_boost",4,EnchantmentCategory.ARMOR_LEGS,EquipmentSlot.LEGS);add("slow_fall",3,EnchantmentCategory.ARMOR_FEET,EquipmentSlot.FEET);
        for(String name:List.of("flaming_touch","disintegrate"))add(name,1,EnchantmentCategory.DIGGER,EquipmentSlot.MAINHAND);
        add("quick_draw",2,EnchantmentCategory.BOW,EquipmentSlot.MAINHAND);add("vampirism",2,EnchantmentCategory.WEAPON,EquipmentSlot.MAINHAND);
        for(String name:List.of("focused_strike","dispersed_strike","final_strike","valiance"))add(name,5,EnchantmentCategory.WEAPON,EquipmentSlot.MAINHAND);
        for(String name:List.of("shatter","tunnel"))add(name,5,EnchantmentCategory.DIGGER,EquipmentSlot.MAINHAND);
        add("pounce",5,EnchantmentCategory.ARMOR_LEGS,EquipmentSlot.LEGS);add("shockwave",5,EnchantmentCategory.ARMOR_FEET,EquipmentSlot.FEET);
    }
    private static void add(String name,int max,EnchantmentCategory type,EquipmentSlot slot){TYPES.put(name,REGISTRY.register(name,()->new Entry(name,max,type,slot)));}
    public static Enchantment get(String name){return TYPES.get(name).get();}
    public static int level(String name,ItemStack stack){return EnchantmentHelper.getItemEnchantmentLevel(get(name),stack);}
    public static Collection<String> names(){return Collections.unmodifiableSet(TYPES.keySet());}
    private static String key(Enchantment enchantment){return ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString();}
    private static final class Entry extends Enchantment {
        private final String name;private final int maximum;
        Entry(String name,int maximum,EnchantmentCategory type,EquipmentSlot slot){super(Rarity.VERY_RARE,type,new EquipmentSlot[]{slot});this.name=name;this.maximum=maximum;}
        @Override public int getMaxLevel(){return maximum;}
        @Override public boolean isDiscoverable(){return false;}
        @Override public boolean isTradeable(){return false;}
        @Override public boolean canApplyAtEnchantingTable(ItemStack stack){return false;}
        @Override public boolean canEnchant(ItemStack stack){
            if(!category.canEnchant(stack.getItem()))return false;
            String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            if(name.equals("flaming_touch")&&id.equals("thaumcraft:elemental_axe"))return false;
            return !name.equals("vampirism")||!id.equals(Tinkerer.ID+":blood_sword");
        }
        @Override protected boolean checkCompatibility(Enchantment other){
            if(other==this)return false;String id=key(other);
            if(Set.of("flaming_touch","disintegrate").contains(name))return Set.of("minecraft:unbreaking","thaumcraft:repair").contains(id);
            if(Set.of("focused_strike","dispersed_strike").contains(name))return Set.of("minecraft:unbreaking","thaumcraft:repair","minecraft:knockback","minecraft:looting").contains(id);
            if(Set.of("final_strike","valiance").contains(name))return Set.of("minecraft:unbreaking","thaumcraft:repair","minecraft:sharpness","minecraft:smite","minecraft:bane_of_arthropods").contains(id);
            if(name.equals("quick_draw"))return !id.equals("minecraft:punch");
            if(name.equals("slow_fall"))return !id.equals("minecraft:feather_falling");
            if(name.equals("shockwave"))return !Set.of("minecraft:feather_falling",Tinkerer.ID+":slow_fall").contains(id);
            if(name.equals("vampirism"))return !Set.of("minecraft:fire_aspect","minecraft:knockback").contains(id);
            if(name.equals("shatter"))return !Set.of("minecraft:efficiency",Tinkerer.ID+":disintegrate").contains(id);
            if(name.equals("tunnel"))return !Set.of("minecraft:efficiency",Tinkerer.ID+":shatter").contains(id);
            return super.checkCompatibility(other);
        }
    }
}

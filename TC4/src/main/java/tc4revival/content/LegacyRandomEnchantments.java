package tc4revival.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

/** Native enchantment selection with retained symbolic definitions for addon API compatibility. */
public  class LegacyRandomEnchantments {
    private LegacyRandomEnchantments() {}
    private enum Target { ARMOR, BOOTS, HELMET, DAMAGE, SWORD, DIGGER, SHEARS, BREAKABLE, BOW, FISHING, HASTE, REPAIR }
    public enum Definition {
        PROTECTION(0,"protection",10,4,1,11,20,Target.ARMOR),
        FIRE_PROTECTION(1,"fire_protection",5,4,10,8,12,Target.ARMOR),
        FEATHER_FALLING(2,"feather_falling",5,4,5,6,10,Target.BOOTS),
        BLAST_PROTECTION(3,"blast_protection",2,4,5,8,12,Target.ARMOR),
        PROJECTILE_PROTECTION(4,"projectile_protection",5,4,3,6,15,Target.ARMOR),
        RESPIRATION(5,"respiration",2,3,10,10,30,Target.HELMET),
        AQUA_AFFINITY(6,"aqua_affinity",2,1,1,0,40,Target.HELMET),
        THORNS(7,"thorns",1,3,10,20,-1,Target.ARMOR),
        SHARPNESS(16,"sharpness",10,5,1,11,20,Target.DAMAGE),
        SMITE(17,"smite",5,5,5,8,20,Target.DAMAGE),
        BANE_OF_ARTHROPODS(18,"bane_of_arthropods",5,5,5,8,20,Target.DAMAGE),
        KNOCKBACK(19,"knockback",5,2,5,20,-1,Target.SWORD),
        FIRE_ASPECT(20,"fire_aspect",2,2,10,20,-1,Target.SWORD),
        LOOTING(21,"looting",2,3,15,9,-1,Target.SWORD),
        EFFICIENCY(32,"efficiency",10,5,1,10,-1,Target.SHEARS),
        SILK_TOUCH(33,"silk_touch",1,1,15,0,-1,Target.SHEARS),
        UNBREAKING(34,"unbreaking",5,3,5,8,-1,Target.BREAKABLE),
        FORTUNE(35,"fortune",2,3,15,9,-1,Target.DIGGER),
        POWER(48,"power",10,5,1,10,15,Target.BOW),
        PUNCH(49,"punch",2,2,12,20,25,Target.BOW),
        FLAME(50,"flame",2,1,20,0,30,Target.BOW),
        INFINITY(51,"infinity",1,1,20,0,30,Target.BOW),
        LUCK_OF_THE_SEA(61,"luck_of_the_sea",2,3,15,9,-1,Target.FISHING),
        LURE(62,"lure",2,3,15,9,-1,Target.FISHING),
        HASTE(150,"thaumcraft:haste",3,3,15,9,-1,Target.HASTE),
        REPAIR(151,"thaumcraft:repair",2,2,20,10,-1,Target.REPAIR);

        public final int legacyId, weight, maximumLevel;
        private final String id;
        private final int minimum, step, range;
        private final Target target;
        Definition(int legacyId,String id,int weight,int maximumLevel,int minimum,int step,int range,Target target) {
            this.legacyId=legacyId; this.id=id.contains(":") ? id : "minecraft:"+id;
            this.weight=weight; this.maximumLevel=maximumLevel; this.minimum=minimum; this.step=step; this.range=range; this.target=target;
        }
        public int minimumCost(int level) { return enchantment().getMinCost(level); }
        public int maximumCost(int level) { return enchantment().getMaxCost(level); }
        public Enchantment enchantment() {
            var result=ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.parse(id));
            if (result==null) throw new IllegalStateException("Missing legacy loot enchantment "+id);
            return result;
        }
        public boolean accepts(ItemStack stack) {
            return stack.is(Items.BOOK) || enchantment().canApplyAtEnchantingTable(stack);
        }
    }
    public record Choice(Definition definition,int level) {}
    public static java.util.Optional<Definition> definition(Enchantment enchantment) {
        return java.util.Arrays.stream(Definition.values()).filter(value->value.enchantment()==enchantment).findFirst();
    }
    public static boolean compatible(Definition first,Definition second) {
        return first.enchantment().isCompatibleWith(second.enchantment()) && second.enchantment().isCompatibleWith(first.enchantment());
    }
    /** Compatibility projection: the enum cannot represent enchantments introduced after TC4. */
    public static HashMap<Integer,Choice> candidates(ItemStack stack,int power) {
        var result=new HashMap<Integer,Choice>();
        for(var entry:net.minecraft.world.item.enchantment.EnchantmentHelper.getAvailableEnchantmentResults(power,stack,false))
            definition(entry.enchantment).ifPresent(value->result.put(value.legacyId,new Choice(value,entry.level)));
        return result;
    }
    /** Compatibility projection; game loot uses the complete native result in apply. */
    public static List<Choice> select(ItemStack stack,int power,RandomSource random) {
        var result=new ArrayList<Choice>();
        for(var entry:net.minecraft.world.item.enchantment.EnchantmentHelper.selectEnchantment(random,stack,power,false))
            definition(entry.enchantment).ifPresent(value->result.add(new Choice(value,entry.level)));
        return result;
    }
    public static ItemStack apply(ItemStack stack,int power,RandomSource random) {
        var chosen=net.minecraft.world.item.enchantment.EnchantmentHelper.selectEnchantment(random,stack,power,false);
        boolean book=stack.is(Items.BOOK);
        if(book) {
            var replacement=new ItemStack(Items.ENCHANTED_BOOK,stack.getCount());
            if(stack.hasTag())replacement.setTag(stack.getTag().copy());
            stack=replacement;
        }
        for(var entry:chosen) {
            if(book)EnchantedBookItem.addEnchantment(stack,entry);
            else stack.enchant(entry.enchantment,entry.level);
        }
        return stack;
    }
}

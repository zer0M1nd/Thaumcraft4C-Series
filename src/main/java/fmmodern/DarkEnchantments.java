package fmmodern;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.*;

public final class DarkEnchantments {
    public static final DeferredRegister<Enchantment> REGISTER = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ForbiddenMagic.ID);
    public static final Map<String, RegistryObject<Enchantment>> ALL = new LinkedHashMap<>();
    static {
        add("cluster",4,EnchantmentCategory.DIGGER); add("wrath",5,EnchantmentCategory.WEAPON);
        add("greedy",1,EnchantmentCategory.WEAPON); add("consuming",1,EnchantmentCategory.DIGGER);
        add("educational",5,EnchantmentCategory.WEAPON); add("corrupting",1,EnchantmentCategory.DIGGER);
        add("voidtouched",1,EnchantmentCategory.DIGGER); add("impact",1,EnchantmentCategory.DIGGER);
    }
    private static void add(String id,int max,EnchantmentCategory category) { ALL.put(id,REGISTER.register(id,()->new DarkEnchantment(id,max,category))); }
    public static int level(String id,ItemStack stack) { return ForbiddenConfig.enchantment(id)?EnchantmentHelper.getItemEnchantmentLevel(ALL.get(id).get(),stack):0; }
    static class DarkEnchantment extends Enchantment {
        private final String id; private final int max;
        DarkEnchantment(String id,int max,EnchantmentCategory category) { super(Rarity.VERY_RARE,category,new EquipmentSlot[]{EquipmentSlot.MAINHAND});this.id=id;this.max=max; }
        @Override public int getMaxLevel() { return max; }
        @Override public int getMinCost(int level) { return switch(id) { case "wrath" -> 5+(level-1)*11; case "greedy","corrupting" ->20; case "impact","voidtouched" ->30; default -> super.getMinCost(level); }; }
        @Override public int getMaxCost(int level) { return getMinCost(level)+switch(id) { case "wrath"->20;case "impact"->10;case "voidtouched"->30;default->50;}; }
        @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return ForbiddenConfig.enchantment(id)&&id.equals("wrath") && canEnchant(stack); }
        @Override public boolean isDiscoverable() { return ForbiddenConfig.enchantment(id)&&id.equals("wrath"); }
        @Override public boolean isTradeable() { return ForbiddenConfig.enchantment(id)&&id.equals("wrath"); }
        @Override public float getDamageBonus(int level,MobType type) { return ForbiddenConfig.enchantment(id)&&id.equals("wrath")?level*1.25f:0; }
        @Override public boolean canEnchant(ItemStack stack) {
            if(!ForbiddenConfig.enchantment(id))return false;
            var item=stack.getItem(); var key=ForgeRegistries.ITEMS.getKey(item); String path=key==null?"":key.getPath();
            return switch(id) {
                case "cluster" -> path.equals("morph_pickaxe") && key.getNamespace().equals(ForbiddenMagic.ID);
                case "impact" -> key!=null&&key.getNamespace().equals(ForbiddenMagic.ID)&&(path.equals("morph_pickaxe")||path.equals("morph_shovel"));
                case "voidtouched" -> key!=null&&key.getNamespace().equals(ForbiddenMagic.ID)&&path.startsWith("morph_");
                case "consuming" -> item instanceof PickaxeItem || item instanceof ShovelItem || path.equals("primal_crusher");
                case "corrupting" -> item instanceof PickaxeItem || path.equals("primal_crusher");
                // Forge's superclass calls canApplyAtEnchantingTable, which calls us again.
                case "wrath" -> item instanceof AxeItem || category.canEnchant(item);
                default -> category.canEnchant(item);
            };
        }
        @Override protected boolean checkCompatibility(Enchantment other) {
            if(!super.checkCompatibility(other)) return false;
            return switch(id) {
                case "cluster" -> other!=Enchantments.BLOCK_FORTUNE;
                case "consuming" -> other!=Enchantments.SILK_TOUCH;
                case "educational" -> other!=Enchantments.MOB_LOOTING&&other!=ALL.get("greedy").get();
                case "greedy" -> other!=Enchantments.MOB_LOOTING&&other!=ALL.get("educational").get();
                default -> true;
            };
        }
    }
}

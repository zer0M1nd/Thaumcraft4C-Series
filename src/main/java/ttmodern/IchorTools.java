package ttmodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.*;

/** Indestructible legacy Ichorium tools, expressed as a modern open tier. */
public final class IchorTools {
    public static final Tier TIER=TierSortingRegistry.registerTier(new ForgeTier(4,0,10,5,25,
            BlockTags.create(new ResourceLocation(Tinkerer.ID,"needs_ichorium_tool")),Ingredient::of),
            new ResourceLocation(Tinkerer.ID,"ichorium"),java.util.List.of(Tiers.NETHERITE),java.util.List.of());
    private static Item.Properties properties(){return new Item.Properties().stacksTo(1).rarity(Rarity.EPIC);}
    // These tools intentionally have no durability; vanilla's default check would
    // otherwise exclude them from both the enchanting table and the osmotic GUI.
    public static Item pick(){return new PickaxeItem(TIER,1,-2.8f,properties()){
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    };}
    public static Item shovel(){return new ShovelItem(TIER,1,-3,properties()){
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    };}
    public static Item axe(){return new AxeItem(TIER,3,-3,properties()){
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    };}
    public static Item sword(){return new SwordItem(TIER,4,-2.4f,properties()){
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    };}
    private IchorTools(){}
}

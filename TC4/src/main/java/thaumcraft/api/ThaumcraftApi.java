package thaumcraft.api;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
/** Main facade. Definitions/recipes/loot/tags use JSON; runtime integration uses the contracts in this package. */
public  class ThaumcraftApi {
    public static final int API_VERSION=1;
    private static final Map<ResourceLocation,Integer> CRAFTING_WARP=new LinkedHashMap<>();
    private ThaumcraftApi() {}
    public static synchronized void addWarpToItem(ResourceLocation item,int amount) {
        if(amount<0)throw new IllegalArgumentException("Negative crafting warp");CRAFTING_WARP.put(item,amount);
    }
    public static OptionalInt craftingWarp(ItemStack stack) {
        var value=CRAFTING_WARP.get(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()));return value==null?OptionalInt.empty():OptionalInt.of(value);
    }
    public static tc4revival.aspect.AspectCatalog aspects() { return tc4revival.aspect.TCAspects.catalog(); }
    public static tc4revival.research.ResearchCatalog research() { return tc4revival.research.ResearchCatalogs.catalog(); }
    public static ItemStack createWand(String rod,String cap,boolean sceptre,boolean charged) {
        return ((tc4revival.content.CastingWandItem)tc4revival.content.TCItems.CASTING_WAND.get()).create(tc4revival.content.WandParts.Rod.require(rod),tc4revival.content.WandParts.Cap.require(cap),sceptre,charged);
    }
}

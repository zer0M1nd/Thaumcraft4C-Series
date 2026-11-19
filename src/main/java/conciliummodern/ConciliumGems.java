package conciliummodern;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

/** Optional Gadomancy progression uses shared registry buses, with no optional API linkage. */
public final class ConciliumGems {
    public static final RegistryObject<DimensionalGem> RIFT_INERT=ConciliumItems.ITEMS.register("rift_gem_inert",()->new DimensionalGem(true,false));
    public static final RegistryObject<DimensionalGem> RIFT=ConciliumItems.ITEMS.register("rift_gem",()->new DimensionalGem(true,true));
    public static final RegistryObject<DimensionalGem> TERRA_INERT=ConciliumItems.ITEMS.register("terra_cast_gem_inert",()->new DimensionalGem(false,false));
    public static final RegistryObject<DimensionalGem> TERRA=ConciliumItems.ITEMS.register("terra_cast_gem",()->new DimensionalGem(false,true));
    public static final RegistryObject<Block> VOID_REALITY=ConciliumBlocks.BLOCKS.register("void_reality",VoidRealityBlock::new);
    public static final RegistryObject<Item> VOID_ITEM=ConciliumItems.ITEMS.register("void_reality",()->new BlockItem(VOID_REALITY.get(),new Item.Properties()));
    public static final RegistryObject<SimpleCraftingRecipeSerializer<TerraGemRecipe>> COMBINE=NetherMembraneRecipe.SERIALIZERS.register("terra_gem_binding",()->new SimpleCraftingRecipeSerializer<>(TerraGemRecipe::new));
    public static boolean available(){return net.minecraftforge.fml.ModList.get().isLoaded("gadomancy");}
    public static void init(){GemCaptureNetwork.init();}
    public static void setup(){
        for(String key:java.util.List.of("TCGADOMANCY","RIFTGEM","TERRACAST"))
            thaumcraft.api.research.ResearchCategories.registerEnablement(ThaumicConcilium.ID+":"+key,ConciliumGems::available);
    }
    private ConciliumGems(){}
}

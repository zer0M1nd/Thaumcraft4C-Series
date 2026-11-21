package thaumicomniverse.compat;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
public final class IndustrialLaserRecipes {
    /** Preserve the requested Thaumcraft outputs after pack unification, retaining edited rarity/lens fields. */
    public static void restoreOreOutputs(net.minecraft.world.item.crafting.RecipeManager manager) {
        for(String mineral:List.of("air_infused_stone","fire_infused_stone","water_infused_stone","earth_infused_stone",
                "order_infused_stone","entropy_infused_stone","amber_ore","cinnabar_ore")) {
            var key=ResourceLocation.parse("thaumicomniverse:expansion/laser/"+mineral);
            manager.byKey(key).ifPresent(recipe->{
                if(recipe instanceof com.buuz135.industrial.recipe.LaserDrillOreRecipe drill)
                    drill.output=net.minecraft.world.item.crafting.Ingredient.of(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+mineral)));
            });
        }
    }
}

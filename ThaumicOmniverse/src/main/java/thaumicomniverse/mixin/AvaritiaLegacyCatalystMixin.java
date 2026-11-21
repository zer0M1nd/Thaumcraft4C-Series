package thaumicomniverse.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "committee.nova.mods.avaritia.common.crafting.recipe.InfinityCatalystCraftRecipe", remap = false)
public abstract class AvaritiaLegacyCatalystMixin implements thaumicomniverse.compat.FeatureRecipeMutation {
    @Shadow public NonNullList<Ingredient> inputs;
    @Unique private Ingredient omniverse$addedPearl;
    @Override public void omniverse$applyFeatures() {
        boolean enabled=thaumicomniverse.RevivalFeatures.Group.AVARITIA_CATALYST.enabled();
        boolean changed=false;
        if (!enabled && omniverse$addedPearl!=null) {
            changed=inputs.remove(omniverse$addedPearl);omniverse$addedPearl=null;
        }
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumicomniverse", "extremely_primordial_pearl"));
        if (item == null) return;
        var stack = new ItemStack(item);
        if (enabled && inputs.stream().noneMatch(ingredient -> ingredient.test(stack))) {
            omniverse$addedPearl=Ingredient.of(stack);inputs.add(omniverse$addedPearl);changed=true;
        }

    }
}

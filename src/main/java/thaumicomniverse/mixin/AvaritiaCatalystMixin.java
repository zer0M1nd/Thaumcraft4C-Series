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
public abstract class AvaritiaCatalystMixin implements thaumicomniverse.compat.FeatureRecipeMutation {
    @Shadow @Final private NonNullList<Ingredient> originalInputs;
    @Unique private Ingredient omniverse$addedPearl;
    @Override public void omniverse$applyFeatures() {
        boolean enabled=thaumicomniverse.RevivalFeatures.Group.AVARITIA_CATALYST.enabled();
        boolean changed=false;
        if (!enabled && omniverse$addedPearl!=null) {
            changed=originalInputs.remove(omniverse$addedPearl);omniverse$addedPearl=null;
        }
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumicomniverse", "extremely_primordial_pearl"));
        if (item == null) return;
        var stack = new ItemStack(item);
        if (enabled && originalInputs.stream().noneMatch(ingredient -> ingredient.test(stack))) {
            omniverse$addedPearl=Ingredient.of(stack);originalInputs.add(omniverse$addedPearl);changed=true;
        }
        if(changed) committee.nova.mods.avaritia.common.crafting.recipe.InfinityCatalystCraftRecipe.INGREDIENTS_LOADED.removeBoolean(this);
    }
}

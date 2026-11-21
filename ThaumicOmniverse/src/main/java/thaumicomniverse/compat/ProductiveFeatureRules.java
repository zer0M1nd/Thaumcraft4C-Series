package thaumicomniverse.compat;

import cy.jdkdigital.productivebees.common.item.BeeCage;
import cy.jdkdigital.productivebees.common.item.Gene;
import cy.jdkdigital.productivebees.common.item.HoneyTreat;
import cy.jdkdigital.productivebees.common.recipe.BeeBreedingRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import thaumicomniverse.RevivalFeatures;

/** Loaded only when Productive Bees is present. Existing genomes and machine inventory remain intact. */
public final class ProductiveFeatureRules {
    public static boolean disabled(String type) {
        return type!=null&&type.startsWith("thaumicomniverse:")&&!RevivalFeatures.Group.PRODUCTIVE_BEES.enabled();
    }
    public static boolean incubationDisabled(IItemHandler inventory) {
        if(RevivalFeatures.Group.PRODUCTIVE_BEES.enabled())return false;
        if(disabled(BeeCage.getBeeType(inventory.getStackInSlot(0))))return true;
        for(var gene:HoneyTreat.getGenes(inventory.getStackInSlot(1)))
            if(gene instanceof CompoundTag data&&disabled(Gene.getAttributeName(ItemStack.of(data))))return true;
        return false;
    }
    public static boolean breedingDisabled(IItemHandler inventory,BeeBreedingRecipe recipe) {
        if(RevivalFeatures.Group.PRODUCTIVE_BEES.enabled())return false;
        if(disabled(BeeCage.getBeeType(inventory.getStackInSlot(1)))||disabled(BeeCage.getBeeType(inventory.getStackInSlot(2))))return true;
        return recipe!=null&&recipe.offspring!=null&&recipe.offspring.get()!=null
                &&disabled(recipe.offspring.get().getBeeType().toString());
    }
    private ProductiveFeatureRules() {}
}

package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.crafter.*;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;
import thaumcraft.api.wands.*;

public class ArcaneMechanicalCrafting {
    public record Plan(WandCharger.Tile charger,ArcaneRecipe recipe,CraftingContainer grid){
        public ItemStack result(boolean commit){
            var stack=charger.inventory.getStackInSlot(0);
            if(!(stack.getItem() instanceof CastingWandItem wand)||!(charger.getLevel() instanceof ServerLevel level))return ItemStack.EMPTY;
            var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            if(!wand.consumeForPlayer(stack,player,recipe.visCost(grid),true,true,commit))return ItemStack.EMPTY;
            var result=recipe.assemble(grid,level.registryAccess());if(commit)charger.changed();return result;
        }
    }
    public static Plan find(MechanicalCrafterBlockEntity root,RecipeGridHandler.GroupedItems items){
        if(!ArcaneContraptions.enabled()||!(root.getLevel() instanceof ServerLevel level))return null;
        items.calcStats();var grid=new MechanicalCraftingInventory(items);
        var chain=RecipeGridHandler.getAllCraftersOfChain(root);if(chain==null)return null;
        var seen=new java.util.HashSet<net.minecraft.core.BlockPos>();
        for(var crafter:chain)for(var face:Direction.values()){
            var pos=crafter.getBlockPos().relative(face);
            if(!seen.add(pos)||!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof WandCharger.Tile charger))continue;
            var stack=charger.inventory.getStackInSlot(0);var core=WandAutomation.core(stack);
            if(!(stack.getItem() instanceof CastingWandItem)||!(core.getItem() instanceof AutomationCore identity)||!identity.enabled(core)||identity.owner(core)==null)continue;
            var knowledge=identity.research(core,level);
            for(var recipe:level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()))
                if(recipe.matches(grid,level)&&recipe.unlocked(grid,knowledge::contains)&&!recipe.assemble(grid,level.registryAccess()).isEmpty())return new Plan(charger,recipe,grid);
        }return null;
    }
}

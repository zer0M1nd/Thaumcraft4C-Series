package automagy.compat.jei;

import automagy.content.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.*;
import mezz.jei.api.recipe.transfer.*;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

/** Optional JEI discovery loads this class only when JEI is installed. */
@JeiPlugin
public class AutomagyJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.parse("automagy:integration");}
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration){
        registration.addGhostIngredientHandler(automagy.client.FilterScreen.class,new TemplateGhostHandler<>());
        registration.addGhostIngredientHandler(automagy.client.GolemWorkbenchScreen.class,new TemplateGhostHandler<>());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){registration.addRecipeCatalyst(new ItemStack(Content.GOLEM_WORKBENCH.get()),RecipeTypes.CRAFTING);registration.addRecipeCatalyst(new ItemStack(Content.CRAFTING_FOCUS.get()),RecipeTypes.CRAFTING);}
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){
        var hidden=new java.util.ArrayList<ItemStack>();hidden.add(new ItemStack(Content.THAUMOSTATIC_SPIRE.get()));
        if(!automagy.CreativeToolsConfig.LIST_JAR.get())hidden.add(new ItemStack(Content.INFINITY_JAR.get()));
        if(!automagy.CreativeToolsConfig.LIST_CAP.get())hidden.add(new ItemStack(Content.THINKING_CAP.get()));
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,hidden);
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration){
        registration.addRecipeTransferHandler(new IRecipeTransferInfo<PortableCraftingMenu,CraftingRecipe>(){
            public Class<PortableCraftingMenu> getContainerClass(){return PortableCraftingMenu.class;}
            public java.util.Optional<net.minecraft.world.inventory.MenuType<PortableCraftingMenu>> getMenuType(){return java.util.Optional.of(Content.PORTABLE_CRAFTING_MENU.get());}
            public mezz.jei.api.recipe.RecipeType<CraftingRecipe> getRecipeType(){return RecipeTypes.CRAFTING;}
            public boolean canHandle(PortableCraftingMenu menu,CraftingRecipe recipe){return recipe.canCraftInDimensions(3,3);}
            public java.util.List<net.minecraft.world.inventory.Slot> getRecipeSlots(PortableCraftingMenu menu,CraftingRecipe recipe){return java.util.List.copyOf(menu.slots.subList(0,9));}
            public java.util.List<net.minecraft.world.inventory.Slot> getInventorySlots(PortableCraftingMenu menu,CraftingRecipe recipe){return java.util.stream.IntStream.range(11,47).filter(i->i!=38+menu.ownerSlot()).mapToObj(menu::getSlot).toList();}
        });
        registration.addRecipeTransferHandler(new IRecipeTransferHandler<GolemWorkbenchMenu,CraftingRecipe>(){
            public Class<GolemWorkbenchMenu> getContainerClass(){return GolemWorkbenchMenu.class;}
            public java.util.Optional<net.minecraft.world.inventory.MenuType<GolemWorkbenchMenu>> getMenuType(){return java.util.Optional.of(Content.GOLEM_WORKBENCH_MENU.get());}
            public mezz.jei.api.recipe.RecipeType<CraftingRecipe> getRecipeType(){return RecipeTypes.CRAFTING;}
            public IRecipeTransferError transferRecipe(GolemWorkbenchMenu menu,CraftingRecipe recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean max,boolean transfer){
                if(!recipe.canCraftInDimensions(3,3)||recipe.getIngredients().size()>9)return registration.getTransferHelper().createInternalError();
                if(transfer)FilterNetwork.recipeTemplate(menu.containerId,recipe.getId(),menu.value(0));
                return null;
            }
        },RecipeTypes.CRAFTING);
    }
}

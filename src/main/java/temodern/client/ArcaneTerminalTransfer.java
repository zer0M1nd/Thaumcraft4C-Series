package temodern.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.jei.JEIPlugin;
import appeng.integration.modules.jeirei.TransferHelper;
import appeng.menu.me.items.CraftingTermMenu.MissingIngredientSlots;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Ingredient;
import tc4revival.compat.jei.ArcaneTransferSlots;
import tc4revival.compat.jei.ThaumcraftJei;
import tc4revival.research.BrowserRecipe;
import temodern.crafting.ArcaneTerminalMenu;
import temodern.crafting.GhostRecipePacket;
import temodern.crafting.TerminalRecipePacket;
import temodern.crafting.WorkMenu;

final class ArcaneTerminalTransfer implements IRecipeTransferHandler<ArcaneTerminalMenu,BrowserRecipe> {
    private final IRecipeTransferHandlerHelper helper;
    ArcaneTerminalTransfer(IRecipeTransferHandlerHelper helper){this.helper=helper;}
    public Class<ArcaneTerminalMenu> getContainerClass(){return ArcaneTerminalMenu.class;}
    public Optional<MenuType<ArcaneTerminalMenu>> getMenuType(){return Optional.of(WorkMenu.TERMINAL.get());}
    public RecipeType<BrowserRecipe> getRecipeType(){return ThaumcraftJei.TYPES.get("arcane");}

    @Override public IRecipeTransferError transferRecipe(ArcaneTerminalMenu menu,BrowserRecipe recipe,IRecipeSlotsView display,
            Player player,boolean max,boolean transfer){
        if(recipe.inputs().size()!=9)return helper.createInternalError();
        var grid=new ArrayList<Ingredient>(9);
        var needed=new LinkedHashMap<Integer,Ingredient>();
        for(var choices:recipe.inputs()){
            var ingredient=choices.isEmpty()?Ingredient.EMPTY:Ingredient.of(choices.stream());
            if(!ingredient.isEmpty())needed.put(grid.size(),ingredient);
            grid.add(ingredient);
        }
        if(needed.isEmpty())return helper.createInternalError();
        var missing=menu.findMissingIngredients(needed);
        var views=ArcaneTransferSlots.items(display,recipe.inputs());
        if(missing.missingSlots().size()==needed.size())
            return helper.createUserErrorForMissingSlots(ItemModText.NO_ITEMS.text(),missing.missingSlots().stream().map(views::get).toList());
        boolean request=Screen.hasControlDown();
        if(transfer)GhostRecipePacket.CHANNEL.sendToServer(new TerminalRecipePacket(menu.containerId,grid,request));
        else if(missing.anyMissingOrCraftable())return new Highlights(views,missing,request);
        return null;
    }

    private record Highlights(List<IRecipeSlotView> views,MissingIngredientSlots missing,boolean request) implements IRecipeTransferError {
        public Type getType(){return Type.COSMETIC;}
        public int getButtonHighlightColor(){return missing.anyMissing()?TransferHelper.ORANGE_PLUS_BUTTON_COLOR:TransferHelper.BLUE_PLUS_BUTTON_COLOR;}
        public void showError(GuiGraphics graphics,int mouseX,int mouseY,IRecipeSlotsView slots,int x,int y){
            graphics.pose().pushPose();
            graphics.pose().translate(x,y,0);
            for(int index:missing.missingSlots())views.get(index).drawHighlight(graphics,TransferHelper.RED_SLOT_HIGHLIGHT_COLOR);
            for(int index:missing.craftableSlots())views.get(index).drawHighlight(graphics,TransferHelper.BLUE_SLOT_HIGHLIGHT_COLOR);
            graphics.pose().popPose();
            JEIPlugin.drawHoveringText(graphics,TransferHelper.createCraftingTooltip(missing,request),mouseX,mouseY);
        }
    }
}

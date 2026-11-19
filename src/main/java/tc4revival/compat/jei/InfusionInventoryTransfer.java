package tc4revival.compat.jei;

import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import tc4revival.research.BrowserRecipe;
import tc4revival.network.*;

/** Exact InventoryMenu scope leaves AE2 pattern terminal transfers untouched. */
public class InfusionInventoryTransfer implements IRecipeTransferHandler<InventoryMenu,BrowserRecipe> {
    public Class<InventoryMenu> getContainerClass() { return InventoryMenu.class; }
    public Optional<MenuType<InventoryMenu>> getMenuType() { return Optional.empty(); }
    public RecipeType<BrowserRecipe> getRecipeType() { return ThaumcraftJei.TYPES.get("infusion"); }
    public IRecipeTransferError transferRecipe(InventoryMenu menu,BrowserRecipe recipe,IRecipeSlotsView slots,Player player,boolean max,boolean transfer) {
        if(transfer) {
            int suffix=recipe.id().lastIndexOf('/');
            String id=suffix<0?recipe.id():recipe.id().substring(0,suffix);
            if(!recipe.inputs().isEmpty()&&!recipe.inputs().get(0).isEmpty())TCNetwork.CHANNEL.sendToServer(new InfusionPlacePacket(ResourceLocation.parse(id),recipe.inputs().get(0).get(0)));
            Minecraft.getInstance().setScreen(null);
        }
        return null;
    }
}

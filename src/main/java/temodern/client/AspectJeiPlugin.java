package temodern.client;

import appeng.api.integrations.jei.*;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import tc4revival.compat.jei.ThaumcraftJei;
import temodern.essentia.EssentiaKey;

@JeiPlugin
public class AspectJeiPlugin implements IModPlugin {
    public net.minecraft.resources.ResourceLocation getPluginUid(){return new net.minecraft.resources.ResourceLocation("thaumicenergistics","aspects");}
    @Override public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration){
        registerTransfer(registration,temodern.crafting.InscriberMenu.class,temodern.crafting.WorkMenu.INSCRIBER.get());
        registerTransfer(registration,appeng.menu.me.items.PatternEncodingTermMenu.class,appeng.menu.me.items.PatternEncodingTermMenu.TYPE);
        registration.addRecipeTransferHandler(new appeng.integration.modules.jei.transfer.UseCraftingRecipeTransfer<>(
                temodern.crafting.ArcaneTerminalMenu.class,temodern.crafting.WorkMenu.TERMINAL.get(),registration.getTransferHelper()),
                mezz.jei.api.constants.RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new ArcaneTerminalTransfer(registration.getTransferHelper()),ThaumcraftJei.TYPES.get("arcane"));
    }
    private <M extends net.minecraft.world.inventory.AbstractContainerMenu> void registerTransfer(mezz.jei.api.registration.IRecipeTransferRegistration registration,Class<M> clazz,net.minecraft.world.inventory.MenuType<M> type){
        var recipeType=ThaumcraftJei.TYPES.get("arcane");
        registration.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<M,tc4revival.research.BrowserRecipe>(){
            public Class<? extends M> getContainerClass(){return clazz;}
            public java.util.Optional<net.minecraft.world.inventory.MenuType<M>> getMenuType(){return java.util.Optional.of(type);}
            public mezz.jei.api.recipe.RecipeType<tc4revival.research.BrowserRecipe> getRecipeType(){return recipeType;}
            public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(M menu,tc4revival.research.BrowserRecipe recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean max,boolean transfer){
                if(transfer){var inputs=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();for(int i=0;i<9;i++)inputs.add(i<recipe.inputs().size()&&!recipe.inputs().get(i).isEmpty()?recipe.inputs().get(i).get(0).copyWithCount(1):net.minecraft.world.item.ItemStack.EMPTY);temodern.crafting.GhostRecipePacket.CHANNEL.sendToServer(new temodern.crafting.GhostRecipePacket(menu.containerId,inputs));}return null;
            }
        },recipeType);
    }
    @Override public void registerIngredients(mezz.jei.api.registration.IModIngredientRegistration registration){
        IngredientConverters.register(new IngredientConverter<ThaumcraftJei.AspectValue>(){
            public mezz.jei.api.ingredients.IIngredientType<ThaumcraftJei.AspectValue> getIngredientType(){return ThaumcraftJei.ASPECTS;}
            public GenericStack getStackFromIngredient(ThaumcraftJei.AspectValue value){var key=EssentiaKey.parse(value.tag());return key==null?null:new GenericStack(key,value.amount());}
            public ThaumcraftJei.AspectValue getIngredientFromStack(GenericStack stack){return stack.what() instanceof EssentiaKey key?new ThaumcraftJei.AspectValue(key.tag(),(int)Math.max(1,Math.min(Integer.MAX_VALUE,stack.amount()))):null;}
        });
    }
}

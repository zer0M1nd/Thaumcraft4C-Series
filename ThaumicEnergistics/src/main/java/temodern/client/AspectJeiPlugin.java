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
        terminalTransfer(registration,ThaumcraftJei.TYPES.get("arcane"),recipe->{
            var result=new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();
            for(int i=0;i<9;i++)result.add(i<recipe.inputs().size()?net.minecraft.world.item.crafting.Ingredient.of(recipe.inputs().get(i).stream()):net.minecraft.world.item.crafting.Ingredient.EMPTY);
            return result;
        });
        terminalTransfer(registration,mezz.jei.api.constants.RecipeTypes.CRAFTING,recipe->{
            var result=new java.util.ArrayList<>(java.util.Collections.nCopies(9,net.minecraft.world.item.crafting.Ingredient.EMPTY));
            int width=recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped?shaped.getWidth():3;
            for(int i=0;i<recipe.getIngredients().size()&&i<9;i++)result.set(i/width*3+i%width,recipe.getIngredients().get(i));
            return result;
        });
    }
    private <R> void terminalTransfer(mezz.jei.api.registration.IRecipeTransferRegistration registration,mezz.jei.api.recipe.RecipeType<R> type,
            java.util.function.Function<R,java.util.List<net.minecraft.world.item.crafting.Ingredient>> inputs){
        registration.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<temodern.crafting.ArcaneTerminalMenu,R>(){
            public Class<? extends temodern.crafting.ArcaneTerminalMenu> getContainerClass(){return temodern.crafting.ArcaneTerminalMenu.class;}
            public java.util.Optional<net.minecraft.world.inventory.MenuType<temodern.crafting.ArcaneTerminalMenu>> getMenuType(){return java.util.Optional.of(temodern.crafting.WorkMenu.TERMINAL.get());}
            public mezz.jei.api.recipe.RecipeType<R> getRecipeType(){return type;}
            public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(temodern.crafting.ArcaneTerminalMenu menu,R recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean max,boolean transfer){
                if(transfer)temodern.crafting.GhostRecipePacket.CHANNEL.sendToServer(new temodern.crafting.TerminalRecipePacket(menu.containerId,inputs.apply(recipe)));return null;
            }
        },type);
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

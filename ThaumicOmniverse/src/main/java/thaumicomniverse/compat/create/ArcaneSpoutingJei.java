package thaumicomniverse.compat.create;

import java.util.*;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tc4revival.compat.jei.ThaumcraftJei;

/** Separate optional class: loading the general JEI plugin must not load Create without Create. */
public class ArcaneSpoutingJei {
    private static final RecipeType<ArcaneProcessingRecipe> TYPE=RecipeType.create("thaumicomniverse","essentia_spouting",ArcaneProcessingRecipe.class);
    private static List<ArcaneProcessingRecipe> previous=List.of();
    private static IJeiRuntime runtime;
    private static int ticks;
    private static final RecipeType<com.simibubi.create.compat.jei.ConversionRecipe> CONVERSION=RecipeType.create("create","mystery_conversion",com.simibubi.create.compat.jei.ConversionRecipe.class);
    private static com.simibubi.create.compat.jei.ConversionRecipe burnerConversion;
    private static boolean conversionVisible;
    public static void categories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));}
    public static void catalysts(IRecipeCatalystRegistration registration){registration.addRecipeCatalyst(new ItemStack(ArcaneCreateContent.SPOUT.get()),TYPE);}
    private static List<ArcaneProcessingRecipe> recipes(){
        var level=Minecraft.getInstance().level;
        return level==null||!ArcaneContraptions.enabled()?List.of():level.getRecipeManager().getAllRecipesFor(ArcaneAssembly.ESSENTIA.type.get()).stream().sorted(Comparator.comparing(r->r.getId().toString())).toList();
    }
    public static void registerRecipes(IRecipeRegistration registration){
        previous=recipes();registration.addRecipes(TYPE,previous);
        burnerConversion=com.simibubi.create.compat.jei.ConversionRecipe.create(new ItemStack(ArcaneCreateContent.EMPTY_BURNER.get()),new ItemStack(ArcaneCreateContent.FILLED_BURNER.get()));
        conversionVisible=true;
        registration.addRecipes(CONVERSION,List.of(burnerConversion));
    }
    public static void runtime(IJeiRuntime value){runtime=value;net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ArcaneSpoutingJei.class);}
    public static void close(){runtime=null;previous=List.of();burnerConversion=null;conversionVisible=false;net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(ArcaneSpoutingJei.class);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||runtime==null||++ticks%20!=0)return;
        boolean show=Minecraft.getInstance().level!=null&&ArcaneContraptions.enabled();
        if(burnerConversion!=null&&show!=conversionVisible){
            if(show)runtime.getRecipeManager().unhideRecipes(CONVERSION,List.of(burnerConversion));
            else runtime.getRecipeManager().hideRecipes(CONVERSION,List.of(burnerConversion));
            conversionVisible=show;
        }
        var current=recipes();if(current.equals(previous))return;
        runtime.getRecipeManager().hideRecipes(TYPE,previous);runtime.getRecipeManager().addRecipes(TYPE,current);previous=current;
    }
    public static class Category implements mezz.jei.api.recipe.category.IRecipeCategory<ArcaneProcessingRecipe>{
        private final mezz.jei.api.gui.drawable.IDrawable background,icon,slot;
        private final AnimatedEssentiaSpout spout=new AnimatedEssentiaSpout();
        Category(IGuiHelper helper){background=helper.createBlankDrawable(177,70);icon=helper.createDrawableItemStack(new ItemStack(ArcaneCreateContent.SPOUT.get()));slot=com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot();}
        @Override public RecipeType<ArcaneProcessingRecipe> getRecipeType(){return TYPE;}
        @Override public Component getTitle(){return Component.translatable("omniverse.assembly.essentia_spouting");}
        @Override public mezz.jei.api.gui.drawable.IDrawable getBackground(){return background;}
        @Override public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
        @Override public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder layout,ArcaneProcessingRecipe recipe,IFocusGroup focus){
            layout.addSlot(RecipeIngredientRole.INPUT,27,51).setBackground(slot,-1,-1).addIngredients(recipe.getIngredients().get(0));
            var level=Minecraft.getInstance().level;
            layout.addSlot(RecipeIngredientRole.OUTPUT,132,51).setBackground(slot,-1,-1).addItemStack(recipe.getResultItem(level==null?net.minecraft.core.RegistryAccess.EMPTY:level.registryAccess()));
            for(var cost:recipe.aspects().view().entrySet())layout.addSlot(RecipeIngredientRole.INPUT,27,32).setBackground(slot,-1,-1)
                    .addIngredient(ThaumcraftJei.ASPECTS,new ThaumcraftJei.AspectValue(cost.getKey().tag(),cost.getValue()));
        }
        @Override public void draw(ArcaneProcessingRecipe recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY){
            com.simibubi.create.foundation.gui.AllGuiTextures.JEI_SHADOW.render(graphics,62,57);
            com.simibubi.create.foundation.gui.AllGuiTextures.JEI_DOWN_ARROW.render(graphics,126,29);
            spout.withAspect(recipe.aspects().aspects().get(0)).draw(graphics,background.getWidth()/2-13,22);
        }
    }
}

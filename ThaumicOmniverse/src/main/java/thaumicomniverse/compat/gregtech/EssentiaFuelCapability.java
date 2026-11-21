package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableInt;
import java.util.List;

public final class EssentiaFuelCapability extends RecipeCapability<EssentiaFuel> {
    public static final EssentiaFuelCapability CAP=new EssentiaFuelCapability();
    private EssentiaFuelCapability(){super("thaumicomniverse_essentia",0xB597E5,true,-1,new IContentSerializer<EssentiaFuel>() {
        public EssentiaFuel of(Object value){if(value instanceof EssentiaFuel fuel)return fuel;throw new IllegalArgumentException("Expected essentia fuel");}
        public EssentiaFuel defaultValue(){return new EssentiaFuel("ignis",1);}
        public Class<EssentiaFuel> contentClass(){return EssentiaFuel.class;}
        public Codec<EssentiaFuel> codec(){return EssentiaFuel.CODEC;}
    });}
    /** Aspect identity participates in GT's recipe tree; amount remains a transactional requirement. */
    public static void registerLookup(){
        com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager.registerMapIngredient(EssentiaFuel.class,
                fuel->new java.util.ArrayList<>(List.of(new AspectKey(fuel.aspect()))));
    }
    @Override public boolean isRecipeSearchFilter(){return true;}
    @Override public boolean doAddGuiSlots(){return true;}
    @Override public List<Object> createXEIContainerContents(List<Content> contents,GTRecipe recipe,IO io){return contents.stream().map(c->(Object)of(c.content)).toList();}
    @Override public Object createXEIContainer(List<?> contents){return contents;}
    @Override public Widget createWidget(){return new EssentiaFuelWidget();}
    @Override public Class<? extends Widget> getWidgetClass(){return EssentiaFuelWidget.class;}
    @Override public void applyWidgetInfo(Widget widget,int index,boolean isXEI,IO io,
            com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.RecipeHolder holder,com.gregtechceu.gtceu.api.recipe.GTRecipeType type,
            GTRecipe recipe,Content content,Object storage,int recipeTier,int chanceTier) {
        if(widget instanceof EssentiaFuelWidget slot&&content!=null)slot.fuel(of(content.content),io);
    }
    private static final class AspectKey extends com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient {
        private final String aspect;
        private AspectKey(String aspect){this.aspect=aspect;}
        @Override protected int hash(){return aspect.hashCode();}
        @Override public boolean equals(Object other){return other instanceof AspectKey key&&aspect.equals(key.aspect);}
    }
    @Override public EssentiaFuel copyInner(EssentiaFuel content){return content;}
    @Override public EssentiaFuel copyWithModifier(EssentiaFuel content,ContentModifier modifier){return new EssentiaFuel(content.aspect(),Math.max(1,modifier.apply(content.amount())));}
}

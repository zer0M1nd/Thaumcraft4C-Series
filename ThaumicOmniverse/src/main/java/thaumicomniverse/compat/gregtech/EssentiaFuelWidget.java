package thaumicomniverse.compat.gregtech;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.List;

/** A real JEI input slot using the same aspect ingredient as TC's crucible and infusion recipes. */
public final class EssentiaFuelWidget extends Widget implements IRecipeIngredientSlot {
    private EssentiaFuel fuel;
    private IngredientIO ingredientIO=IngredientIO.INPUT;
    public EssentiaFuelWidget(){super(0,0,18,18);}
    public void fuel(EssentiaFuel fuel,IO io) {
        this.fuel=fuel;ingredientIO=io==IO.IN?IngredientIO.INPUT:IngredientIO.OUTPUT;
        setHoverTooltips(Component.translatable("thaumicomniverse.gt.fuel",thaumcraft.api.aspects.AspectViews.display(tc4revival.aspect.TCAspects.catalog().require(fuel.aspect())).displayName(),fuel.amount()));
    }
    @Override public IngredientIO getIngredientIO(){return ingredientIO;}
    @Override public List<Component> getFullTooltipTexts() {
        return fuel==null?List.of():new tc4revival.compat.jei.ThaumcraftJei.AspectRenderer().getTooltip(
                new tc4revival.compat.jei.ThaumcraftJei.AspectValue(fuel.aspect(),fuel.amount()),net.minecraft.world.item.TooltipFlag.Default.NORMAL);
    }
    @Override public List<Object> getXEIIngredients() {
        if(fuel==null)return List.of();
        var typed=new mezz.jei.api.ingredients.ITypedIngredient<tc4revival.compat.jei.ThaumcraftJei.AspectValue>() {
            public mezz.jei.api.ingredients.IIngredientType<tc4revival.compat.jei.ThaumcraftJei.AspectValue> getType(){return tc4revival.compat.jei.ThaumcraftJei.ASPECTS;}
            public tc4revival.compat.jei.ThaumcraftJei.AspectValue getIngredient(){return new tc4revival.compat.jei.ThaumcraftJei.AspectValue(fuel.aspect(),fuel.amount());}
        };
        return List.of(new com.lowdragmc.lowdraglib.jei.ClickableIngredient<>(typed,getPositionX(),getPositionY(),18,18));
    }
    @Override public void drawInBackground(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        super.drawInBackground(graphics,mouseX,mouseY,partial);if(fuel==null)return;
        var aspect=tc4revival.aspect.TCAspects.catalog().require(fuel.aspect());int x=getPositionX()+1,y=getPositionY()+1;
        tc4revival.client.AspectTooltips.draw(graphics,Minecraft.getInstance().font,new tc4revival.client.AspectTooltips.Cell(aspect.tag(),""),x,y);
        String number=Integer.toString(fuel.amount());var font=Minecraft.getInstance().font;
        graphics.pose().pushPose();graphics.pose().translate(0,0,200);graphics.drawString(font,number,x+17-font.width(number),y+9,0xffffff,true);graphics.pose().popPose();
    }
}

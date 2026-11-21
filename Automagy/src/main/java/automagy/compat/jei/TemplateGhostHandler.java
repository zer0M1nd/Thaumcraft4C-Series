package automagy.compat.jei;

import automagy.content.*;
import java.util.*;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

/** Targets only the nine reference slots, never a result or real inventory slot. */
final class TemplateGhostHandler<T extends AbstractContainerScreen<?>> implements IGhostIngredientHandler<T> {
    public <I> List<Target<I>> getTargetsTyped(T gui,ITypedIngredient<I> ingredient,boolean starting){
        var stack=ingredient.getItemStack();if(stack.isEmpty()||stack.get().isEmpty())return List.of();
        var menu=gui.getMenu();if(!(menu instanceof FilterMenu)&&!(menu instanceof GolemWorkbenchMenu))return List.of();
        int page=menu instanceof GolemWorkbenchMenu workbench?workbench.value(0):-1;
        var targets=new ArrayList<Target<I>>();
        for(int i=0;i<9;i++){
            int index=i;var slot=menu.slots.get(i);var area=new Rect2i(gui.getGuiLeft()+slot.x,gui.getGuiTop()+slot.y,16,16);
            targets.add(new Target<>(){
                public Rect2i getArea(){return area;}
                public void accept(I ignored){FilterNetwork.ghost(menu.containerId,page,index,stack.get());}
            });
        }return targets;
    }
    public void onComplete() {}
}

package thaumicomniverse.compat.create;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.AspectTooltips;

public class ArcaneAssemblyTooltips {
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ArcaneAssemblyTooltips::gather);
        bus.addListener(Registration::register);
    }
    public record Ingredients(List<Ingredient> ingredients) implements TooltipComponent {}
    public static List<Component> lines(ArcaneProcessingRecipe step,int index){
        var result=new ArrayList<Component>();result.add(Component.translatable("omniverse.assembly.step",index,step.getDescriptionForAssembly()));
        if(step.infusion()){var ingredients=new JsonArray();step.components().forEach(value->ingredients.add(value.toJson()));result.add(Component.translatable("omniverse.assembly.ingredients_marker",ingredients.toString()));}
        var aspects=new JsonObject();step.aspects().view().forEach((a,n)->aspects.addProperty(a.tag(),n));result.add(Component.translatable("omniverse.assembly.aspects_marker",aspects.toString()));return result;
    }
    @SubscribeEvent public static void gather(RenderTooltipEvent.GatherComponents event){
        var elements=event.getTooltipElements();
        for(int i=0;i<elements.size();i++){
            var text=elements.get(i).left().orElse(null);
            if(!(text instanceof Component component) || !(component.getContents() instanceof TranslatableContents contents))continue;
            String key=contents.getKey();if(!key.startsWith("omniverse.assembly.") || !key.endsWith("_marker") || contents.getArgs().length!=1)continue;
            var json=JsonParser.parseString(contents.getArgs()[0].toString());
            if(key.equals("omniverse.assembly.aspects_marker")){
                var cells=new ArrayList<AspectTooltips.Cell>();json.getAsJsonObject().entrySet().forEach(e->cells.add(new AspectTooltips.Cell(e.getKey(),e.getValue().getAsString())));
                elements.set(i,Either.right(new AspectTooltips.Data(List.of(new AspectTooltips.Row(Component.empty(),cells)),Math.max(50,event.getScreenWidth()/2))));
            }else if(key.equals("omniverse.assembly.ingredients_marker")){
                var ingredients=new ArrayList<Ingredient>();json.getAsJsonArray().forEach(e->ingredients.add(Ingredient.fromJson(e)));elements.set(i,Either.right(new Ingredients(ingredients)));
            }
        }
    }
    public static class Registration {
        @SubscribeEvent public static void register(RegisterClientTooltipComponentFactoriesEvent event){event.register(Ingredients.class,IngredientRenderer::new);}
    }
    public static class IngredientRenderer implements ClientTooltipComponent {
        private final Ingredients data;
        public IngredientRenderer(Ingredients data){this.data=data;}
        @Override public int getHeight(){return (data.ingredients().size()+7)/8*18;}
        @Override public int getWidth(Font font){return Math.min(8,data.ingredients().size())*18;}
        @Override public void renderImage(Font font,int x,int y,GuiGraphics graphics){
            for(int i=0;i<data.ingredients().size();i++){var stacks=data.ingredients().get(i).getItems();if(stacks.length==0)continue;
                int chosen=(int)(net.minecraft.Util.getMillis()/1000%stacks.length);graphics.renderItem(stacks[chosen],x+i%8*18,y+i/8*18);graphics.renderItemDecorations(font,stacks[chosen],x+i%8*18,y+i/8*18);}
        }
    }
}

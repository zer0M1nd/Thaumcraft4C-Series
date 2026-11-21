package conciliummodern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.client.ResearchPageRenderers;
import tc4revival.client.LegacyGui;

/** The page resolves the current recipe after every reload instead of retaining an old recipe object. */
public final class ChainedRiftPage implements ResearchPageRenderers.Renderer {
    private final java.util.List<ResourceLocation> recipeIds;
    public ChainedRiftPage(com.google.gson.JsonObject data){
        var values=new java.util.ArrayList<ResourceLocation>();
        if(data.has("recipes"))for(var value:data.getAsJsonArray("recipes"))values.add(ResourceLocation.parse(value.getAsString()));
        else values.add(ResourceLocation.parse(data.get("recipe").getAsString()));
        if(values.isEmpty())throw new IllegalArgumentException("Empty chained rift recipe page");recipeIds=java.util.List.copyOf(values);
    }
    @Override public void render(ResearchPageRenderers.Context context,GuiGraphics graphics,int mouseX,int mouseY,float partial){
        var mc=Minecraft.getInstance();var bounds=context.bounds();if(mc.level==null)return;
        var recipeId=recipeIds.get((int)(mc.level.getGameTime()/40%recipeIds.size()));
        var found=mc.level.getRecipeManager().byKey(recipeId).orElse(null);if(!(found instanceof ChainedRiftRecipe recipe))return;
        int x=bounds.x(),y=bounds.y(),middle=x+bounds.width()/2;
        graphics.drawCenteredString(mc.font,Component.translatable("tc.research_name.thaumicconcilium:HEXOFPREDICTABILITY"),middle,y+4,0x503068);
        var output=recipe.getResultItem(mc.level.registryAccess());var inputs=recipe.getIngredients().get(0).getItems();
        if(inputs.length==0)return;
        var input=inputs[(int)((mc.level.getGameTime()/20)%inputs.length)].copyWithCount(recipe.catalystCount());
        graphics.renderItem(input,middle-44,y+36);graphics.renderItemDecorations(mc.font,input,middle-44,y+36);
        graphics.renderItem(output,middle+28,y+36);graphics.renderItemDecorations(mc.font,output,middle+28,y+36);
        LegacyGui.blit(graphics,ResourceLocation.parse("thaumicconcilium:textures/misc/hex.png"),middle-12,y+30,24,24,0,0,16,16,16,16);
        int index=0;
        for(var cost:recipe.aspectCosts().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList()){
            var aspect=tc4revival.aspect.TCAspects.catalog().require(cost.getKey());int ax=x+8+(index%3)*42,ay=y+78+(index/3)*38;index++;
            int color=thaumcraft.api.aspects.AspectViews.display(aspect).color();graphics.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
            LegacyGui.blit(graphics,thaumcraft.api.aspects.AspectViews.display(aspect).texture(),ax,ay,24,24,0,0,16,16,16,16);graphics.setColor(1,1,1,1);
            graphics.drawString(mc.font,Integer.toString(cost.getValue()),ax+12,ay+20,0xffffff);
            if(mouseX>=ax && mouseX<ax+32 && mouseY>=ay && mouseY<ay+32)graphics.renderTooltip(mc.font,Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()),mouseX,mouseY);
        }
        if(mouseY>=y+36 && mouseY<y+52){
            if(mouseX>=middle-44 && mouseX<middle-28)context.screen().ingredientTooltip(graphics,input,middle-44,y+36,mouseX,mouseY);
            if(mouseX>=middle+28 && mouseX<middle+44)context.screen().ingredientTooltip(graphics,output,middle+28,y+36,mouseX,mouseY);
        }
    }
}

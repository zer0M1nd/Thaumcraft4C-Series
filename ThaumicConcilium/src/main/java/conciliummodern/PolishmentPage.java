package conciliummodern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.LegacyGui;
import thaumcraft.api.client.ResearchPageRenderers;

/** A separate requirement page keeps the ordinary infusion page's links and tooltips intact. */
public class PolishmentPage implements ResearchPageRenderers.Renderer {
    private final ResourceLocation id;
    public PolishmentPage(com.google.gson.JsonObject data){id=ResourceLocation.parse(data.get("recipe").getAsString());}
    @Override public void render(ResearchPageRenderers.Context context,GuiGraphics graphics,int mx,int my,float partial){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(!(mc.level.getRecipeManager().byKey(id).orElse(null) instanceof PolishmentRecipe recipe))return;
        var bounds=context.bounds();int x=bounds.x(),y=bounds.y(),middle=x+bounds.width()/2;
        graphics.drawCenteredString(mc.font,Component.translatable("tc.crafting.polishment"),middle,y+4,0x503068);
        LegacyGui.blit(graphics,ResourceLocation.parse("thaumicconcilium:textures/gui/crystal_book.png"),middle-48,y+15,96,96,0,0,256,256,256,256);
        var result=recipe.getResultItem(mc.level.registryAccess());graphics.renderItem(result,middle-8,y+40);
        var aspect=tc4revival.aspect.TCAspects.catalog().require(recipe.aspect());int color=thaumcraft.api.aspects.AspectViews.display(aspect).color();
        graphics.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
        LegacyGui.blit(graphics,thaumcraft.api.aspects.AspectViews.display(aspect).texture(),middle-10,y+90,20,20,0,0,16,16,16,16);graphics.setColor(1,1,1,1);
        graphics.drawCenteredString(mc.font,Integer.toString(recipe.amount()),middle,y+111,0x503068);
        var description=Component.translatable("book.thaumicconcilium.polishment");
        int lines=mc.font.split(description,bounds.width()-6).size();
        int textY=Math.min(128,bounds.height()-lines*mc.font.lineHeight-3);
        graphics.drawWordWrap(mc.font,description,x+3,y+Math.max(116,textY),bounds.width()-6,0x302030);
        if(mx>=middle-8 && mx<middle+8 && my>=y+40 && my<y+56)graphics.renderTooltip(mc.font,result,mx,my);
        if(mx>=middle-10 && mx<middle+10 && my>=y+90 && my<y+111)graphics.renderTooltip(mc.font,Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()),mx,my);
    }
}

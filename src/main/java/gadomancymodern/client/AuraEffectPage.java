package gadomancymodern.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import thaumcraft.api.client.ResearchPageRenderers;
public class AuraEffectPage implements ResearchPageRenderers.Renderer {
    protected final java.util.List<String> tags=new java.util.ArrayList<>();
    public AuraEffectPage(com.google.gson.JsonObject data){data.getAsJsonArray("aspects").forEach(value->tags.add(value.getAsString()));}
    @Override public void render(ResearchPageRenderers.Context context,GuiGraphics graphics,int mouseX,int mouseY,float partial){var font=Minecraft.getInstance().font;int x=context.bounds().x()+7,y=context.bounds().y()+2;int step=Math.min(50,(context.bounds().height()-2-33-font.lineHeight)/Math.max(1,tags.size()-1));for(String tag:tags){var aspect=tc4revival.aspect.TCAspects.catalog().find(tag).orElse(null);if(aspect==null)continue;int color=thaumcraft.api.aspects.AspectViews.display(aspect).color();graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);tc4revival.client.LegacyGui.blit(graphics,thaumcraft.api.aspects.AspectViews.display(aspect).texture(),x,y,32,32,0,0,16,16,16,16);graphics.setColor(1,1,1,1);graphics.drawString(font,thaumcraft.api.aspects.AspectViews.display(aspect).displayName(),x+16-font.width(thaumcraft.api.aspects.AspectViews.display(aspect).displayName())/2,y+33,0x505050,false);var lines=new java.util.ArrayList<String>();String base="gadomancy.aura.effect."+tag;var language=net.minecraft.locale.Language.getInstance();if(language.has(base))lines.add(language.getOrDefault(base));else for(int i=0;language.has(base+"_"+i);i++)lines.add(language.getOrDefault(base+"_"+i));int lineY=y;for(String line:lines){graphics.drawWordWrap(font,Component.literal(line),x+38,lineY,94,0);lineY+=font.split(Component.literal(line),94).size()*9;}y+=step;}}
}

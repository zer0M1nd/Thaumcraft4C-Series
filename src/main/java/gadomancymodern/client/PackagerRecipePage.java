package gadomancymodern.client;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.client.ResearchPageRenderers;
/** Delegate to the actual book's arcane artwork after choosing the same server-configured recipe. */
public class PackagerRecipePage implements ResearchPageRenderers.Renderer {
    public void render(ResearchPageRenderers.Context context,GuiGraphics graphics,int mx,int my,float partial){
        var world=Minecraft.getInstance().level;if(world==null)return;
        var loaded=world.getRecipeManager().byKey(new ResourceLocation("gadomancy","arcane_packager")).orElse(null);if(!(loaded instanceof gadomancymodern.automation.PackagerRecipe recipe))return;
        var definition=recipe.displayDefinition();var inline=new JsonObject();inline.addProperty("kind","inline_arcane");inline.addProperty("research","gadomancy:arcane_packager");inline.addProperty("width",3);inline.addProperty("height",3);inline.addProperty("mirrored",true);inline.add("output",definition.get("result"));inline.add("aspects",definition.get("vis"));
        var sequence=new JsonObject();sequence.addProperty("kind","sequence");var elements=new JsonArray();
        for(var row:definition.getAsJsonArray("pattern"))for(char symbol:row.getAsString().toCharArray()){
            var element=new JsonObject();var ingredient=definition.getAsJsonObject("key").get(String.valueOf(symbol));
            if(ingredient==null){element.addProperty("kind","sequence");element.add("elements",new JsonArray());}
            else{element.addProperty("kind","stack");element.add("stack",ingredient.deepCopy());}elements.add(element);
        }
        sequence.add("elements",elements);inline.add("ingredients",sequence);var page=new JsonObject();page.addProperty("id","gadomancy:arcane_packager/preview");page.addProperty("type","ARCANE_CRAFTING");page.add("recipe",inline);
        tc4revival.client.ResearchCraftingArtwork.draw(context.screen(),graphics,tc4revival.research.ResearchPage.decode(page),0,context.bounds().x()+11,context.bounds().y()+2,mx,my);
    }
}

package arcanearteries;

import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observation and nonstandard book navigation only; gameplay inputs use MC Pilot. */
@Mod.EventBusSubscriber(modid=ArcaneArteries.ID,value=Dist.CLIENT)
public class ArteriesClientAudit {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("arcanearteries.validation"))return;
        var mc=Minecraft.getInstance();Path root=mc.gameDirectory.toPath(), request=root.resolve("arteries-request.json");
        if(!Files.exists(request))return;
        var data=JsonParser.parseString(Files.readString(request)).getAsJsonObject();Files.delete(request);
        var out=new JsonObject();out.add("request",data);
        try {
            switch(data.get("action").getAsString()){
                case "language" -> {mc.options.languageCode=data.get("value").getAsString();mc.getLanguageManager().setSelected(mc.options.languageCode);mc.reloadResourcePacks();}
                case "orbs" -> ArteriesConfig.ORBS.set(data.get("value").getAsBoolean());
                case "locate" -> {if(!(mc.screen instanceof tc4revival.client.ResearchBrowserScreen tree))throw new IllegalStateException("Open the real Thaumonomicon first");tree.locateRecipe(data.get("key").getAsString());}
                case "inventory" -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                case "close" -> mc.setScreen(null);
                case "snapshot" -> {}
                case "stop" -> mc.stop();
                default -> throw new IllegalArgumentException("Unknown audit action");
            }
            out.addProperty("screen",mc.screen==null?"none":mc.screen.getClass().getName());
            out.addProperty("serverHard",ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING));
            out.addProperty("localHard",ArteriesConfig.HARD_CRAFTING.get());
            out.addProperty("manaCost",ArteriesConfig.value(ArteriesConfig.MANA_COST));
            out.addProperty("localManaCost",ArteriesConfig.MANA_COST.get());
            out.addProperty("customName",ArteriesConfig.value(ArteriesConfig.SELF_NAME));
            out.addProperty("normalResearch",ArcaneArteries.researchEnabled("DAGGER"));
            out.addProperty("hardResearch",ArcaneArteries.researchEnabled("DAGGERH"));
            var names=new JsonObject();for(var item:ArcaneArteries.CONTENT.entrySet())names.addProperty(item.getKey(),item.getValue().get().getDefaultInstance().getHoverName().getString());out.add("itemNames",names);
            out.addProperty("success",true);
        }catch(Exception error){out.addProperty("success",false);out.addProperty("error",error.toString());}
        Files.writeString(root.resolve("arteries-response.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
}

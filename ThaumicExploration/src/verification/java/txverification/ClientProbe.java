package txverification;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumicexploration.*;

/** Isolated observation bridge for interfaces MC Pilot cannot enumerate. Never in product JAR. */
@Mod.EventBusSubscriber(modid="thaumicexploration_verification",value=Dist.CLIENT)
public final class ClientProbe {
    private static String last="";
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.player.tickCount%10!=0)return;
        var path=mc.gameDirectory.toPath().resolve("tx-client-request.json");if(!Files.isRegularFile(path))return;
        try{
            var request=JsonParser.parseString(Files.readString(path)).getAsJsonObject();String id=request.get("id").getAsString();if(last.equals(id))return;last=id;
            var result=new LinkedHashMap<String,Object>();result.put("id",id);
            switch(request.get("action").getAsString()){
                case "audit"->audit(result);
                case "book"->{String key=request.get("research").getAsString();if(mc.screen instanceof tc4revival.client.ResearchBrowserScreen tree)tree.locateRecipe(key);else if(mc.screen instanceof tc4revival.client.ResearchReaderScreen reader)result.put("follow",reader.follow(key));else throw new IllegalStateException("Open the real Thaumonomicon first");}
                case "close"->mc.player.closeContainer();
                case "look"->{mc.player.setYRot(request.get("yaw").getAsFloat());mc.player.setXRot(request.get("pitch").getAsFloat());}
                case "language"->{String code=request.get("code").getAsString();mc.options.languageCode=code;mc.getLanguageManager().setSelected(code);mc.options.save();mc.reloadResourcePacks();}
                default->throw new IllegalArgumentException("Unknown probe action");
            }
            result.put("screen",mc.screen==null?"world":mc.screen.getClass().getName());result.put("success",true);
            Files.writeString(mc.gameDirectory.toPath().resolve("tx-client-response.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }catch(Throwable error){try{Files.writeString(mc.gameDirectory.toPath().resolve("tx-client-response.json"),new Gson().toJson(Map.of("id",last,"success",false,"error",error.toString())));}catch(Exception ignored){}}
    }
    private static void audit(Map<String,Object> result){
        var mc=Minecraft.getInstance();var missing=new ArrayList<String>();var translations=new LinkedHashMap<String,String>();int quads=0;
        for(var entry:ExplorationMod.ITEM.entrySet()){
            var stack=new ItemStack(entry.getValue().get());translations.put(entry.getKey(),stack.getHoverName().getString());
            if(!net.minecraft.locale.Language.getInstance().has(stack.getDescriptionId()))missing.add("language:"+stack.getDescriptionId());
            var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            if(model==mc.getModelManager().getMissingModel())missing.add("model:"+entry.getKey());
            var faces=new ArrayList<net.minecraft.core.Direction>(Arrays.asList(net.minecraft.core.Direction.values()));faces.add(null);
            for(var face:faces)for(var quad:model.getQuads(null,face,RandomSource.create(0))){quads++;if(quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))missing.add("texture:"+entry.getKey());}
        }
        int pages=0,recipes=0;var enabled=new LinkedHashMap<String,Boolean>();
        for(var entry:tc4revival.research.ResearchCatalogs.bundled().all())if(entry.key().startsWith("thaumicexploration:")&&!entry.mirror()){
            enabled.put(entry.key(),tc4revival.research.ResearchCatalogs.enabled(entry));
            if(!net.minecraft.locale.Language.getInstance().has("tc.research_name."+entry.key()))missing.add("research-title:"+entry.key());
            for(var page:tc4revival.research.ResearchPages.bundled().require(entry.key())){
                pages++;page.text().ifPresent(key->{if(!net.minecraft.locale.Language.getInstance().has(key))missing.add("research-text:"+key);});
                if(tc4revival.research.LiveBookRecipes.supported(page.type())&&tc4revival.research.ResearchCatalogs.enabled(entry)){
                    var views=tc4revival.client.ClientLiveBookRecipes.get(page);recipes+=views.size();if(views.isEmpty())missing.add("recipe-page:"+entry.key()+"/"+page.id());
                }
            }
        }
        result.put("language",mc.options.languageCode);result.put("items",translations);result.put("quads",quads);result.put("pages",pages);result.put("recipe_views",recipes);result.put("missing",missing);
        result.put("enabled",enabled);
        var jar=mc.level.getBlockEntity(new net.minecraft.core.BlockPos(0,80,24));if(jar instanceof BoundEntity be)result.put("visual_jar",Map.of("amount",be.amount(),"color",be.color,"network",be.network));
    }
}

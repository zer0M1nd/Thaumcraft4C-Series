package tc4validation.config;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.*;
import tc4revival.config.TCConfig;
import tc4revival.research.*;

/** Read-only screen inspection, requested through this isolated client's directory. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class ConfigurationClientProbe {
    private static String previous="";
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("tc4.configurationValidation")||event.phase!=TickEvent.Phase.END||++ticks%10!=0)return;
        var mc=Minecraft.getInstance();var root=mc.gameDirectory.toPath();var request=root.resolve("configuration-probe-request.json");
        if(!Files.isRegularFile(request))return;
        try{
            var text=Files.readString(request);if(text.equals(previous))return;previous=text;
            var result=new JsonObject();result.add("request",JsonParser.parseString(text));result.addProperty("time",java.time.Instant.now().toString());
            result.addProperty("profile",TCConfig.SERVER.revival.profile().name());
            result.addProperty("access",ResearchTableAccess.resolve(ClientKnowledge.snapshot().orElse(null)).toString());
            if(mc.level!=null) {
                var aspects=new JsonObject();for(String id:List.of("minecraft:copper_ingot","minecraft:amethyst_shard","create:andesite_alloy")) {
                    var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id));
                    if(item!=null&&item!=net.minecraft.world.item.Items.AIR) {
                        var resolved=ClientItemAspects.resolve(new net.minecraft.world.item.ItemStack(item));
                        var sample=new JsonObject();sample.addProperty("resolved",resolved.isPresent());
                        var amounts=new JsonObject();resolved.ifPresent(r->r.amounts().view().forEach((a,n)->amounts.addProperty(a.tag(),n)));
                        sample.add("amounts",amounts);aspects.add(id,sample);
                    }
                }result.add("aspectSamples",aspects);
                var recipes=new JsonArray();mc.level.getRecipeManager().getRecipes().stream().map(r->r.getId().toString())
                        .filter(ConfigurationClientProbe::target).sorted().forEach(recipes::add);result.add("targetRecipeIds",recipes);
                var research=new JsonArray();ResearchCatalogs.bundled().all().stream().filter(ResearchCatalogs::enabled)
                        .map(ResearchDefinition::key).filter(k->target(k)||k.equals("CONVENIENTTUBES")||k.equals("ALCHEMICALBRASS")||k.startsWith("REVIVAL_")).sorted().forEach(research::add);
                result.add("enabledTargetResearch",research);
                var tabs=new JsonObject();net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.forEach(tab->{
                    var key=net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    if(key!=null&&target(key.toString())){var items=new JsonArray();tab.getDisplayItems().stream().map(s->net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).toString()).distinct().sorted().forEach(items::add);tabs.add(key.toString(),items);}
                });result.add("creativeTabs",tabs);
                if(net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
                    var field=tc4revival.compat.jei.JeiInferenceExport.class.getDeclaredField("runtime");field.setAccessible(true);
                    var runtime=(mezz.jei.api.runtime.IJeiRuntime)field.get(null);var visible=new JsonArray();
                    if(runtime!=null)runtime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).stream()
                            .filter(s->target(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).toString()))
                            .filter(s->runtime.getIngredientVisibility().isIngredientVisible(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,s))
                            .map(s->net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).toString()).distinct().sorted().forEach(visible::add);
                    result.addProperty("jeiReady",runtime!=null);result.add("jeiVisibleTargetItems",visible);
                }
            }
            var screen=mc.screen;result.addProperty("screen",screen==null?"none":screen.getClass().getName());
            if(screen!=null){
                result.addProperty("width",screen.width);result.addProperty("height",screen.height);
                if(screen instanceof AbstractContainerScreen<?> c){result.addProperty("left",c.getGuiLeft());result.addProperty("top",c.getGuiTop());}
                var fields=new JsonObject();
                if(screen instanceof ResearchTableScreen)for(String name:List.of("access","page","aspects","note","shortcuts","autoConnect","plannedWrites","search","searchStrategy")){
                    var field=screen.getClass().getDeclaredField(name);field.setAccessible(true);var v=field.get(screen);
                    if(v instanceof EditBox box){var b=new JsonObject();b.addProperty("text",box.getValue());b.addProperty("x",box.getX());b.addProperty("y",box.getY());b.addProperty("visible",box.visible);b.addProperty("active",box.active);fields.add(name,b);}
                    else fields.addProperty(name,String.valueOf(v));
                }
                result.add("fields",fields);
                if(screen instanceof ResearchBrowserScreen) {
                    var field=ResearchBrowserScreen.class.getDeclaredField("search");field.setAccessible(true);
                    result.addProperty("bookSearchVisible",field.get(screen)!=null);
                    if(field.get(screen) instanceof EditBox box){result.addProperty("bookSearchText",box.getValue());result.addProperty("bookSearchX",box.getX());result.addProperty("bookSearchY",box.getY());}
                    var navField=ResearchBrowserScreen.class.getDeclaredField("navigation");navField.setAccessible(true);
                    var nav=(ResearchBookNavigation)navField.get(screen);result.addProperty("bookCategory",nav.category());
                    var categories=new JsonArray();nav.searchable().stream().map(n->n.research().category()).distinct().sorted().forEach(categories::add);result.add("bookSearchableCategories",categories);
                    var candidates=ResearchBrowserScreen.class.getDeclaredField("candidates");candidates.setAccessible(true);result.addProperty("bookCandidates",String.valueOf(candidates.get(screen)));
                }
            }
            if(mc.player!=null){var menu=mc.player.containerMenu;var slots=new JsonArray();for(var s:menu.slots){var r=new JsonObject();r.addProperty("slot",s.index);r.addProperty("stack",s.getItem().save(new net.minecraft.nbt.CompoundTag()).toString());slots.add(r);}result.add("slots",slots);}
            Files.writeString(root.resolve("configuration-probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("CONFIGURATION_CLIENT_PROBE").error("Read-only probe failed",e);}
    }
    private static boolean target(String key) { return key.startsWith("thaumicomniverse:")||key.startsWith("thaumicenergistics:"); }
}

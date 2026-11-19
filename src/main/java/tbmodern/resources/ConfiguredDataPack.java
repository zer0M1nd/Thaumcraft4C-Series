package tbmodern.resources;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tbmodern.Configuration;

/** Config-derived server data below user data packs; no base internals or registry mutation. */
@Mod.EventBusSubscriber(modid="thaumicbases",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConfiguredDataPack extends AbstractPackResources {
    private final Map<ResourceLocation,byte[]> files;
    public ConfiguredDataPack(String id) {
        super(id,true);
        var aspects=tc4revival.aspect.TCAspects.catalog();
        String nether=aspects.find("infernus").map(tc4revival.aspect.Aspect::tag)
            .orElseGet(()->aspects.find("forbidden_magic:infernus").or(()->aspects.find("forbiddenmagic:infernus")).map(tc4revival.aspect.Aspect::tag).orElse("ignis"));
        files=prepare(Configuration.PEARL.get(),Configuration.SHARDS.get(),nether);
    }
    public static Map<ResourceLocation,byte[]> prepare(boolean pearl,int shards) {
        return prepare(pearl,shards,"ignis");
    }
    /** Optional Nether aspect follows the registered catalog, with no dependency on external Java classes. */
    public static Map<ResourceLocation,byte[]> prepare(boolean pearl,int shards,String nether) {
        if(nether==null || !nether.matches("[a-z0-9_.:-]+"))throw new IllegalArgumentException("Invalid optional aspect tag");
        if(shards<1 || shards>64)throw new IllegalArgumentException("Invalid shard yield");
        try(var input=Objects.requireNonNull(ConfiguredDataPack.class.getResourceAsStream("/tbmodern/configured-data.json"))) {
            var templates=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            Map<ResourceLocation,byte[]> result=new LinkedHashMap<>();
            for(var entry:templates.entrySet()) {
                String path=entry.getKey();JsonObject data=entry.getValue().getAsJsonObject();
                if(path.startsWith("recipes/alchemy/shards_"))data.getAsJsonObject("result").addProperty("count",shards);
                if(path.equals("recipes/equipment/nethersaplingrec.json") && !nether.equals("ignis")) {
                    var costs=data.getAsJsonObject("aspects");costs.remove("ignis");costs.addProperty(nether,32);
                }
                if(path.equals("research_catalog/modern.json") && !nether.equals("ignis"))for(var category:data.getAsJsonArray("categories"))for(var element:category.getAsJsonObject().getAsJsonArray("research")) {
                    var research=element.getAsJsonObject();if(!research.get("key").getAsString().equals("TB.NetherSapling"))continue;
                    var costs=research.getAsJsonObject("aspects");costs.addProperty("ignis",8);costs.addProperty(nether,8);
                }
                if(!pearl && path.equals("recipes/equipment/brightfocirec.json")) {
                    data.getAsJsonObject("center").getAsJsonObject("ingredient").addProperty("item","thaumcraft:primal_charm");
                    brightCosts(data.getAsJsonObject("aspects"));
                }
                if(!pearl && path.equals("research_catalog/modern.json"))for(var category:data.getAsJsonArray("categories"))for(var element:category.getAsJsonObject().getAsJsonArray("research")) {
                    var research=element.getAsJsonObject();if(!research.get("key").getAsString().equals("TB.NodeFoci.Bright"))continue;
                    var parents=new JsonArray();research.getAsJsonArray("parents").forEach(parent->{if(!parent.getAsString().equals("PRIMPEARL"))parents.add(parent);});research.add("parents",parents);
                }
                if(path.equals("research_page_recipes/modern.json"))for(var element:data.getAsJsonArray("entries")) {
                    var row=element.getAsJsonObject();String key=row.get("key").getAsString();var display=row.getAsJsonObject("data");
                    if(key.startsWith("thaumicbases:alchemy/shards_"))display.getAsJsonObject("output").getAsJsonObject("stack").addProperty("count",shards);
                    if(key.equals("thaumicbases:equipment/nethersaplingrec") && !nether.equals("ignis")) {
                        var costs=display.getAsJsonObject("aspects");costs.remove("ignis");costs.addProperty(nether,32);
                    }
                    if(!pearl && key.equals("thaumicbases:equipment/brightfocirec")) {
                        display.getAsJsonObject("input").getAsJsonObject("stack").addProperty("item","thaumcraft:primal_charm");brightCosts(display.getAsJsonObject("aspects"));
                    }
                }
                result.put(new ResourceLocation("thaumicbases",path),data.toString().getBytes(StandardCharsets.UTF_8));
            }
            try(var aspectInput=Objects.requireNonNull(ConfiguredDataPack.class.getResourceAsStream("/tbmodern/bright-aspects.json"))) {
                var variants=JsonParser.parseReader(new InputStreamReader(aspectInput,StandardCharsets.UTF_8)).getAsJsonObject();
                var aspects=new JsonObject();aspects.addProperty("format",1);var entries=new JsonArray();entries.add(variants.get(pearl?"pearl":"charm"));aspects.add("entries",entries);
                result.put(new ResourceLocation("thaumicbases","item_aspects/configured_brightness.json"),aspects.toString().getBytes(StandardCharsets.UTF_8));
            }
            {
                var data=new JsonObject();data.addProperty("format",1);var entries=new JsonArray();
                for(String part:List.of("leaves","sapling","log")) {
                    var row=new JsonObject();var items=new JsonArray();items.add("thaumicbases:nether_"+part);row.add("items",items);
                    var quantities=new JsonArray();quantity(quantities,part.equals("log")?"arbor":"herba",part.equals("leaves")?1:3);
                    if(part.equals("log") && !nether.equals("ignis"))quantity(quantities,"ignis",1);
                    quantity(quantities,nether,part.equals("log") && nether.equals("ignis")?2:1);row.add("aspects",quantities);entries.add(row);
                }
                data.add("entries",entries);
                result.put(new ResourceLocation("thaumicbases","item_aspects/zz_optional_nether.json"),data.toString().getBytes(StandardCharsets.UTF_8));
            }
            return Collections.unmodifiableMap(result);
        } catch(IOException failure) {throw new IllegalStateException("Cannot read configured addon data",failure);}
    }
    private static void quantity(JsonArray values,String aspect,int amount) {
        var row=new JsonObject();row.addProperty("aspect",aspect);row.addProperty("amount",amount);values.add(row);
    }
    private static void brightCosts(JsonObject aspects) {aspects.addProperty("auram",64);aspects.addProperty("lux",16);aspects.addProperty("potentia",32);}
    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA)return;
        String id="thaumicbases_configured_data";
        Pack.ResourcesSupplier supplier=ConfiguredDataPack::new;
        var info=Objects.requireNonNull(Pack.readPackInfo(id,supplier));
        var pack=Pack.create(id,Component.literal("Thaumic Bases configured recipes"),true,supplier,info,PackType.SERVER_DATA,Pack.Position.BOTTOM,true,PackSource.BUILT_IN);
        event.addRepositorySource(consumer->consumer.accept(pack));
    }
    @Override public IoSupplier<InputStream> getRootResource(String... path) {
        if(path.length!=1 || !path[0].equals("pack.mcmeta"))return null;
        return ()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"Thaumic Bases configured recipes\"}}".getBytes(StandardCharsets.UTF_8));
    }
    @Override public IoSupplier<InputStream> getResource(PackType type,ResourceLocation location) {
        byte[] bytes=type==PackType.SERVER_DATA?files.get(location):null;return bytes==null?null:()->new ByteArrayInputStream(bytes);
    }
    @Override public void listResources(PackType type,String namespace,String prefix,ResourceOutput output) {
        if(type!=PackType.SERVER_DATA)return;
        files.forEach((key,bytes)->{if(key.getNamespace().equals(namespace) && (prefix.isEmpty() || key.getPath().startsWith(prefix+"/")))output.accept(key,()->new ByteArrayInputStream(bytes));});
    }
    @Override public Set<String> getNamespaces(PackType type) {return type==PackType.SERVER_DATA?Set.of("thaumicbases"):Set.of();}
    @Override public void close() {}
}

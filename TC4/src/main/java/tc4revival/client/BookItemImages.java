package tc4revival.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Resolve original standalone item thumbnails to real registered items using their modern model declarations. */
public final class BookItemImages {
    private record Index(Map<String,String> items,Map<String,JsonArray> rows) {}
    private static volatile Index index=new Index(Map.of(),Map.of());
    private BookItemImages() {}
    public static Optional<String> find(String texture) {
        return Optional.ofNullable(index.items().get(texture));
    }
    /** Build off the render thread once per resource reload; opening a book only reads this index. */
    public static net.minecraft.server.packs.resources.PreparableReloadListener reloadListener() {
        return new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Index>() {
            @Override protected Index prepare(ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler) {
                return build(resources);
            }
            @Override protected void apply(Index prepared,ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler) {
                index=prepared;
            }
        };
    }
    private static Index build(ResourceManager current) {
            var result=new TreeMap<String,String>();
            current.listResources("models/item",id->id.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
                String path=entry.getKey().getPath();String item=entry.getKey().getNamespace()+":"+path.substring("models/item/".length(),path.length()-5);
                if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(item)))return;
                try(var reader=entry.getValue().openAsReader()) {
                    var json=JsonParser.parseReader(reader).getAsJsonObject();
                    if(!json.has("textures"))return;
                    var textures=json.getAsJsonObject("textures");
                    if(!textures.has("layer0"))return;
                    String layer=textures.get("layer0").getAsString();
                    if(layer.startsWith("#"))return;
                    var id=ResourceLocation.parse(layer);
                    result.putIfAbsent(id.getNamespace()+":textures/"+id.getPath()+".png",item);
                }catch(Exception failure){tc4revival.TC4Revival.LOGGER.debug("Cannot resolve book thumbnail {}",entry.getKey(),failure);}
            });
            // Original text refers to source paths (often textures/items), while modern
            // models use their mapped targets (textures/item). Follow the resource
            // declarations instead of guessing an item from the old filename.
            try {
                addOriginalAliases(result,tc4revival.resources.OriginalResources.mappings());
                for(var requirement:tc4revival.resources.AddonResourceRequirements.installed())
                    addOriginalAliases(result,requirement.mapping());
            }catch(java.io.IOException failure){tc4revival.TC4Revival.LOGGER.warn("Cannot resolve original book item thumbnails",failure);}
            var atlasRows=new TreeMap<String,JsonArray>();
            current.listResources("book/item_images",id->id.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
                try(var reader=entry.getValue().openAsReader()) {
                    for(var element:JsonParser.parseReader(reader).getAsJsonArray()) {
                        var row=element.getAsJsonObject();var stacks=row.getAsJsonArray("items");
                        if(stacks.isEmpty()||stacks.size()>64)throw new IllegalArgumentException("Invalid book item row");
                        atlasRows.put(row.get("image").getAsString()+":"+row.get("u").getAsInt()+":"+row.get("v").getAsInt()+":"+row.get("width").getAsInt()+":"+row.get("height").getAsInt(),stacks.deepCopy());
                    }
                }catch(Exception failure){tc4revival.TC4Revival.LOGGER.warn("Cannot load book item image mapping {}",entry.getKey(),failure);}
            });
            // Original GUI atlas cells are not standalone item textures and deliberately remain illustrations.
            return new Index(Map.copyOf(result),Map.copyOf(atlasRows));
    }
    public static Optional<JsonArray> find(tc4revival.research.ResearchText.Picture picture) {
        var standalone=find(picture.texture());
        var row=index.rows().get(picture.texture()+":"+picture.u()+":"+picture.v()+":"+picture.width()+":"+picture.height());
        if(row!=null)return Optional.of(row.deepCopy());
        if(picture.u()!=0||picture.v()!=0||standalone.isEmpty())return Optional.empty();
        var result=new JsonArray();result.add(standalone.get());return Optional.of(result);
    }
    private static void addOriginalAliases(Map<String,String> result,Map<String,String> mapping) {
        mapping.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            String item=result.get(resourceId(entry.getKey()));
            if(item!=null)result.putIfAbsent(resourceId(entry.getValue()),item);
        });
    }
    private static String resourceId(String assetPath) {
        String path=assetPath.substring("assets/".length());int slash=path.indexOf('/');
        return path.substring(0,slash)+":"+path.substring(slash+1);
    }
}

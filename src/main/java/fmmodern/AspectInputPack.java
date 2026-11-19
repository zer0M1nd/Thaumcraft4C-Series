package fmmodern;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.registries.ForgeRegistries;

/** Builtin declarative inputs, selected using the same conditions as their gameplay recipes.
 * A higher-priority user datapack can replace the resource normally. */
public final class AspectInputPack extends PathPackResources {
    public AspectInputPack(String id,Path root){super(id,root,true);}
    @Override public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id){return select(id,super.getResource(type,id));}
    @Override public void listResources(PackType type,String namespace,String prefix,PackResources.ResourceOutput output){
        super.listResources(type,namespace,prefix,(id,supplier)->output.accept(id,select(id,supplier)));
    }
    private static net.minecraft.server.packs.resources.PreparableReloadListener inputReload;
    public static void captureLoader(net.minecraftforge.event.AddReloadListenerEvent event){
        // Use the public loader factory, not reflection or a second implementation of the engine.
        var holder=new net.minecraftforge.event.AddReloadListenerEvent(event.getServerResources(),event.getRegistryAccess());
        tc4revival.aspect.recipe.RecipeAspects.addReloadListener(holder);
        inputReload=holder.getListeners().get(0);
    }
    public static void worldReady(net.minecraftforge.event.server.ServerAboutToStartEvent event){
        // World research difficulty is unavailable at the initial datapack preparation stage.
        // Re-read calculation inputs once after SERVER config loads, as we do for crystalwell itself.
        if(inputReload==null)throw new IllegalStateException("Missing recipe aspect loader");
        var barrier=new net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier(){
            public <T> java.util.concurrent.CompletableFuture<T> wait(T value){return java.util.concurrent.CompletableFuture.completedFuture(value);}
        };
        inputReload.reload(barrier,event.getServer().getResourceManager(),net.minecraft.util.profiling.InactiveProfiler.INSTANCE,
            net.minecraft.util.profiling.InactiveProfiler.INSTANCE,Runnable::run,Runnable::run).join();
    }
    private static IoSupplier<InputStream> select(ResourceLocation id,IoSupplier<InputStream> supplier){
        if(supplier==null||!id.getPath().startsWith("recipe_aspect_inputs/"))return supplier;
        return ()->{
            JsonObject root;
            try(var reader=new InputStreamReader(supplier.get(),StandardCharsets.UTF_8)){root=JsonParser.parseReader(reader).getAsJsonObject();}
            var selected=new JsonArray();
            for(var element:root.getAsJsonArray("recipes")){
                var recipe=element.getAsJsonObject();boolean enabled=true;
                if(recipe.has("conditions"))for(var condition:recipe.getAsJsonArray("conditions")){
                    var c=condition.getAsJsonObject();String type=c.get("type").getAsString();
                    enabled &= switch(type){
                        case "forbidden_magic:config" -> RecipeConfigCondition.value(c.get("option").getAsString())==(!c.has("expected")||c.get("expected").getAsBoolean());
                        case "forge:mod_loaded" -> net.minecraftforge.fml.ModList.get().isLoaded(c.get("modid").getAsString());
                        default -> throw new IllegalArgumentException("Unsupported aspect input condition "+type);
                    };
                }
                if(enabled){recipe.remove("conditions");selected.add(recipe);}
            }
            root.add("recipes",selected);
            var items=new JsonArray();
            for(var element:root.getAsJsonArray("items")){
                var row=element.getAsJsonObject();var key=ResourceLocation.parse(row.get("item").getAsString());
                if(!ForgeRegistries.ITEMS.containsKey(key))continue;
                var item=ForgeRegistries.ITEMS.getValue(key);row.addProperty("damageable",item.canBeDepleted());
                if(item.hasCraftingRemainingItem())row.addProperty("container",ForgeRegistries.ITEMS.getKey(item.getCraftingRemainingItem()).toString());
                items.add(row);
            }
            root.add("items",items);
            return new ByteArrayInputStream(root.toString().getBytes(StandardCharsets.UTF_8));
        };
    }
}

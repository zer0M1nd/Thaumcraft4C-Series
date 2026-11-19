package tc4revival.aspect.editor;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;

public class EditorResources {
    private static volatile EditorDefinitions server=EditorDefinitions.EMPTY;
    public static EditorDefinitions server(){return server;}
    public static void clear(){server=EditorDefinitions.EMPTY;}
    public static EditorDefinitions load(ResourceManager manager){
        var files=new TreeMap<ResourceLocation,JsonElement>();
        manager.listResources("aspect_editor",id->id.getPath().endsWith(".json")).forEach((id,resource)->{
            try(var reader=resource.openAsReader()){
                String path=id.getPath();files.put(new ResourceLocation(id.getNamespace(),path.substring(14,path.length()-5)),JsonParser.parseReader(reader));
            }catch(IOException error){throw new IllegalArgumentException("Cannot load aspect editor resource "+id,error);}
        });
        var definitions=EditorDefinitions.merge(files.values()).filtered(TCAspects.catalog(),tc4revival.aspect.item.AspectRegistryTargets::editor);validate(definitions);server=definitions;
        tc4revival.aspect.sync.ServerAspectData.capture("aspect_editor",files);return definitions;
    }
    public static void validate(EditorDefinitions definitions){
        definitions.validateAspects(TCAspects.catalog());
        for(String key:definitions.entries().keySet()){
            int separator=key.indexOf(':');String kind=key.substring(0,separator);var id=new ResourceLocation(key.substring(separator+1));
            if(kind.equals("item")&&(!ForgeRegistries.ITEMS.containsKey(id)||id.toString().equals("minecraft:air")))throw new IllegalArgumentException("Unknown item "+id);
            if(kind.equals("block")&&!ForgeRegistries.BLOCKS.containsKey(id))throw new IllegalArgumentException("Unknown block "+id);
            if(kind.equals("entity")&&!ForgeRegistries.ENTITY_TYPES.containsKey(id))throw new IllegalArgumentException("Unknown entity "+id);
        }
    }
    public static Optional<AspectAmounts> block(BlockState state){
        var id=ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return server.resolve("block",id,tag->state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,new ResourceLocation(tag))))
                .map(v->EditorDefinitions.amounts(v,TCAspects.catalog()));
    }
    public static Optional<AspectAmounts> entity(net.minecraft.world.entity.EntityType<?> type){
        return server.get("entity:"+ForgeRegistries.ENTITY_TYPES.getKey(type)).map(v->EditorDefinitions.amounts(v,TCAspects.catalog()));
    }
}

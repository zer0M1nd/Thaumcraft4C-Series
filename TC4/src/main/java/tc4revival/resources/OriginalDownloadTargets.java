package tc4revival.resources;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Discovers installed addons without linking to or changing their classes. */
public class OriginalDownloadTargets {
    static OriginalResourceDownloads.Target base(Map<String,String> mapping) {
        return new OriginalResourceDownloads.Target("thaumcraft", "Thaumcraft 4.2.3.5", "Thaumcraft",
                OriginalResources.REFERENCE_SHA256, "https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2227552",
                mapping, true, false);
    }

    public static List<OriginalResourceDownloads.Target> installed() throws IOException {
        var result = new ArrayList<OriginalResourceDownloads.Target>();
        result.add(base(OriginalResources.mappings()));
        for (var mod : net.minecraftforge.fml.ModList.get().getMods()) {
            var file = mod.getOwningFile().getFile();
            var declaration=file.findResource("data",mod.getModId(),"thaumcraft","original_resources.json");
            if(Files.isRegularFile(declaration)) {
                result.addAll(addons(mod.getModId(),declaration));
                continue;
            }

        }
        return List.copyOf(result);
    }
    static List<OriginalResourceDownloads.Target> addons(String owner,Path declaration) throws IOException {
        var requirements=AddonResourceRequirements.decode(owner,read(declaration));
        var result=new ArrayList<OriginalResourceDownloads.Target>();
        for(int index=0;index<requirements.size();index++) {
            var source=requirements.get(index);
            String id=index==0&&!owner.equals("thaumcraft")?owner:owner+"_"+source.minecraftVersion().replace('.','_')+"_"+index+"_"+source.sha256().substring(0,12);
            result.add(new OriginalResourceDownloads.Target(id,source.name(),source.legacyModId(),source.sha256(),source.download(),source.mapping(),false,false,source.minecraftVersion(),source.downloads(),source.requiredResources(),source.annotationIdentity(),source.sources()));
        }
        return List.copyOf(result);
    }
    static OriginalResourceDownloads.Target addon(String owner,Path declaration,Path unused) throws IOException {
        return Files.isRegularFile(declaration)?addons(owner,declaration).get(0):null;
    }
    private static JsonObject read(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
        catch (RuntimeException error) { throw new IOException("Invalid original resource declaration " + file, error); }
    }
    private static Map<String,String> mapping(JsonObject json, String owner) throws IOException {
        var result = new TreeMap<String,String>();
        for (var entry : json.getAsJsonObject("mapping").entrySet()) {
            var source = entry.getValue().getAsString();
            if (!OriginalResources.safePath(entry.getKey()) || !OriginalResources.safePath(source)
                    || !entry.getKey().startsWith("assets/" + owner + "/")) throw new IOException("Invalid resource ownership");
            result.put(entry.getKey(), source);
        }
        return result;
    }
}

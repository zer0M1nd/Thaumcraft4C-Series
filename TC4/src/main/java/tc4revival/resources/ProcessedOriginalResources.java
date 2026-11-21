package tc4revival.resources;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Validated, data-only original resources. The manifest is written last, then the directory is swapped.
 * Every startup checks the bytes, including optional animation metadata retained at extraction time.
 */
public final class ProcessedOriginalResources {
    public static final String MANIFEST=".original-resources.json";
    static boolean logoChecked(Path directory) throws IOException {
        var manifest = JsonParser.parseString(Files.readString(directory.resolve(MANIFEST))).getAsJsonObject();
        return manifest.has("logo_checked") && manifest.get("logo_checked").getAsBoolean();
    }
    private static final int MAX_FILE=32*1024*1024;
    private static final long MAX_TOTAL=256L*1024*1024;
    public static String directoryName(String modId,String minecraftVersion) {
        // Legacy Forge allowed qualified identities, e.g. flammpfeil.slashblade.
        // Keep dots verbatim so these never collide with underscore identities.
        if(!modId.matches("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*")||!minecraftVersion.matches("[0-9]+(?:\\.[0-9]+)*"))throw new IllegalArgumentException("Invalid original resource identity");
        return modId.toLowerCase(Locale.ROOT)+"_"+minecraftVersion.replace('.','_');
    }
    public static OriginalResources.Snapshot read(Path directory,Map<String,String> mapping,String modId,String version,boolean baseLanguages) throws IOException {
        try {
            checked(directory,directory.resolve(MANIFEST));
            if(Files.size(directory.resolve(MANIFEST))>4*1024*1024)throw new IOException("Oversized resource manifest");
            var manifest=JsonParser.parseString(Files.readString(directory.resolve(MANIFEST))).getAsJsonObject();
            if(manifest.get("format").getAsInt()!=1||!manifest.get("modid").getAsString().equals(modId)
                    ||!manifest.get("minecraft_version").getAsString().equals(version))throw new IOException("Different original resource identity");
            String sourceHash=manifest.get("source_sha256").getAsString();
            if(!sourceHash.matches("[0-9a-f]{64}"))throw new IOException("Invalid original source hash");
            var entries=manifest.getAsJsonObject("files");
            if(entries.size()>30000)throw new IOException("Too many processed resources");
            var found=new TreeMap<String,byte[]>();long total=0;
            for(var entry:entries.entrySet()) {
                String path=entry.getKey();
                if((!OriginalResources.safePath(path) && !path.equals(LegacyModLogo.CACHE_FILE)) || path.endsWith(".class"))throw new IOException("Invalid processed path: "+path);
                Path file=directory.resolve(path);checked(directory,file);
                long size=Files.size(file); total+=size;
                if(size>MAX_FILE||total>MAX_TOTAL)throw new IOException("Processed resource size limit exceeded");
                byte[] bytes;
                try(var input=Files.newInputStream(file)){bytes=input.readNBytes(MAX_FILE+1);}
                if(bytes.length>MAX_FILE||!OriginalResources.sha256(bytes).equals(entry.getValue().getAsString()))throw new IOException("Damaged resource: "+path);
                found.put(path,bytes);
            }
            var wanted=new HashSet<>(mapping.values());
            if(baseLanguages){wanted.add("assets/thaumcraft/lang/en_US.lang");wanted.add("assets/thaumcraft/lang/zh_CN.lang");wanted.add("assets/thaumcraft/lang/zh_TW.lang");}
            for(String path:wanted)if(!found.containsKey(path))throw new IOException("Missing required resource: "+path);
            return new OriginalResources.Snapshot(directory.toAbsolutePath().normalize(),sourceHash,Collections.unmodifiableMap(found));
        }catch(IllegalStateException|IllegalArgumentException|NullPointerException failure){throw new IOException("Invalid processed resource manifest: "+directory,failure);}
    }
    private static void checked(Path root,Path file) throws IOException {
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(root))throw new IOException("Invalid processed directory");
        for(Path p=file;!p.equals(root);p=p.getParent())if(p==null||Files.isSymbolicLink(p))throw new IOException("Linked processed resource");
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Missing processed resource: "+file);
    }
    public static Path store(Path game,OriginalResourceDownloads.Target target,OriginalResources.Snapshot source) throws IOException {
        Path root=game.resolve("thaumcraft").toAbsolutePath().normalize();Files.createDirectories(root);
        Path destination=root.resolve(directoryName(target.legacyModId(),target.minecraftVersion()));
        Path staging=Files.createTempDirectory(root,".extract-");Path previous=null;
        try {
            var manifest=new JsonObject();manifest.addProperty("format",1);manifest.addProperty("modid",target.legacyModId());
            manifest.addProperty("minecraft_version",target.minecraftVersion());manifest.addProperty("source_sha256",source.sha256());
            var hashes=new JsonObject();
            for(var entry:new TreeMap<>(source.resources()).entrySet()) {
                if((!OriginalResources.safePath(entry.getKey()) && !entry.getKey().equals(LegacyModLogo.CACHE_FILE)) || entry.getKey().endsWith(".class"))continue;
                Path file=staging.resolve(entry.getKey());Files.createDirectories(file.getParent());Files.write(file,entry.getValue());
                hashes.addProperty(entry.getKey(),OriginalResources.sha256(entry.getValue()));
            }
            var selectors=new TreeSet<>(target.requiredResources());selectors.addAll(target.mapping().values());
            manifest.add("requirements",new Gson().toJsonTree(selectors));
            manifest.addProperty("logo_checked", true);
            manifest.add("files",hashes);Files.writeString(staging.resolve(MANIFEST),new GsonBuilder().setPrettyPrinting().create().toJson(manifest),StandardCharsets.UTF_8);
            target.read(staging); // Check the complete requirement union before replacing any data.
            if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS)) {
                if(Files.isSymbolicLink(destination)||!Files.isDirectory(destination,LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid destination: "+destination);
                previous=root.resolve(".previous-"+UUID.randomUUID());Files.move(destination,previous);
            }
            try {
                try { Files.move(staging,destination,StandardCopyOption.ATOMIC_MOVE); }
                catch(AtomicMoveNotSupportedException unsupported){Files.move(staging,destination);}
            }
            catch(IOException failure){if(previous!=null&&!Files.exists(destination))Files.move(previous,destination);throw failure;}
            if(previous!=null)deleteTree(previous);
            // Delete only the validated original inside the resource directory, and only if unchanged.
            Path original=source.file().toAbsolutePath().normalize();
            if(original.getParent().equals(root)&&Files.isRegularFile(original,LinkOption.NOFOLLOW_LINKS)
                    &&original.toString().toLowerCase(Locale.ROOT).endsWith(".jar")&&Files.size(original)<=OriginalResources.MAX_JAR_BYTES
                    &&OriginalResources.sha256(Files.readAllBytes(original)).equals(source.sha256()))Files.delete(original);
            return destination;
        }finally{if(Files.exists(staging,LinkOption.NOFOLLOW_LINKS))deleteTree(staging);}
    }
    private static void deleteTree(Path directory) throws IOException {
        // No FOLLOW_LINKS: even a hostile cached symlink is removed as a link, never traversed.
        try(var paths=Files.walk(directory)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
    }
    private ProcessedOriginalResources() {}
}

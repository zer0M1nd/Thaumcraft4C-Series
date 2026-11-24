package tc4revival.resources;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** One downloader for the core and every addon. Original archives are never executable mods. */
public class OriginalResourceDownloads {
    public record Target(String owner,String name,String legacyModId,String sha256,String page,
                         Map<String,String> mapping,boolean baseLanguages,boolean mappedOnly,String minecraftVersion,
                         List<String> downloads,List<String> requiredResources,OriginalResources.AnnotationIdentity annotationIdentity, ArchiveSources sources) {
        public Target(String owner,String name,String legacyModId,String sha256,String page,Map<String,String> mapping,boolean baseLanguages,boolean mappedOnly,String minecraftVersion,List<String> downloads,List<String> requiredResources,OriginalResources.AnnotationIdentity identity) {
            this(owner,name,legacyModId,sha256,page,mapping,baseLanguages,mappedOnly,minecraftVersion,downloads,requiredResources,identity,ArchiveSources.legacy(sha256,page,downloads));
        }
        boolean accepts(String digest) { return sha256.equals(digest) || sources.hashes().containsValue(digest); }
        public Target(String owner,String name,String legacyModId,String sha256,String page,Map<String,String> mapping,boolean baseLanguages,boolean mappedOnly,String minecraftVersion,List<String> downloads,List<String> requiredResources) {
            this(owner,name,legacyModId,sha256,page,mapping,baseLanguages,mappedOnly,minecraftVersion,downloads,requiredResources,null);
        }
        public Target(String owner,String name,String legacyModId,String sha256,String page,Map<String,String> mapping,boolean baseLanguages,boolean mappedOnly) {
            this(owner,name,legacyModId,sha256,page,mapping,baseLanguages,mappedOnly,"1.7.10");
        }
        public Target(String owner,String name,String legacyModId,String sha256,String page,Map<String,String> mapping,boolean baseLanguages,boolean mappedOnly,String version) {
            this(owner,name,legacyModId,sha256,page,mapping,baseLanguages,mappedOnly,version,List.of(page),List.copyOf(mapping.values()));
        }
        public Target {
            sources = sources.withCatalog(sha256, page);
            mapping=Map.copyOf(mapping);downloads=List.copyOf(downloads);requiredResources=List.copyOf(requiredResources);
            if(!owner.matches("[a-z0-9_-]+"))throw new IllegalArgumentException("Invalid owner");
            ProcessedOriginalResources.directoryName(legacyModId,minecraftVersion);
            for(String path:requiredResources)if(!OriginalResources.safePath(path.endsWith("/")?path.substring(0,path.length()-1):path)||path.endsWith(".class"))
                throw new IllegalArgumentException("Invalid required resource: "+path);
        }
        OriginalResources.Snapshot read(Path file) throws IOException {
            var expanded=new TreeMap<>(mapping);
            var prefixes=requiredResources.stream().filter(p->p.endsWith("/")).toList();
            for(String path:requiredResources)if(!path.endsWith("/"))expanded.put(path,path);
            if(!prefixes.isEmpty()) {
                Set<String> paths;
                if(Files.isDirectory(file,LinkOption.NOFOLLOW_LINKS)) {
                    paths=ProcessedOriginalResources.read(file,mapping,legacyModId,minecraftVersion,baseLanguages).resources().keySet();
                    var manifest=com.google.gson.JsonParser.parseString(Files.readString(file.resolve(ProcessedOriginalResources.MANIFEST))).getAsJsonObject();
                    var covered=new HashSet<String>();
                    if(manifest.has("requirements"))manifest.getAsJsonArray("requirements").forEach(e->covered.add(e.getAsString()));
                    if(!covered.containsAll(prefixes))throw new IOException("New required resource directory; re-extraction required");
                }
                else try(var zip=new java.util.zip.ZipFile(file.toFile())){paths=zip.stream().filter(e->!e.isDirectory()).map(java.util.zip.ZipEntry::getName).collect(java.util.stream.Collectors.toSet());}
                for(String prefix:prefixes) {
                    var selected=paths.stream().filter(p->p.startsWith(prefix)&&OriginalResources.safePath(p)&&!p.endsWith(".class")).toList();
                    if(selected.isEmpty())throw new IOException("Missing required directory: "+prefix);
                    selected.forEach(p->expanded.put(p,p));
                }
            }
            return OriginalResources.read(file,expanded,legacyModId,minecraftVersion,baseLanguages,annotationIdentity);
        }
    }
    public record Result(String owner,Path downloaded,String warning,String failure) {}
    public static List<Result> obtainMissing(Path game,List<Target> targets,Consumer<String> progress) {
        var results=new ArrayList<Result>();
        for (var target:grouped(targets)) try {
            var task=new OriginalArchiveTask(game,target);
            try (var local=task.local()) {
                if(local==null) results.add(new Result(target.owner(),null,"","Waiting for the startup download window: "+target.name()));
                else { String warning=local.apply(); if(!warning.isBlank()) results.add(new Result(target.owner(),game.resolve("thaumcraft").resolve(task.item().id()),warning,"")); }
            }
        } catch(IOException e) { results.add(new Result(target.owner(),null,"",e.getMessage())); }
        return List.copyOf(results);
    }
    /** Union all owners before extraction, so none loses resources when the original JAR is deleted. */
    static List<Target> grouped(List<Target> targets) {
        var grouped=new LinkedHashMap<String,Target>();
        for(var target:targets) {
            String key=ProcessedOriginalResources.directoryName(target.legacyModId(),target.minecraftVersion());
            var old=grouped.get(key);
            if(old==null){grouped.put(key,target);continue;}
            if(!old.legacyModId().equals(target.legacyModId())) throw new IllegalArgumentException("Conflicting original identities: "+key);
            if(old.annotationIdentity()!=null&&target.annotationIdentity()!=null&&(!old.annotationIdentity().classFile().equals(target.annotationIdentity().classFile()) || !old.annotationIdentity().version().equals(target.annotationIdentity().version())))throw new IllegalArgumentException("Conflicting original annotation identities: "+key);
            var paths=new LinkedHashSet<>(old.requiredResources());paths.addAll(old.mapping().values());paths.addAll(target.requiredResources());paths.addAll(target.mapping().values());
            var urls=new LinkedHashSet<>(old.downloads());urls.addAll(target.downloads());
            grouped.put(key,new Target(old.owner(),old.name(),old.legacyModId(),old.sha256(),old.page(),old.mapping(),old.baseLanguages()||target.baseLanguages(),false,old.minecraftVersion(),List.copyOf(urls),List.copyOf(paths),old.annotationIdentity()!=null?old.annotationIdentity():target.annotationIdentity(),old.sources().merge(target.sources())));
        }
        return List.copyOf(grouped.values());
    }
}

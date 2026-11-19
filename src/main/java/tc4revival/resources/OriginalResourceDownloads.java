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
                         List<String> downloads,List<String> requiredResources,OriginalResources.AnnotationIdentity annotationIdentity) {
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
    @FunctionalInterface interface Transfer { void fetch(URI url,Path target) throws IOException; }
    public static List<Result> obtainMissing(Path game,List<Target> targets,Consumer<String> progress) {
        return obtainMissing(game,targets,progress,CurseForgeDownload::fetch);
    }
    /** Union all owners before extraction, so none loses resources when the original JAR is deleted. */
    static List<Target> grouped(List<Target> targets) {
        var grouped=new LinkedHashMap<String,Target>();
        for(var target:targets) {
            String key=ProcessedOriginalResources.directoryName(target.legacyModId(),target.minecraftVersion());
            var old=grouped.get(key);
            if(old==null){grouped.put(key,target);continue;}
            if(!old.sha256().equals(target.sha256())||!old.legacyModId().equals(target.legacyModId()))throw new IllegalArgumentException("Conflicting original versions: "+key);
            if(old.annotationIdentity()!=null&&target.annotationIdentity()!=null&&!old.annotationIdentity().equals(target.annotationIdentity()))throw new IllegalArgumentException("Conflicting original annotation identities: "+key);
            var paths=new LinkedHashSet<>(old.requiredResources());paths.addAll(old.mapping().values());paths.addAll(target.requiredResources());paths.addAll(target.mapping().values());
            var urls=new LinkedHashSet<>(old.downloads());urls.addAll(target.downloads());
            grouped.put(key,new Target(old.owner(),old.name(),old.legacyModId(),old.sha256(),old.page(),old.mapping(),old.baseLanguages()||target.baseLanguages(),false,old.minecraftVersion(),List.copyOf(urls),List.copyOf(paths),old.annotationIdentity()!=null?old.annotationIdentity():target.annotationIdentity()));
        }
        return List.copyOf(grouped.values());
    }
    static List<Result> obtainMissing(Path game,List<Target> targets,Consumer<String> progress,Transfer transfer) {
        return obtainMissing(game, targets, Set.of(), progress, transfer);
    }
    static List<Result> obtainMissing(Path game,List<Target> targets,Set<String> logoSources,Consumer<String> progress,Transfer transfer) {
        var results=new ArrayList<Result>();
        for(var target:grouped(targets)) {
            Path temporary=null;
            OriginalResources.Snapshot previous=null;
            try {
                var directory=game.resolve("thaumcraft");
                OriginalResources.Snapshot selected=null;
                for(var candidate:OriginalResources.candidates(directory)) {
                    try {
                        var snapshot=target.read(candidate);
                        if (Files.isDirectory(candidate) && logoSources.contains(ProcessedOriginalResources.directoryName(target.legacyModId(), target.minecraftVersion()))
                                && !ProcessedOriginalResources.logoChecked(candidate)) {
                            previous = snapshot;
                            continue;
                        }
                        if(selected==null||snapshot.sha256().equals(target.sha256()))selected=snapshot;
                        if(snapshot.sha256().equals(target.sha256()))break;
                    }catch(IOException ignored){ /* Unrelated or incomplete originals are not candidates. */ }
                }
                if (previous != null && selected == null && !previous.sha256().equals(target.sha256())) {
                    results.add(new Result(target.owner(),previous.file(),"Supply the same original JAR to recover its optional logo.",""));
                    continue;
                }
                if(selected!=null) {
                    Path prepared=selected.file();
                    if(!Files.isDirectory(prepared))prepared=ProcessedOriginalResources.store(game,target,selected);
                    if(!selected.sha256().equals(target.sha256()))results.add(new Result(target.owner(),prepared,"SHA-256 differs from reference: "+selected.sha256(),""));
                    continue;
                }
                progress.accept("Downloading legacy resources: "+target.name());
                temporary=Files.createTempFile(directory,".tc4-download-",".part");
                var failures=new ArrayList<String>();
                for(URI url:CurseForgeDownload.attempts(target.downloads())) {
                    try {
                        transfer.fetch(url,temporary);
                        var snapshot=target.read(temporary);
                        if (previous != null && !snapshot.sha256().equals(previous.sha256()))
                            throw new IOException("Downloaded archive differs from the existing original resource cache");
                        selected=snapshot;break;
                    }catch(IOException|RuntimeException failure){failures.add(url+": "+failure.getMessage());progress.accept("Original resource source failed: "+url.getHost());}
                }
                if(selected==null)throw new IOException(String.join("\n",failures));
                Path prepared=ProcessedOriginalResources.store(game,target,selected);
                String warning=selected.sha256().equals(target.sha256())?"":"SHA-256 differs from reference: "+selected.sha256();
                results.add(new Result(target.owner(),prepared,warning,""));
            }catch(IOException|RuntimeException failure) {
                if (previous != null) results.add(new Result(target.owner(),previous.file(),"Optional logo unavailable: "+failure.getMessage(),""));
                else results.add(new Result(target.owner(),null,"",target.name()+": "+failure.getMessage()+"\n"+target.page()));
            }finally {
                if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){ /* Never treated as a complete original. */ }
            }
        }
        return List.copyOf(results);
    }
}

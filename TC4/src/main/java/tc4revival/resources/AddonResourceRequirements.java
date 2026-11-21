package tc4revival.resources;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.fml.ModList;

/** Resource declarations from installed addons; reference archives are data, never executable mods. */
public  class AddonResourceRequirements {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public record Requirement(String owner,String name,String legacyModId,String sha256,String download,
                              Map<String,String> mapping,Map<String,Map<String,String>> languages,String minecraftVersion, List<String> downloads,List<String> requiredResources,Set<String> namespaceAliases,OriginalResources.AnnotationIdentity annotationIdentity) {
        public Requirement { namespaceAliases=Set.copyOf(namespaceAliases); }
        public Requirement(String owner,String name,String legacyModId,String sha256,String download,Map<String,String> mapping,Map<String,Map<String,String>> languages,String minecraftVersion,List<String> downloads,List<String> requiredResources,Set<String> namespaceAliases){
            this(owner,name,legacyModId,sha256,download,mapping,languages,minecraftVersion,downloads,requiredResources,namespaceAliases,null);
        }
        public Requirement(String owner,String name,String legacyModId,String sha256,String download,Map<String,String> mapping,Map<String,Map<String,String>> languages,String minecraftVersion,List<String> downloads,List<String> requiredResources){
            this(owner,name,legacyModId,sha256,download,mapping,languages,minecraftVersion,downloads,requiredResources,Set.of());
        }
        public boolean owns(String path){return ownsPath(owner,namespaceAliases,path);}
        public Requirement(String owner,String name,String legacyModId,String sha256,String download,Map<String,String> mapping,Map<String,Map<String,String>> languages,String minecraftVersion){
            this(owner,name,legacyModId,sha256,download,mapping,languages,minecraftVersion,List.of(download),List.copyOf(mapping.values()));
        }
        public Requirement(String owner,String name,String legacyModId,String sha256,String download,Map<String,String> mapping,Map<String,Map<String,String>> languages){
            this(owner,name,legacyModId,sha256,download,mapping,languages,"1.7.10");
        }
    }
    public record Resolved(Requirement requirement,OriginalResources.Snapshot source) {}
    public record Scan(List<Resolved> resolved,List<String> missing,List<String> warnings) {}
    public record Prepared(String owner,Path directory,Set<String> allowed) {
        /** Multiple archives owned by one addon must not replace each other's repository entry. */
        public String packId() { return owner+"/"+directory.getFileName(); }
    }
    private AddonResourceRequirements() {}
    public static List<Requirement> installed() throws IOException {
        var result=new ArrayList<Requirement>();
        for(var mod:ModList.get().getMods()) {
            var path=mod.getOwningFile().getFile().findResource("data",mod.getModId(),"thaumcraft","original_resources.json");
            if(!Files.isRegularFile(path))continue;
            try(var reader=Files.newBufferedReader(path)) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                result.addAll(decode(mod.getModId(),json));
            }catch(RuntimeException error){throw new IOException("Invalid addon original resource declaration: "+path,error);}
        }
        result.sort(Comparator.comparing(Requirement::owner));return List.copyOf(result);
    }
    /** One owner can declare additional original archives without installing additional modern mods. */
    public static List<Requirement> decode(String owner,JsonObject root) throws IOException {
        var sources=new ArrayList<JsonObject>();sources.add(root);
        if(root.has("additional_sources"))for(var source:root.getAsJsonArray("additional_sources"))sources.add(source.getAsJsonObject());
        var result=new ArrayList<Requirement>();var targets=new HashSet<String>();
        var languageKeys=new HashMap<String,Set<String>>();
        for(var json:sources) {
                var aliases=new HashSet<>(strings(json,"resource_namespace_aliases",List.of()));
                for(var alias:aliases)if(!alias.matches("[a-z0-9_.-]+")||alias.equals("minecraft")||alias.equals("thaumcraft"))
                    throw new IOException("Invalid addon resource migration namespace: "+alias);
                var mapping=new TreeMap<String,String>();
                json.getAsJsonObject("mapping").entrySet().forEach(e->mapping.put(e.getKey(),e.getValue().getAsString()));
                var languages=new TreeMap<String,Map<String,String>>();
                if(json.has("languages"))json.getAsJsonObject("languages").entrySet().forEach(e->{
                    var keys=new TreeMap<String,String>();e.getValue().getAsJsonObject().entrySet().forEach(k->keys.put(k.getKey(),k.getValue().getAsString()));
                    languages.put(e.getKey(),Map.copyOf(keys));
                });
                for(var e:mapping.entrySet()) {
                    if(!OriginalResources.safePath(e.getKey()) || !OriginalResources.safePath(e.getValue())
                            || !ownsPath(owner,aliases,e.getKey()))throw new IOException("Invalid addon resource ownership: "+e);
                    boolean language=languages.containsKey(e.getKey())&&e.getKey().matches("assets/[^/]+/lang/[a-z_]+\\.json");
                    if(!targets.add(e.getKey())&&(!language||!languageKeys.containsKey(e.getKey())))
                        throw new IOException("Ambiguous resource in multiple original archives: "+e.getKey());
                    if(language) {
                        var keys=languageKeys.computeIfAbsent(e.getKey(),unused->new HashSet<>());
                        for(String key:languages.get(e.getKey()).keySet())if(!keys.add(key))
                            throw new IOException("Ambiguous translation in multiple original archives: "+key);
                    }
                }
                for(String target:languages.keySet())if(!mapping.containsKey(target)||!target.endsWith(".json"))throw new IOException("Language mapping missing: "+target);
                String hash=json.get("sha256").getAsString();
                if(!hash.matches("[0-9a-f]{64}"))throw new IOException("Expected SHA-256 for "+owner);
                OriginalResources.AnnotationIdentity identity=null;
                if(json.has("legacy_annotation")) {
                    try {
                        var annotation=json.getAsJsonObject("legacy_annotation");
                        if(!annotation.keySet().equals(Set.of("class","version")))throw new IllegalArgumentException("Expected class and version");
                        identity=new OriginalResources.AnnotationIdentity(annotation.get("class").getAsString(),annotation.get("version").getAsString(),hash);
                    }catch(RuntimeException invalid){throw new IOException("Invalid legacy_annotation for "+owner,invalid);}
                }
                var downloads=strings(json,"downloads",json.has("download")?List.of(json.get("download").getAsString()):List.of());
                if(downloads.isEmpty())throw new IOException("Missing original resource download links");
                String page=json.has("download")?json.get("download").getAsString():downloads.get(0);
                result.add(new Requirement(owner,json.get("name").getAsString(),json.get("legacy_modid").getAsString(),
                    hash,page,Map.copyOf(mapping),Map.copyOf(languages),
                    json.has("legacy_minecraft_version")?json.get("legacy_minecraft_version").getAsString():"1.7.10",
                    downloads,
                    strings(json,"required_resources",List.copyOf(mapping.values())),aliases,identity));
        }
        return List.copyOf(result);
    }
    private static List<String> strings(JsonObject json,String field,List<String> fallback) throws IOException {
        if(!json.has(field))return fallback;
        var values=new ArrayList<String>();
        for(var value:json.getAsJsonArray(field))values.add(value.getAsString());
        if(values.isEmpty())throw new IOException("Empty "+field);
        return List.copyOf(values);
    }
    private static boolean ownsPath(String owner,Set<String> aliases,String path){
        if(path.startsWith("assets/"+owner+"/"))return true;
        for(var alias:aliases)if(path.startsWith("assets/"+alias+"/"))return true;
        return false;
    }
    public static Scan scan(Path game,List<Requirement> requirements) throws IOException {
        var resolved=new ArrayList<Resolved>();var missing=new ArrayList<String>();var warnings=new ArrayList<String>();
        var files=OriginalResources.candidates(game.resolve("thaumcraft"));
        for(var requirement:requirements) {
            var candidates=new ArrayList<OriginalResources.Snapshot>();
            for(var file:files)try {candidates.add(OriginalResources.read(file,requirement.mapping(),requirement.legacyModId(),requirement.minecraftVersion(),false,requirement.annotationIdentity()));}
                catch(IOException ignored) { /* Other mods and incomplete archives are not candidates. */ }
            if(candidates.isEmpty()) {missing.add(requirement.name()+" ("+requirement.legacyModId()+")\n"+requirement.download());continue;}
            var selected=candidates.stream().filter(s->s.sha256().equals(requirement.sha256())).findFirst().orElse(candidates.get(0));
            if(!selected.sha256().equals(requirement.sha256()))warnings.add(requirement.name()+": SHA-256 differs from reference; using "+selected.file().getFileName());
            if(candidates.size()>1)warnings.add(requirement.name()+": multiple matching archives; using "+selected.file().getFileName());
            resolved.add(new Resolved(requirement,selected));
        }
        return new Scan(List.copyOf(resolved),List.copyOf(missing),List.copyOf(warnings));
    }
    public static Prepared prepare(Path game,Resolved resolved) throws IOException {
        return prepare(game,resolved,resources->{});
    }
    public static Prepared prepare(Path game,Resolved resolved,java.util.function.Consumer<Map<String,byte[]>> converter) throws IOException {
        var requirement=resolved.requirement();var output=new TreeMap<String,byte[]>();
        requirement.mapping().forEach((target,source)->output.put(target,resolved.source().resources().get(source)));
        requirement.mapping().forEach((target,source)->{
            byte[] metadata=resolved.source().resources().get(source+".mcmeta");
            if(target.endsWith(".png") && metadata!=null)output.put(target+".mcmeta",metadata);
        });
        // Legacy loaders accepted unquoted animation keys and resolved bare sound names
        // within the owning namespace. Convert these descriptors, never their bitmaps/audio.
        for(var entry:output.entrySet()) {
            String path=entry.getKey();
            if(path.endsWith(".png.mcmeta")) {
                String text=new String(entry.getValue(),StandardCharsets.UTF_8);
                try { net.minecraft.util.GsonHelper.parse(text); }
                catch(JsonParseException legacySyntax) {
                    entry.setValue(new Gson().toJson(JsonParser.parseString(text)).getBytes(StandardCharsets.UTF_8));
                }
            } else if(path.endsWith("/sounds.json")) {
                var sounds=JsonParser.parseString(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
                String namespace=path.split("/")[1];boolean changed=false;
                for(var event:sounds.entrySet()) {
                    var list=event.getValue().getAsJsonObject().getAsJsonArray("sounds");
                    if(list==null)continue;
                    for(int i=0;i<list.size();i++) {
                        var sound=list.get(i);
                        String name=sound.isJsonPrimitive()?sound.getAsString():sound.getAsJsonObject().get("name").getAsString();
                        if(name.contains(":"))continue;
                        if(sound.isJsonPrimitive())list.set(i,new JsonPrimitive(namespace+":"+name));
                        else sound.getAsJsonObject().addProperty("name",namespace+":"+name);
                        changed=true;
                    }
                }
                if(changed)entry.setValue(new Gson().toJson(sounds).getBytes(StandardCharsets.UTF_8));
            }
        }
        var imagePaths = new TreeMap<String,String>();
        requirement.mapping().forEach((target,source)->{
            if(target.startsWith("assets/") && source.startsWith("assets/") && target.endsWith(".png"))
                imagePaths.putIfAbsent(source.substring(7).replaceFirst("/", ":"), target.substring(7).replaceFirst("/", ":"));
        });
        for(var language:requirement.languages().entrySet()) {
            var old=readLanguage(output.get(language.getKey()));
            var englishSource=requirement.mapping().get(language.getKey().replace("/zh_cn.json","/en_us.json"));
            var englishBytes=englishSource==null?null:resolved.source().resources().get(englishSource);
            var english=language.getKey().endsWith("/zh_cn.json")&&englishBytes!=null?readLanguage(englishBytes):Map.<String,String>of();
            var converted=new JsonObject();
            var missing=new ArrayList<String>();
            for(var key:language.getValue().entrySet()) {
                String value=old.get(key.getValue());
                // Some old zh_CN files contain untranslated English placeholders.
                if(value!=null&&value.equals(english.get(key.getValue()))&&value.codePoints().noneMatch(c->Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN))value=null;
                if(value==null) {
                    missing.add(key.getValue());
                    // Minecraft loads en_us before the selected locale. An English value in
                    // zh_cn would hide a real translation supplied by a lower-priority pack.
                    continue;
                }
                converted.addProperty(key.getKey(),tc4revival.research.ResearchText.remapPictures(value,imagePaths));
            }
            if(!missing.isEmpty())LOGGER.debug("{}: {} missing legacy translations in {}; leaving keys to resource-pack/game fallback: {}",
                    requirement.owner(),missing.size(),language.getKey(),missing);
            output.put(language.getKey(),new GsonBuilder().disableHtmlEscaping().create().toJson(converted).getBytes(StandardCharsets.UTF_8));
        }
        converter.accept(output);
        for(String path:output.keySet())if(!OriginalResources.safePath(path)||!requirement.owns(path))throw new IOException("Invalid converted resource ownership: "+path);
        output.put("pack.mcmeta","{\"pack\":{\"pack_format\":15,\"description\":\"Local addon resources\"}}".getBytes(StandardCharsets.UTF_8));
        String key=OriginalResources.sha256((requirement.mapping().toString()+requirement.languages()).getBytes(StandardCharsets.UTF_8));
        Path directory=game.resolve(".thaumcraft-cache").resolve(requirement.owner()+"-"+resolved.source().sha256()+"-"+key.substring(0,16));
        for(var entry:output.entrySet()) {
            Path file=directory.resolve(entry.getKey());Files.createDirectories(file.getParent());
            if(Files.isRegularFile(file)&&Arrays.equals(Files.readAllBytes(file),entry.getValue()))continue;
            Path temporary=Files.createTempFile(file.getParent(),".writing-",".tmp");
            try{Files.write(temporary,entry.getValue());Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}
        }
        return new Prepared(requirement.owner(),directory,Set.copyOf(output.keySet()));
    }
    private static Map<String,String> readLanguage(byte[] bytes) {
        var result=new HashMap<String,String>();
        new String(bytes,StandardCharsets.UTF_8).replace("\uFEFF", "").lines().forEach(line->{
            int equals=line.indexOf('=');
            if(equals>0&&!line.stripLeading().startsWith("#"))result.put(line.substring(0,equals).strip(),line.substring(equals+1).strip());
        });
        return result;
    }
}

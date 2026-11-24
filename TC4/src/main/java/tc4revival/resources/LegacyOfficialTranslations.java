package tc4revival.resources;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.fml.ModList;

/** Missing original-author translations supplied by explicitly pinned legacy-maintainer language files. */
public final class LegacyOfficialTranslations {
    public static final String PACK_ID="thaumcraft_original_translations";
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    public record Source(String owner, URI url, String sha256, Map<String,String> keys,
                         Map<String,String> pictures, List<URI> downloads, ArchiveSources sources) {
        public Source(String owner, URI url, String sha256, Map<String,String> keys, Map<String,String> pictures) {
            this(owner, url, sha256, keys, pictures, List.of(url, mirror(url)),
                    new ArchiveSources(Map.of(url.toString(), sha256, mirror(url).toString(), sha256), List.of(page(url))));
        }
    }
    private static URI mirror(URI raw) {
        String[] path = raw.getRawPath().substring(1).split("/", 4);
        return URI.create("https://cdn.jsdelivr.net/gh/" + path[0] + "/" + path[1] + "@" + path[2] + "/" + path[3]);
    }
    private static String page(URI raw) {
        String[] path = raw.getRawPath().substring(1).split("/", 4);
        return "https://github.com/" + path[0] + "/" + path[1] + "/blob/" + path[2] + "/" + path[3];
    }
    private LegacyOfficialTranslations() {}
    public static List<Source> installed() throws IOException {
        var result=new ArrayList<Source>();
        for(var mod:ModList.get().getMods()) {
            if(!mod.getModId().equals("thaumcraft")&&mod.getDependencies().stream().noneMatch(d->d.getModId().equals("thaumcraft")))continue;
            var path=mod.getOwningFile().getFile().findResource("data",mod.getModId(),"thaumcraft","legacy_localization.json");
            if(!Files.isRegularFile(path))continue;
            try(var reader=Files.newBufferedReader(path)) {result.addAll(decode(mod.getModId(),JsonParser.parseReader(reader).getAsJsonObject()));}
        }
        return List.copyOf(result);
    }
    public static List<Source> decode(String owner,JsonObject json) throws IOException {
        if(!owner.matches("[a-z0-9_]+"))throw new IOException("Invalid translation owner");
        if(!json.has("official_supplements"))return List.of();
        var result=new ArrayList<Source>();
        for(var element:json.getAsJsonArray("official_supplements")) {
            var s=element.getAsJsonObject();var url=URI.create(s.get("url").getAsString());String hash=s.get("sha256").getAsString();
            if(!allowed(url)||!hash.matches("[0-9a-f]{64}"))throw new IOException("Expected pinned legacy Chinese source and SHA-256");
            var keys=new TreeMap<String,String>();s.getAsJsonObject("keys").entrySet().forEach(e->keys.put(e.getKey(),e.getValue().getAsString()));
            var pictures=new TreeMap<String,String>();
            if(s.has("pictures"))s.getAsJsonObject("pictures").entrySet().forEach(e->pictures.put(e.getKey(),e.getValue().getAsString()));
            var pins = new LinkedHashMap<String,String>();
            pins.put(url.toString(), hash);
            var raw = new LinkedHashSet<>(List.of(url.toString()));
            raw.addAll(ArchiveSources.read(s, "downloads", hash, pins));
            var automatic = new LinkedHashSet<URI>();
            var pages = new LinkedHashSet<String>();
            for (String address : raw) {
                URI direct = URI.create(address);
                if (!allowed(direct)) throw new IOException("Expected a pinned Chinese language file: " + address);
                URI mirror = mirror(direct);
                pins.put(mirror.toString(), pins.get(address));
                automatic.add(direct);
                automatic.add(mirror);
                pages.add(page(direct));
            }
            pages.addAll(ArchiveSources.read(s, "manual_downloads", hash, pins));
            result.add(new Source(owner, url, hash, Map.copyOf(keys), Map.copyOf(pictures),
                    List.copyOf(automatic), new ArchiveSources(pins, List.copyOf(pages))));
        }
        return List.copyOf(result);
    }
    static boolean allowed(URI url) {
        return "https".equals(url.getScheme())&&"raw.githubusercontent.com".equals(url.getHost())&&url.getPort()==-1
                &&url.getUserInfo()==null&&url.getQuery()==null&&url.getFragment()==null
                &&url.getPath().matches("/GTNewHorizons/[A-Za-z0-9_-]+/[0-9a-f]{40}/src/main/resources/assets/[a-z0-9_]+/lang/zh_CN\\.lang");
    }
    public static Map<String,Map<String,String>> load(Path game,List<Source> sources,boolean unused) {
        var result=new TreeMap<String,Map<String,String>>();
        for(var source:sources)try {
            Path root=game.resolve("resources/thaumcraft-localization/official"),file=root.resolve(source.sha256()+".lang");
            boolean valid=Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)&&Files.size(file)<=8*1024*1024;
            if(!valid)continue;
            byte[] bytes=Files.readAllBytes(file);
            if (!source.sources().hashes().containsValue(OriginalResources.sha256(bytes))) continue;
            var old=LegacyLocalization.parse(bytes);var target=result.computeIfAbsent(source.owner(),key->new TreeMap<>());
            source.keys().forEach((modern,legacy)->{var value=old.get(legacy);if(value!=null&&!value.isBlank())target.put(modern,tc4revival.research.ResearchText.remapPictures(value,source.pictures()));});
        }catch(IOException|RuntimeException failure){LOGGER.warn("Legacy Chinese supplement unavailable for {}: {}",source.owner(),failure.toString());}
        return result;
    }
}

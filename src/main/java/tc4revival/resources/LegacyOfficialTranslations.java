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
    public record Source(String owner,URI url,String sha256,Map<String,String> keys,Map<String,String> pictures) {}
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
            result.add(new Source(owner,url,hash,Map.copyOf(keys),Map.copyOf(pictures)));
        }
        return List.copyOf(result);
    }
    private static boolean allowed(URI url) {
        return "https".equals(url.getScheme())&&"raw.githubusercontent.com".equals(url.getHost())&&url.getPort()==-1
                &&url.getUserInfo()==null&&url.getQuery()==null&&url.getFragment()==null
                &&url.getPath().matches("/GTNewHorizons/[A-Za-z0-9_-]+/[0-9a-f]{40}/src/main/resources/assets/[a-z0-9_]+/lang/zh_CN\\.lang");
    }
    public static Map<String,Map<String,String>> load(Path game,List<Source> sources,boolean download) {
        var result=new TreeMap<String,Map<String,String>>();
        for(var source:sources)try {
            Path root=game.resolve("resources/thaumcraft-localization/official"),file=root.resolve(source.sha256()+".lang");
            boolean valid=Files.isRegularFile(file)&&Files.size(file)<=8*1024*1024&&OriginalResources.sha256(Files.readAllBytes(file)).equals(source.sha256());
            if(!valid&&download) {
                Files.createDirectories(root);Path temp=Files.createTempFile(root,".language-",".part");
                try {
                    CurseForgeDownload.transfer(source.url(),temp,LegacyOfficialTranslations::allowed,8*1024*1024,10000,30000);
                    if(!OriginalResources.sha256(Files.readAllBytes(temp)).equals(source.sha256()))throw new IOException("Legacy language checksum mismatch");
                    Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);valid=true;
                }finally{Files.deleteIfExists(temp);}
            }
            if(!valid)continue;
            var old=LegacyLocalization.parse(Files.readAllBytes(file));var target=result.computeIfAbsent(source.owner(),key->new TreeMap<>());
            source.keys().forEach((modern,legacy)->{var value=old.get(legacy);if(value!=null&&!value.isBlank())target.put(modern,tc4revival.research.ResearchText.remapPictures(value,source.pictures()));});
        }catch(IOException|RuntimeException failure){LOGGER.warn("Legacy Chinese supplement unavailable for {}: {}",source.owner(),failure.toString());}
        return result;
    }
}

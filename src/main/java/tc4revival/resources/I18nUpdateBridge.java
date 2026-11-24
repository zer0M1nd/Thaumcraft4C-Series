package tc4revival.resources;

import com.google.gson.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Optional I18nUpdateMod 3.x service integration. Never calls init or GameConfig.addResourcePack. */
public class I18nUpdateBridge {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private final Class<?> config;
    private final Method getString;
    private static final String INDEX = "downloaded.json";
    private static final long MAX_ARCHIVE = 128L * 1024 * 1024;

    private I18nUpdateBridge(Class<?> config) throws ReflectiveOperationException {
        this.config = config;
        var assets = Class.forName("i18nupdatemod.util.AssetUtil", false, config.getClassLoader());
        getString = assets.getMethod("getString", String.class);
        config.getMethod("getAssetDetail", String.class, String.class);
    }
    public static Optional<I18nUpdateBridge> find() throws ReflectiveOperationException {
        try { return Optional.of(new I18nUpdateBridge(Class.forName("i18nupdatemod.core.I18nConfig", false, I18nUpdateBridge.class.getClassLoader()))); }
        catch (ClassNotFoundException absent) {
            // I18nUpdateMod is a ModLauncher transformation service, absent from Forge's ModList.
            var pending = new ArrayDeque<ModuleLayer>();
            var visited = new HashSet<ModuleLayer>();
            ModuleLayer layer = I18nUpdateBridge.class.getModule().getLayer();
            if (layer != null) pending.add(layer);
            pending.add(ModuleLayer.boot());
            while (!pending.isEmpty()) {
                layer = pending.removeFirst();
                if (!visited.add(layer)) continue;
                for (Module module : layer.modules()) if (module.getPackages().contains("i18nupdatemod.core")) {
                    var type = Class.forName(module, "i18nupdatemod.core.I18nConfig");
                    if (type != null) return Optional.of(new I18nUpdateBridge(type));
                }
                pending.addAll(layer.parents());
            }
            return Optional.empty();
        }
    }
    public static Path cache(Path game) { return game.resolve("resources/thaumcraft-i18n/1.7.10"); }

    public static List<Path> cached(Path game) throws IOException {
        Path root = cache(game), index = root.resolve(INDEX);
        if (!Files.isRegularFile(index)) return List.of();
        try {
            var json = JsonParser.parseString(Files.readString(index)).getAsJsonObject();
            if (!json.get("minecraft_version").getAsString().equals("1.7.10")) return List.of();
            var result = new ArrayList<Path>();
            for (var value : json.getAsJsonArray("downloads")) {
                var entry = value.getAsJsonObject();
                Path file = child(root, entry.get("file").getAsString());
                validateZip(file);
                if (!md5(file).equals(entry.get("md5").getAsString()) || !OriginalResources.sha256(Files.readAllBytes(file)).equals(entry.get("sha256").getAsString()))
                    throw new IOException("Damaged cached legacy translation archive");
                result.add(file);
            }
            return List.copyOf(result);
        } catch (RuntimeException error) { throw new IOException("Invalid legacy localization cache index", error); }
    }

    public record Asset(String name, java.net.URI url, String md5, String version) {}
    List<Asset> assets() throws Exception {
        Object detail=config.getMethod("getAssetDetail",String.class,String.class).invoke(null,"1.7.10","Forge");
        var result=new ArrayList<Asset>();
        for(Object asset:(List<?>)detail.getClass().getField("downloads").get(detail)) {
            Class<?> type=asset.getClass();
            String name=(String)type.getField("fileName").get(asset);
            child(Path.of("."),name);
            var uri=java.net.URI.create((String)type.getField("fileUrl").get(asset));
            if(!CurseForgeDownload.webLink(uri)) throw new IOException("Invalid I18n archive link");
            String md5Url=(String)type.getField("md5Url").get(asset);
            String expected=((String)invokeDownload(getString,md5Url)).strip().toLowerCase(Locale.ROOT);
            if(!expected.matches("[0-9a-f]{32}")) throw new IOException("Invalid I18n archive checksum");
            result.add(new Asset(name,uri,expected,(String)type.getField("targetVersion").get(asset)));
        }
        return List.copyOf(result);
    }
    static synchronized void record(Path game,Asset asset,Path file) throws IOException {
        Path index=cache(game).resolve(INDEX);
        var entries=new JsonArray();
        if(Files.isRegularFile(index)) try {
            for(var row:JsonParser.parseString(Files.readString(index)).getAsJsonObject().getAsJsonArray("downloads"))
                if(!row.getAsJsonObject().get("file").getAsString().equals(asset.name())) entries.add(row);
        } catch(RuntimeException ignored) {}
        var entry=new JsonObject();entry.addProperty("file",asset.name());entry.addProperty("url",asset.url().toString());
        entry.addProperty("upstream_version",asset.version());entry.addProperty("md5",asset.md5());
        entry.addProperty("sha256",OriginalResources.sha256(Files.readAllBytes(file)));entries.add(entry);
        var json=new JsonObject();json.addProperty("minecraft_version","1.7.10");json.add("downloads",entries);
        Path temp=Files.createTempFile(index.getParent(),".index-",".part");
        try { Files.writeString(temp,json.toString()); Files.move(temp,index,StandardCopyOption.REPLACE_EXISTING); }
        finally { Files.deleteIfExists(temp); }
    }
    /** Retry only transport errors; a bad checksum or incompatible API is not a retryable download. */
    static Object invokeDownload(Method method,Object... arguments) throws Exception {
        for(int attempt=1;;attempt++)try {return method.invoke(null,arguments);}
        catch(java.lang.reflect.InvocationTargetException failure) {
            if(!(failure.getCause() instanceof IOException)||attempt>=3)throw failure;
            LOGGER.warn("I18nUpdateMod transport failed (attempt {}/3); retrying: {}",attempt,failure.getCause().toString());
            Thread.sleep(attempt*750L);
        }
    }
    private static Path child(Path root,String name) throws IOException {
        if (!name.matches("[A-Za-z0-9._-]+\\.zip")) throw new IOException("Invalid legacy archive filename");
        return root.resolve(name);
    }
    static void validateZip(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file)>MAX_ARCHIVE) throw new IOException("Invalid legacy translation archive");
        try (var zip = new ZipFile(file.toFile())) {
            if (zip.getEntry("pack.mcmeta")==null) throw new IOException("Legacy translation pack metadata missing");
        }
    }
    static String md5(Path file) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file))); }
        catch(java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}

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
    private final Method download, getString;
    private static final String INDEX = "downloaded.json";
    private static final long MAX_ARCHIVE = 128L * 1024 * 1024;

    private I18nUpdateBridge(Class<?> config) throws ReflectiveOperationException {
        this.config = config;
        var assets = Class.forName("i18nupdatemod.util.AssetUtil", false, config.getClassLoader());
        download = assets.getMethod("download", String.class, Path.class);
        getString = assets.getMethod("getString", String.class);
        config.getMethod("getAssetDetail", String.class, String.class);
    }
    public static Optional<I18nUpdateBridge> find() throws ReflectiveOperationException {
        try { return Optional.of(new I18nUpdateBridge(Class.forName("i18nupdatemod.core.I18nConfig"))); }
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
                if (!OriginalResources.sha256(Files.readAllBytes(file)).equals(entry.get("sha256").getAsString()))
                    throw new IOException("Damaged cached legacy translation archive");
                result.add(file);
            }
            return List.copyOf(result);
        } catch (RuntimeException error) { throw new IOException("Invalid legacy localization cache index", error); }
    }

    public List<Path> update(Path game) throws Exception {
        Path root = cache(game); Files.createDirectories(root);
        Object detail = config.getMethod("getAssetDetail",String.class,String.class).invoke(null,"1.7.10","Forge");
        var downloads = (List<?>) detail.getClass().getField("downloads").get(detail);
        if (downloads.isEmpty()) throw new IOException("I18nUpdateMod returned no 1.7.10 downloads");
        var index = new JsonObject(); index.addProperty("minecraft_version","1.7.10");
        var entries = new JsonArray(); var files = new ArrayList<Path>();
        for (Object asset : downloads) {
            Class<?> type = asset.getClass();
            String name = (String) type.getField("fileName").get(asset);
            String url = (String) type.getField("fileUrl").get(asset);
            String md5Url = (String) type.getField("md5Url").get(asset);
            String version = (String) type.getField("targetVersion").get(asset);
            Path file = child(root,name);
            String expected = ((String)invokeDownload(getString,md5Url)).strip().toLowerCase(Locale.ROOT);
            if (!expected.matches("[0-9a-f]{32}")) throw new IOException("Invalid I18nUpdateMod download checksum");
            if (!Files.isRegularFile(file) || Files.size(file)>MAX_ARCHIVE || !md5(file).equals(expected)) {
                Path temp = Files.createTempFile(root,".download-",".part");
                try {
                    invokeDownload(download,url,temp);
                    validateZip(temp);
                    if (!md5(temp).equals(expected)) throw new IOException("Legacy translation download checksum mismatch");
                    Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
            }
            validateZip(file);
            var entry = new JsonObject(); entry.addProperty("file",name); entry.addProperty("url",url);
            entry.addProperty("upstream_version",version); entry.addProperty("md5",expected);
            entry.addProperty("sha256",OriginalResources.sha256(Files.readAllBytes(file)));
            entries.add(entry); files.add(file);
        }
        index.add("downloads",entries);
        Path temp = Files.createTempFile(root,".index-",".tmp");
        try {
            Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(index));
            Files.move(temp,root.resolve(INDEX),StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
        return List.copyOf(files);
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
    private static void validateZip(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file)>MAX_ARCHIVE) throw new IOException("Invalid legacy translation archive");
        try (var zip = new ZipFile(file.toFile())) {
            if (zip.getEntry("pack.mcmeta")==null) throw new IOException("Legacy translation pack metadata missing");
        }
    }
    private static String md5(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file)));
    }
}

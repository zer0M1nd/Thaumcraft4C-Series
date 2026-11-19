package tc4revival.resources;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipInputStream;

/** Reads data only: no original classes are loaded or executed. */
public  class OriginalResources {
    public static final String REFERENCE_SHA256 = "3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be";
    public static final long MAX_JAR_BYTES = 128L * 1024 * 1024;
    private static final int MAX_ENTRY_BYTES = 32 * 1024 * 1024;
    private static final long MAX_RESOURCE_BYTES = 256L * 1024 * 1024;
    private OriginalResources() {}

    public record Snapshot(Path file, String sha256, Map<String, byte[]> resources) {
        public boolean reference() { return REFERENCE_SHA256.equals(sha256); }
    }

    /** A declaration for originals with absent or stale mcmod.info. The exact archive hash anchors the
     * declared Minecraft version; @Mod is inspected as data and is never loaded as a class. */
    public record AnnotationIdentity(String classFile,String version,String sha256) {
        public AnnotationIdentity {
            if(classFile==null||!classFile.matches("(?:[A-Za-z_$][A-Za-z0-9_$]*/)*[A-Za-z_$][A-Za-z0-9_$]*\\.class")
                    ||classFile.length()>512||version==null||version.isBlank()||version.length()>128
                    ||sha256==null||!sha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Invalid legacy annotation identity");
        }
    }

    public static List<Path> candidates(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var files = Files.list(directory)) {
            return files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")
                    || Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS) && !p.getFileName().toString().startsWith(".") && Files.isRegularFile(p.resolve(ProcessedOriginalResources.MANIFEST), LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
        }
    }

    public static Map<String, String> mappings() throws IOException {
        var result = new LinkedHashMap<String, String>();
        try (var input = OriginalResources.class.getResourceAsStream("/tc4revival/external-resources.tsv")) {
            if (input == null) throw new IOException("Missing external resource mapping");
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                var pair = line.split("\\t", -1);
                if (pair.length != 2 || !safePath(pair[0]) || !safePath(pair[1])
                        || result.putIfAbsent(pair[0], pair[1]) != null)
                    throw new IOException("Invalid external resource mapping: " + line);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static boolean safePath(String path) {
        return path.startsWith("assets/") && !path.contains("\\") && !path.contains(":")
                && Arrays.stream(path.split("/", -1)).noneMatch(s -> s.isEmpty() || s.equals(".") || s.equals(".."));
    }

    public static Snapshot read(Path file, Map<String, String> mapping) throws IOException {
        return read(file,mapping,"Thaumcraft",true);
    }
    public static Snapshot read(Path file,Map<String,String> mapping,String modId,boolean baseLanguages) throws IOException {
        return read(file, mapping, modId, "1.7.10", baseLanguages);
    }
    public static Snapshot read(Path file,Map<String,String> mapping,String modId,String minecraftVersion,boolean baseLanguages) throws IOException {
        return read(file, mapping, modId, minecraftVersion, baseLanguages, true,null);
    }
    public static Snapshot read(Path file,Map<String,String> mapping,String modId,String minecraftVersion,boolean baseLanguages,AnnotationIdentity identity) throws IOException {
        return read(file,mapping,modId,minecraftVersion,baseLanguages,true,identity);
    }

    // Thaumic Bases 1.3.1710.4 has no mcmod.info; its existing loader identifies resources by its complete map.
    static Snapshot readMappedAssets(Path file, Map<String,String> mapping) throws IOException {
        return read(file, mapping, null, "1.7.10", false, false,null);
    }

    private static Snapshot read(Path file, Map<String,String> mapping, String modId, String minecraftVersion, boolean baseLanguages, boolean metadataRequired,AnnotationIdentity identity) throws IOException {
        if(Files.isDirectory(file,LinkOption.NOFOLLOW_LINKS)){
            var result=ProcessedOriginalResources.read(file,mapping,modId,minecraftVersion,baseLanguages);
            if(identity!=null&&!identity.sha256().equals(result.sha256()))throw new IOException("Different annotated original archive hash");
            return result;
        }
        String identityPath=identity==null?identityClass(modId):identity.classFile();
        var wanted = new HashSet<>(mapping.values());
        if (metadataRequired) wanted.add("mcmod.info");
        if (baseLanguages) {
            wanted.add("assets/thaumcraft/lang/en_US.lang");
            wanted.add("assets/thaumcraft/lang/zh_CN.lang");
            wanted.add("assets/thaumcraft/lang/zh_TW.lang");
        }
        // Reject unrelated archives using only the ZIP directory, without inflating classes/assets.
        // This is only a prefilter: accepted files still undergo the complete bounded snapshot check below.
        if (Files.size(file) > MAX_JAR_BYTES) throw new IOException("JAR exceeds 128 MiB");
        try (var index = new java.util.zip.ZipFile(file.toFile())) {
            // An explicit, hash-pinned identity also covers archives with stale mcmod.info.
            // Automatic historical fallbacks remain limited to archives without that file.
            // Inspect @Mod only as data; never load or execute the original classes.
            if(metadataRequired && identityPath!=null && (identity!=null || index.getEntry("mcmod.info")==null)) {
                wanted.remove("mcmod.info");wanted.add(identityPath);
            }
            var missing = wanted.stream().filter(p -> index.getEntry(p) == null).sorted().toList();
            if (!missing.isEmpty()) throw new IOException("Missing " + missing.size() + " required resources, e.g. " + missing.get(0));
        }
        // Hash and read the same immutable snapshot, even if the player replaces the file later.
        byte[] archive;
        try (var input = Files.newInputStream(file)) {
            archive = input.readNBytes((int) MAX_JAR_BYTES + 1);
        }
        if (archive.length > MAX_JAR_BYTES) throw new IOException("JAR exceeds 128 MiB");
        var found = new HashMap<String, byte[]>();
        var optional = new HashSet<String>();
        mapping.values().stream().filter(path -> path.endsWith(".png")).forEach(path -> optional.add(path + ".mcmeta"));
        long total = 0;
        int entries = 0;
        int entryLimit = metadataRequired ? MAX_ENTRY_BYTES : 16 * 1024 * 1024;
        byte[] discard = new byte[8192];
        try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entries > (metadataRequired ? 30000 : 10000)) throw new IOException("Too many JAR entries");
                if (entry.getSize() > entryLimit)
                    throw new IOException("Invalid resource: " + entry.getName());
                if (!wanted.contains(entry.getName()) && !optional.contains(entry.getName())) {
                    int size = 0;
                    for (int count; (count = zip.read(discard)) != -1;) {
                        size += count;
                        total += count;
                        if (size > entryLimit || total > MAX_RESOURCE_BYTES)
                            throw new IOException("External resource size limit exceeded");
                    }
                    continue;
                }
                // Bound even ignored entries: advancing a ZipInputStream otherwise drains them unbounded.
                var bytes = zip.readNBytes(entryLimit + 1);
                total += bytes.length;
                if (bytes.length > entryLimit || total > MAX_RESOURCE_BYTES)
                    throw new IOException("External resource size limit exceeded");
                if (entry.isDirectory()) throw new IOException("Resource is a directory: " + entry.getName());
                if (found.putIfAbsent(entry.getName(), bytes) != null)
                    throw new IOException("Duplicate resource: " + entry.getName());
            }
        }
        var missing = wanted.stream().filter(p -> !found.containsKey(p)).sorted().toList();
        if (!missing.isEmpty()) throw new IOException("Missing " + missing.size() + " required resources, e.g. " + missing.get(0));
        if (metadataRequired && (identity!=null || !found.containsKey("mcmod.info"))) {
            if(identity==null)verifyAnnotationIdentity(found.get(identityPath),modId,minecraftVersion);
            else {
                var values=annotationValues(found.get(identityPath));
                if(!modId.equals(values.get("modid"))||!identity.version().equals(values.get("version"))
                        ||!identity.sha256().equals(sha256(archive)))throw new IOException("Different declared legacy annotation identity or archive hash");
            }
        } else if (metadataRequired) try {
            var metadata=com.google.gson.JsonParser.parseString(new String(found.get("mcmod.info"),StandardCharsets.UTF_8));
            var rows=metadata.isJsonArray()?metadata.getAsJsonArray():metadata.getAsJsonObject().getAsJsonArray("modList");
            boolean match=false;
            if(rows!=null)for(var row:rows) {
                var mod=row.getAsJsonObject();
                if(mod.has("modid") && mod.get("modid").getAsString().equals(modId)
                        && mod.has("mcversion") && mod.get("mcversion").getAsString().equals(minecraftVersion))match=true;
            }
            if(!match)throw new IOException("Expected legacy modid "+modId+" for Minecraft "+minecraftVersion);
        }catch(RuntimeException failure) {throw new IOException("Invalid mcmod.info",failure);}
        if (metadataRequired) LegacyModLogo.read(archive, modId).ifPresent(logo -> found.put(LegacyModLogo.CACHE_FILE, logo));
        return new Snapshot(file.toAbsolutePath().normalize(), sha256(archive), Collections.unmodifiableMap(found));
    }

    private static String identityClass(String modId) {
        if("thaumicbases".equals(modId))return "tb/core/TBCore.class";
        if("exnihilo".equals(modId))return "exnihilo/ExNihilo.class";
        return null;
    }
    private static void verifyAnnotationIdentity(byte[] bytes,String modId,String minecraftVersion) throws IOException {
        if(bytes==null || identityClass(modId)==null || !"1.7.10".equals(minecraftVersion))throw new IOException("Unidentified legacy resource version");
        var values=annotationValues(bytes);
        boolean version="thaumicbases".equals(modId) ? values.getOrDefault("version","").contains(".1710.")
                : "1.38-53".equals(values.get("version")) && values.getOrDefault("dependencies","").contains("Forge@[10.13.");
        if(!modId.equals(values.get("modid")) || !version)
            throw new IOException("Expected legacy modid "+modId+" for Minecraft 1.7.10");
    }
    private static Map<String,String> annotationValues(byte[] bytes) throws IOException {
        if(bytes==null)throw new IOException("Missing legacy identity metadata");
        var values=new HashMap<String,String>();
        try {
            new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor,boolean visible) {
                    if(!descriptor.equals("Lcpw/mods/fml/common/Mod;"))return null;
                    return new org.objectweb.asm.AnnotationVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override public void visit(String key,Object value){if(value instanceof String text)values.put(key,text);}
                    };
                }
            },org.objectweb.asm.ClassReader.SKIP_CODE|org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
        }catch(RuntimeException failure){throw new IOException("Invalid legacy identity metadata",failure);}
        return values;
    }

    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}

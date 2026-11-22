package tc4revival.resources;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IConfigurable;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

/** Optional artwork from the selected legacy mod's own mcmod.info record. */
final class LegacyModLogo {
    static final String CACHE_FILE = ".original-logo.png";
    private static final int MAX_BYTES = 16 * 1024 * 1024;

    static Set<String> primarySourceOwners() {
        var owners = new HashSet<String>();
        for (var mod : ModList.get().getMods()) {
            // Read the declaration: Forge's live logoFile may already contain an extracted logo on reload.
            if (usesPrimarySource(mod.getModId(), mod.getConfig())) {
                owners.add(mod.getModId());
            }
        }
        return owners;
    }

    static boolean usesPrimarySource(String owner, IConfigurable declaration) {
        return switch (owner) {
            case "thaumcraft", "thaumicomniverse" -> false;
            case "thaumicfutures" -> declaration.<String>getConfigElement("logoFile").filter(path -> !path.isBlank()).isEmpty();
            default -> true;
        };
    }

    static Optional<byte[]> read(byte[] verifiedArchive, String modId) {
        try (var zip = new ZipFile(new SeekableInMemoryByteChannel(verifiedArchive))) {
            var metadata = zip.getEntry("mcmod.info");
            if (metadata == null || metadata.getSize() > 1024 * 1024) return Optional.empty();
            try (var input = zip.getInputStream(metadata)) {
                var bytes = input.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) return Optional.empty();
                var root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
                var rows = root.isJsonArray() ? root.getAsJsonArray() : root.getAsJsonObject().getAsJsonArray("modList");
                if (rows == null) return Optional.empty();
                for (var value : rows) {
                    var row = value.getAsJsonObject();
                    if (!row.has("modid") || !modId.equals(row.get("modid").getAsString())) continue;
                    var logo = row.get("logoFile");
                    if (logo == null || !logo.isJsonPrimitive()) return Optional.empty();
                    String path = logo.getAsString().strip();
                    // Legacy FML treats a leading slash as the archive root (e.g. Thaumic Tinkerer).
                    if (path.startsWith("/")) path = path.substring(1);
                    if (path.isEmpty() || path.contains("\\") || path.contains(":") || path.indexOf(0) >= 0
                            || Arrays.stream(path.split("/", -1)).anyMatch(p -> p.isEmpty() || p.equals(".") || p.equals("..")))
                        return Optional.empty();
                    var entry = zip.getEntry(path);
                    if (entry == null || entry.isDirectory() || entry.getSize() > MAX_BYTES) return Optional.empty();
                    try (var image = zip.getInputStream(entry)) {
                        byte[] png = image.readNBytes(MAX_BYTES + 1);
                        return validPng(png) ? Optional.of(png) : Optional.empty();
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Missing or malformed artwork never makes a verified gameplay resource pack unusable.
        }
        return Optional.empty();
    }

    private static boolean validPng(byte[] bytes) {
        if (bytes.length < 33 || bytes.length > MAX_BYTES) return false;
        var header = ByteBuffer.wrap(bytes);
        if (header.getLong() != 0x89504e470d0a1a0aL || header.getInt() != 13 || header.getInt() != 0x49484452) return false;
        int width = header.getInt(), height = header.getInt();
        return width > 0 && height > 0 && width <= 8192 && height <= 8192 && (long) width * height <= 16_777_216;
    }

    private LegacyModLogo() {}
}

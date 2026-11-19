package tc4revival.resources;

import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

/** Supplies existing asset assertions through the production conversion path; never adds assets to the test classpath. */
public  class ExternalTestResources {
    private static Map<String, byte[]> converted;
    private ExternalTestResources() {}
    public static synchronized InputStream open(String path) throws IOException {
        String target = path.startsWith("/") ? path.substring(1) : path;
        if (converted == null) {
            var mapping = OriginalResources.mappings();
            converted = ExternalResourceCache.convert(OriginalResources.read(
                    Path.of("references/Thaumcraft-1.7.10-4.2.3.5.jar"), mapping), mapping);
        }
        byte[] external = converted.get(target);
        InputStream own = ExternalTestResources.class.getResourceAsStream("/" + target);
        if (external == null) return own;
        if (target.contains("/lang/") && own != null) {
            var merged = JsonParser.parseString(new String(external, StandardCharsets.UTF_8)).getAsJsonObject();
            try (own) {
                var overrides = JsonParser.parseString(new String(own.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                overrides.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
            }
            return new ByteArrayInputStream(merged.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (own != null) {
            own.close();
            throw new IOException("External-only asset unexpectedly bundled: " + target);
        }
        return new ByteArrayInputStream(external);
    }
}

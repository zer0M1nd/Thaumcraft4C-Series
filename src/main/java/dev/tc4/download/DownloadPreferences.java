package dev.tc4.download;

import com.electronwill.nightconfig.toml.TomlParser;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/** Both bootstrap layers read the core's client setting before Forge loads client configs. */
public final class DownloadPreferences {
    public static final String KEY = "downloads.chinese_localization";
    public enum Mode { ENABLED, DISABLED, DETECT }
    private static String detectedCountry;

    public static Optional<Mode> parse(Object value) {
        if (!(value instanceof String text)) return Optional.empty();
        return switch (text.strip().toUpperCase(Locale.ROOT)) {
            case "ENABLED", "启用" -> Optional.of(Mode.ENABLED);
            case "DISABLED", "禁用" -> Optional.of(Mode.DISABLED);
            case "DETECT", "检测" -> Optional.of(Mode.DETECT);
            default -> Optional.empty();
        };
    }

    public static boolean chinese(Path game) {
        Mode mode = Mode.DETECT;
        Path config = game.resolve("config/thaumcraft-client.toml");
        if (Files.isRegularFile(config)) {
            try (var reader = Files.newBufferedReader(config)) {
                mode = parse(new TomlParser().parse(reader).get(KEY)).orElse(Mode.DETECT);
            } catch (IOException | RuntimeException e) {
                System.getLogger("ThaumcraftDownloads").log(System.Logger.Level.WARNING,
                        "Cannot read Chinese localization preference", e);
            }
        }
        return enabled(mode, DownloadPreferences::country);
    }

    static boolean enabled(Mode mode, Supplier<String> country) {
        return switch (mode) {
            case ENABLED -> true;
            case DISABLED -> false;
            case DETECT -> "CN".equals(country.get());
        };
    }

    private static synchronized String country() {
        if (detectedCountry != null) return detectedCountry;
        // Both endpoints report an ISO country code for the network exit, independently of game/OS language.
        detectedCountry = queryCountry("https://www.cloudflare.com/cdn-cgi/trace", true);
        if (detectedCountry.isEmpty()) detectedCountry = queryCountry("https://api.country.is/", false);
        return detectedCountry;
    }

    private static String queryCountry(String url, boolean trace) {
        try {
            var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setInstanceFollowRedirects(false);
            try {
                if (connection.getResponseCode() == 200) try (var in = connection.getInputStream()) {
                    String body = new String(in.readNBytes(4096), StandardCharsets.UTF_8);
                    String code = trace ? body.lines().filter(s -> s.matches("loc=[A-Z]{2}")).map(s -> s.substring(4)).findFirst().orElse("")
                            : com.google.gson.JsonParser.parseString(body).getAsJsonObject().get("country").getAsString();
                    if (code.matches("[A-Z]{2}")) return code;
                }
            } finally { connection.disconnect(); }
        } catch (IOException | RuntimeException e) {
            System.getLogger("ThaumcraftDownloads").log(System.Logger.Level.DEBUG, "Network region unavailable", e);
        }
        return "";
    }

    private DownloadPreferences() {}
}

package dev.tc4.updater;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public final class ReleaseApi {
    public static final URI PUBLIC_SERVER = URI.create("http://sn.zer0m1nd.me:42003/");
    private static final long MAX_JAR = 128L * 1024 * 1024;
    public static final List<URI> SITES = List.of(URI.create("https://tc4c.zer0m1nd.me/"), URI.create("http://tc4c.zer0m1nd.me/"),
            PUBLIC_SERVER, URI.create("https://tc4c1.zer0m1nd.me/"), URI.create("http://tc4c1.zer0m1nd.me/"));
    private final List<URI> sites;
    private final int timeoutMillis;

    public record Source(URI url, String sha256, long size) {}
    public record Release(String modid, String version, String filename, String sha256, long size, URI url,
                          List<Source> sources, List<URI> manualPages, List<Source> manualFiles) {
        public Release(String modid, String version, String filename, String sha256, long size, URI url) {
            this(modid, version, filename, sha256, size, url, List.of(), List.of(), List.of());
        }
        public Release {
            sources = sources == null ? List.of() : List.copyOf(sources);
            manualPages = manualPages == null ? List.of() : List.copyOf(manualPages);
            manualFiles = manualFiles == null ? List.of() : List.copyOf(manualFiles);
        }
        Source primary() { return new Source(url, sha256, size); }
        Source from(URI uri) {
            return sources.stream().filter(s -> s.url().equals(uri)).findFirst().orElse(primary());
        }
        Source matching(String hash, long length) {
            if (sha256.equals(hash) && size == length) return primary();
            return java.util.stream.Stream.concat(sources.stream(), manualFiles.stream())
                    .filter(s -> s.sha256().equals(hash) && s.size() == length).findFirst().orElse(null);
        }
    }
    public record Channels(String name, URI changelog, Release stable, Release testing) {}
    public record Catalog(Set<String> managed, Map<String, Channels> releases) {}

    public ReleaseApi() { this(SITES, 7000); }
    public ReleaseApi(URI server) { this(List.of(server), 10000); }
    ReleaseApi(URI server, int timeoutMillis) { this(List.of(server), timeoutMillis); }
    ReleaseApi(List<URI> sites, int timeoutMillis) {
        this.sites = List.copyOf(sites);
        this.timeoutMillis = timeoutMillis;
    }

    public Map<String, Release> latest(boolean includeUnstable) throws IOException {
        Map<String, Release> result = new LinkedHashMap<>();
        catalog(includeUnstable).forEach((id, channels) -> {
            Release latest = channels.stable;
            if (channels.testing != null && (latest == null || Version.parse(channels.testing.version).compareTo(Version.parse(latest.version)) > 0)) latest = channels.testing;
            if (latest != null) result.put(id, latest);
        });
        return result;
    }

    public Map<String, Channels> catalog(boolean includeTesting) throws IOException {
        return snapshot(includeTesting).releases();
    }

    public Catalog snapshot(boolean includeTesting) throws IOException {
        return snapshot(UpdaterConfig.DEFAULT_BASELINE, includeTesting);
    }

    static String catalogPath(String baseline) throws IOException {
        return UpdaterConfig.DEFAULT_BASELINE.equals(UpdaterConfig.baselineName(baseline)) ? "/latest.json" : "/baselines/" + baseline + ".json";
    }
    public List<URI> catalogLinks(String baseline) throws IOException {
        String path = catalogPath(baseline);
        return sites.stream().map(site -> site.resolve(path)).toList();
    }
    public Catalog snapshot(String baseline, boolean includeTesting) throws IOException {
        String path = catalogPath(baseline);
        IOException failure = new IOException("No release catalog available");
        for (URI site : sites) {
            try { return snapshot(site, path, baseline, includeTesting); }
            catch (IOException ex) { failure.addSuppressed(ex); }
            if (UpdaterConfig.DEFAULT_BASELINE.equals(baseline) && (site.equals(PUBLIC_SERVER) || sites.size() == 1)) {
                try { return snapshot(site, "/api/v2/mods", baseline, includeTesting); }
                catch (IOException ex) { failure.addSuppressed(ex); }
            }
        }
        String details=Arrays.stream(failure.getSuppressed()).map(Throwable::getMessage).distinct()
                .collect(java.util.stream.Collectors.joining(" / "));
        throw new IOException(failure.getMessage()+": "+details,failure);
    }

    public List<URI> manualPages(String id) {
        URI site = sites.equals(SITES) ? URI.create("http://tc4c.zer0m1nd.me/") : sites.get(0);
        return List.of(site.resolve("/mods/" + id + "/index.html"));
    }

    List<URI> manualPages(Release release) {
        var pages = new LinkedHashSet<>(release.manualPages());
        pages.addAll(manualPages(release.modid()));
        // A named baseline may pin a file older than the releases listed on the project page.
        URI archive = sites.equals(SITES) ? PUBLIC_SERVER : sites.get(0);
        pages.add(archive.resolve(release.url().getRawPath()));
        return List.copyOf(pages);
    }
    public Catalog catalogForPage(URI page,boolean testing) throws IOException {
        return catalogForPage(page, UpdaterConfig.DEFAULT_BASELINE, testing);
    }
    public Catalog catalogForPage(URI page, String baseline, boolean testing) throws IOException {
        requireOrigin(page); return snapshot(page.resolve("/"), catalogPath(baseline), baseline, testing);
    }
    private Catalog snapshot(URI server, String path, String baseline, boolean includeTesting) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        transfer(server.resolve(path), data, 4 * 1024 * 1024, Duration.ofSeconds(20), (n,total)->{});
        try {
            JsonObject root = JsonParser.parseString(data.toString(StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schema").getAsInt() != 2) throw new IOException("Unsupported release API schema");
            String actual = root.has("baseline") ? root.get("baseline").getAsString() : UpdaterConfig.DEFAULT_BASELINE;
            if (!baseline.equals(actual)) throw new IOException("Expected release baseline " + baseline + ", received " + actual);
            if (!root.has("managed_modids") || !root.get("managed_modids").isJsonArray())
                throw new IOException("Release API is missing the managed_modids whitelist");
            Set<String> managed = new LinkedHashSet<>();
            for (JsonElement item : root.getAsJsonArray("managed_modids")) {
                if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()
                        || !UpdateEngine.validModid(item.getAsString()) || !managed.add(item.getAsString()))
                    throw new IOException("Invalid or duplicate managed modid");
            }
            if (managed.size() > 512) throw new IOException("Managed modid whitelist exceeds limit");
            managed.removeAll(UpdateEngine.EXCLUDED);
            Map<String, Channels> releases = new LinkedHashMap<>();
            Set<String> seen = new HashSet<>();
            for (var item : root.getAsJsonArray("mods")) {
                JsonObject mod = item.getAsJsonObject();
                String id = mod.get("modid").getAsString();
                if (!managed.contains(id)) continue;
                if (!seen.add(id)) throw new IOException("Duplicate API mod ID: " + id);
                if (mod.has("deployment_allowed") && !mod.get("deployment_allowed").getAsBoolean()) { managed.remove(id); continue; }
                Release stable = release(server, mod.get("latest_stable"), id, true);
                Release testing = includeTesting ? release(server, mod.get("latest_testing"), id, false) : null;
                String name = mod.has("name") ? mod.get("name").getAsString() : id;
                // Construct the trusted same-origin link; never open arbitrary URLs supplied in a catalog label.
                releases.put(id, new Channels(name, server.resolve("/docs/changelogs/" + id + ".html"), stable, testing));
            }
            if (!UpdaterConfig.DEFAULT_BASELINE.equals(baseline) && !releases.keySet().equals(managed))
                throw new IOException("Incomplete release baseline: " + baseline);
            return new Catalog(Set.copyOf(managed), Map.copyOf(releases));
        } catch (RuntimeException error) {
            throw new IOException("Malformed release API response", error);
        }
    }

    private Release release(URI server, JsonElement element, String id, boolean stable) throws IOException {
        if (element == null || element.isJsonNull()) return null;
        JsonObject row = element.getAsJsonObject();
        String version = row.get("version").getAsString();
        if (!id.equals(row.get("modid").getAsString()) || Version.parse(version).stable() != stable
                || !(stable ? "stable" : "testing").equals(row.get("channel").getAsString()))
            throw new IOException("Release identity/channel mismatch: " + id);
        String filename = FilesSupport.filename(row.get("filename").getAsString());
        String hash = row.get("sha256").getAsString();
        long size = row.get("size").getAsBigDecimal().longValueExact();
        if (!hash.matches("[a-fA-F0-9]{64}") || size <= 0 || size > MAX_JAR) throw new IOException("Invalid release hash/size");
        URI uri = server.resolve(row.get("url").getAsString());
        if (!sameOrigin(server,uri)) throw new IOException("Release URL is outside the catalog origin");
        requireOrigin(uri);
        if (!uri.normalize().equals(uri) || !uri.getPath().startsWith("/download/files/releases/" + id + "/")
                || !uri.getPath().endsWith("/" + filename)) throw new IOException("Invalid release download path");
        var sources = new ArrayList<Source>();
        var seen = new HashSet<URI>();
        seen.add(uri);
        if (row.has("sources")) for (var value : row.getAsJsonArray("sources")) {
            var src = value.getAsJsonObject();
            URI link = server.resolve(src.get("url").getAsString());
            requireOrigin(link);
            String digest = src.get("sha256").getAsString().toLowerCase(Locale.ROOT);
            long length = src.get("size").getAsBigDecimal().longValueExact();
            if (!digest.matches("[0-9a-f]{64}") || length <= 0 || length > MAX_JAR || !seen.add(link)
                    || !link.normalize().equals(link) || !link.getPath().startsWith("/download/files/releases/" + id + "/"))
                throw new IOException("Invalid alternate release source for " + id);
            sources.add(new Source(link, digest, length));
        }
        var pages = new ArrayList<URI>();
        var manualFiles = new ArrayList<Source>();
        if (row.has("manual_links")) for (var value : row.getAsJsonArray("manual_links")) {
            var page = URI.create(value.isJsonObject() ? value.getAsJsonObject().get("url").getAsString() : value.getAsString());
            if (page.getHost() == null || page.getUserInfo() != null || !Set.of("http","https").contains(page.getScheme()))
                throw new IOException("Invalid release browser link");
            pages.add(page);
            if (value.isJsonObject()) {
                var file = value.getAsJsonObject();
                String digest = file.get("sha256").getAsString().toLowerCase(Locale.ROOT);
                long length = file.get("size").getAsBigDecimal().longValueExact();
                if (!digest.matches("[0-9a-f]{64}") || length <= 0 || length > MAX_JAR)
                    throw new IOException("Invalid manual release hash/size");
                manualFiles.add(new Source(page, digest, length));
            }
        }
        return new Release(id, version, filename, hash.toLowerCase(Locale.ROOT), size, uri, sources, pages, manualFiles);
    }

    public List<URI> links(Release release) {
        var result = new LinkedHashSet<URI>();
        result.add(release.url());
        release.sources().forEach(source -> result.add(source.url()));
        for (var site : sites) result.add(site.resolve(release.url().getRawPath()));
        return List.copyOf(result);
    }

    public void download(Release release, URI source, Path destination, dev.tc4.download.DownloadQueue.Progress progress) throws IOException {
        Source expected = release.from(source);
        try (OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            long count = transfer(source, output, expected.size(), Duration.ofSeconds(90), (n,ignored)->progress.bytes(n,expected.size()));
            if (count != expected.size()) throw new IOException("Truncated download for " + release.modid);
        }
        if (!FilesSupport.sha256(destination).equals(expected.sha256())) throw new IOException("SHA-256 mismatch for " + release.modid);
        try (FileChannel channel = FileChannel.open(destination, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    private static boolean sameOrigin(URI base, URI uri) {
        return Objects.equals(base.getScheme(),uri.getScheme()) && Objects.equals(base.getHost(),uri.getHost())
                && base.getPort()==uri.getPort();
    }
    private void requireOrigin(URI uri) throws IOException {
        if (sites.stream().noneMatch(site -> sameOrigin(site,uri)) || uri.getRawUserInfo() != null || uri.getFragment() != null
                || uri.getRawQuery()!=null || !Set.of("http","https").contains(uri.getScheme()))
            throw new IOException("Refusing download outside the release server: " + uri);
    }

    private long transfer(URI uri, OutputStream output, long limit, Duration budget, dev.tc4.download.DownloadQueue.Progress progress) throws IOException {
        long deadline = System.nanoTime() + budget.toNanos();
        for (int redirect = 0; redirect < 4; redirect++) {
            requireOrigin(uri);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(Math.min(timeoutMillis, 5000));
            connection.setReadTimeout(timeoutMillis);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "ThaumcraftUpdater");
            connection.setRequestProperty("Accept-Encoding", "identity");
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirect without Location");
                    uri = uri.resolve(location);
                    continue;
                }
                if (status != 200) throw new IOException("Release server returned HTTP " + status);
                if (connection.getContentLengthLong() > limit) throw new IOException("Response exceeds expected size");
                try (InputStream input = connection.getInputStream()) {
                    long total = 0;
                    byte[] buffer = new byte[65536];
                    for (int count; (count = input.read(buffer)) != -1;) {
                        total += count;
                        if (total > limit || System.nanoTime() > deadline) throw new IOException("Download size/time limit exceeded");
                        output.write(buffer, 0, count);
                        long since = System.nanoTime();
                        progress.bytes(total, connection.getContentLengthLong());
                        deadline += System.nanoTime()-since;
                    }
                    return total;
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many release server redirects");
    }
}

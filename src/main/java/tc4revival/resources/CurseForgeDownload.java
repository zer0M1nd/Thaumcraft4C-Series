package tc4revival.resources;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Set;
import java.util.function.Predicate;

/** Verified CurseForge CDN files first, then the project's bounded original-resource proxy.
 * Unknown versions retain the public website fallback; no challenge scraping.
 */
public class CurseForgeDownload {
    // The author's GitHub release has the exact SHA-256 declared by tc4auto.
    // Resolve this fixed share link without scraping a file-hosting web page.
    private static final java.util.Map<String,String> VERIFIED_ALIASES = java.util.Map.of(
            "https://wwamr.lanzoum.com/ivivH4909gfe",
            "https://github.com/cdj68765/TC4AutoResearch/releases/download/0.0.3/TCAutoResearch.jar");
    private static final Set<String> HOSTS = Set.of("www.curseforge.com", "curseforge.com",
            "edge.forgecdn.net", "media.forgecdn.net", "mediafilez.forgecdn.net", "files.forgecdn.net");
    public static URI endpoint(String filePage) throws IOException {
        try {
            var uri = URI.create(filePage);
            if(VERIFIED_ALIASES.containsKey(filePage))return URI.create(VERIFIED_ALIASES.get(filePage));
            if(VERIFIED_ALIASES.containsValue(filePage))return uri;
            if(allowed(uri)&&uri.getHost().endsWith(".forgecdn.net")&&uri.getPath().endsWith(".jar"))return uri;
            if (!"https".equals(uri.getScheme()) || !Set.of("www.curseforge.com", "curseforge.com").contains(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException("Not a CurseForge file page");
            var match = java.util.regex.Pattern.compile("/minecraft/mc-mods/([a-z0-9-]+)/files/([0-9]+)/?").matcher(uri.getPath());
            if (!match.matches()) throw new IllegalArgumentException("A specific CurseForge file is required");
            String key = match.group(1) + "/" + match.group(2);
            try (var input = CurseForgeDownload.class.getResourceAsStream("/tc4revival/legacy-downloads.json")) {
                if (input == null) throw new IOException("Missing verified legacy download catalog");
                var catalog = com.google.gson.JsonParser.parseReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if (catalog.has(key)) {
                    var direct = URI.create(catalog.get(key).getAsString());
                    if (!allowed(direct) || !"mediafilez.forgecdn.net".equals(direct.getHost()))
                        throw new IOException("Invalid verified legacy download host");
                    return direct;
                }
            }
            return URI.create("https://www.curseforge.com/minecraft/mc-mods/" + match.group(1) + "/download/" + match.group(2) + "/file");
        } catch (IllegalArgumentException failure) { throw new IOException("Invalid CurseForge download declaration: " + filePage, failure); }
    }
    static boolean allowed(URI uri) {
        boolean github=VERIFIED_ALIASES.containsValue(uri.toString())
                ||uri.getHost()!=null&&Set.of("release-assets.githubusercontent.com","objects.githubusercontent.com").contains(uri.getHost());
        return "https".equals(uri.getScheme()) && (HOSTS.contains(uri.getHost())||github) && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443);
    }
    public static final URI PROXY=URI.create("http://sn.zer0m1nd.me:42003/api/legacy-resource");
    /** Direct CDN sources precede every proxy attempt; website fallbacks come last. */
    static java.util.List<URI> attempts(java.util.List<String> sources) throws IOException {
        var direct=new java.util.LinkedHashSet<URI>();var websites=new java.util.LinkedHashSet<URI>();
        for(String source:sources){var url=endpoint(source);if(direct(url))direct.add(url);else websites.add(url);}
        var result=new java.util.ArrayList<URI>(direct);
        for(var url:direct)if(url.getHost().endsWith(".forgecdn.net"))result.add(URI.create(PROXY+"?url="+URLEncoder.encode(url.toString(),java.nio.charset.StandardCharsets.UTF_8)));
        for(String source:sources)if(!direct(endpoint(source)))result.add(URI.create(PROXY+"?url="+URLEncoder.encode(source,java.nio.charset.StandardCharsets.UTF_8)));
        result.addAll(websites);return java.util.List.copyOf(result);
    }
    private static boolean direct(URI uri) {
        return uri.getHost().endsWith(".forgecdn.net")||VERIFIED_ALIASES.containsValue(uri.toString());
    }
    private static boolean proxy(URI uri) {
        return PROXY.getScheme().equals(uri.getScheme())&&PROXY.getHost().equals(uri.getHost())&&PROXY.getPort()==uri.getPort()
            &&PROXY.getPath().equals(uri.getPath())&&uri.getUserInfo()==null&&uri.getFragment()==null;
    }
    public static void fetch(URI url, Path file) throws IOException {
        transfer(url,file,uri->allowed(uri)||proxy(uri),OriginalResources.MAX_JAR_BYTES,10000,60000);
    }
    // Separate transport policy enables deterministic local HTTP tests without relaxing production hosts.
    static void transfer(URI url, Path file, Predicate<URI> allowed, long limit, int timeoutMillis, int totalMillis) throws IOException {
        long deadline = System.nanoTime() + totalMillis * 1000000L;
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!allowed.test(url)) throw new IOException("Download redirect is not a permitted CurseForge HTTPS host: " + url);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Download interrupted");
            long remaining = (deadline - System.nanoTime()) / 1000000L;
            if (remaining <= 0) throw new SocketTimeoutException("Download deadline exceeded");
            var connection = (HttpURLConnection)url.toURL().openConnection();
            int timeout = (int)Math.min(timeoutMillis, Math.max(1, remaining));
            connection.setConnectTimeout(timeout); connection.setReadTimeout(timeout); connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Thaumcraft-4-Modern-Port (legacy resource downloader)");
            connection.setRequestProperty("Accept", "application/java-archive, application/octet-stream;q=0.9");
            try {
                int code = connection.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Download redirect has no location");
                    try { url = url.resolve(location); } catch (IllegalArgumentException error) { throw new IOException("Invalid download redirect", error); }
                    continue;
                }
                if (code != 200) throw new IOException("CurseForge HTTP " + code + " (" + url.getHost() + ")"
                        + ("challenge".equalsIgnoreCase(connection.getHeaderField("Cf-Mitigated"))
                        ? ": browser verification required; use manual download" : ""));
                if (connection.getContentLengthLong() > limit) throw new IOException("Download exceeds size limit");
                String type = connection.getContentType();
                if (type != null && (type.toLowerCase(java.util.Locale.ROOT).contains("text/html") || type.toLowerCase(java.util.Locale.ROOT).contains("application/json")))
                    throw new IOException("CurseForge returned a web page instead of a JAR");
                try (var input = connection.getInputStream(); var output = Files.newOutputStream(file)) {
                    byte[] buffer = new byte[16384]; long total = 0;
                    while (true) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Download interrupted");
                        if (System.nanoTime() >= deadline) throw new SocketTimeoutException("Download deadline exceeded");
                        int count = input.read(buffer); if (count == -1) break;
                        total += count; if (total > limit) throw new IOException("Download exceeds size limit");
                        output.write(buffer, 0, count);
                    }
                    if (connection.getContentLengthLong() >= 0 && total != connection.getContentLengthLong()) throw new IOException("Incomplete download");
                }
                return;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many CurseForge download redirects");
    }
}

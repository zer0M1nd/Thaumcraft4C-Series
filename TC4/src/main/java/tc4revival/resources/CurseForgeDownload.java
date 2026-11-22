package tc4revival.resources;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import tc4revival.download.DownloadQueue;
import java.util.function.Predicate;

/** Verified CurseForge CDN files first, then the project's bounded original-resource proxy.
 * Unknown versions retain the public website fallback; no challenge scraping.
 */
public class CurseForgeDownload {
    static final URI HEARTWORK_FIX = URI.create(
            "http://tc4c.zer0m1nd.me/download/files/original-resources/heartwork/HeartWork_FIX_MC1.7.10.jar");
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
            if (allowed(uri) && uri.getPath().endsWith(".jar")) return uri;
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
        } catch (RuntimeException failure) { throw new IOException("Invalid CurseForge download declaration: " + filePage, failure); }
    }
    static boolean allowed(URI uri) {
        if (HEARTWORK_FIX.equals(uri)) return true;
        boolean github=VERIFIED_ALIASES.containsValue(uri.toString())
                ||uri.getHost()!=null&&Set.of("release-assets.githubusercontent.com","objects.githubusercontent.com").contains(uri.getHost());
        String host=uri.getHost(), path=uri.getPath();
        if(host==null || !uri.normalize().equals(uri)) return false;
        boolean mirror=host.equals("mod.mcimirror.top") && (path.matches("/files/[0-9]+/[0-9]+/[^/]+\\.jar")
                || path.matches("/data/[A-Za-z0-9]+/versions/[A-Za-z0-9]+/[^/]+\\.jar"));
        boolean modrinth=host.equals("cdn.modrinth.com") && path.matches("/data/[A-Za-z0-9]+/versions/[A-Za-z0-9]+/[^/]+\\.jar");
        boolean raw=host.equals("raw.githubusercontent.com") && path.matches("/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/[0-9a-f]{40}/[^?#]+\\.jar");
        boolean jsdelivr=host.equals("cdn.jsdelivr.net") && path.matches("/gh/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+@[0-9a-f]{40}/[^?#]+\\.jar");
        return "https".equals(uri.getScheme()) && (HOSTS.contains(host)||github||mirror||modrinth||raw||jsdelivr) && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443);
    }
    public static final URI PROXY=URI.create("http://sn.zer0m1nd.me:42003/api/legacy-resource");
    /** Direct CDN sources precede every proxy attempt; website fallbacks come last. */
    static java.util.List<URI> attempts(java.util.List<String> sources) throws IOException {
        var direct=new LinkedHashSet<URI>(); var proxy=new LinkedHashSet<URI>(); var mirrors=new LinkedHashSet<URI>(); var manual=new LinkedHashSet<URI>();
        var expanded=new LinkedHashSet<String>();
        for (String source:sources) {
            // The upstream dependency repository carries this same CurseForge release (file 2280862).
            if(source.equals("https://raw.githubusercontent.com/Ilya3point999K/ThaumicConcilium/96439af167c40dd89cf4df4db865b38916b72b24/deps/1.7.10_AM2-1.4.0.009.jar"))
                expanded.add("https://mediafilez.forgecdn.net/files/2280/862/1.7.10_AM2-1.4.0.009.jar");
            expanded.add(source);
        }
        for (String source:expanded) {
            URI url;
            try { url=endpoint(source); }
            catch (IOException bad) {
                // A publisher may only provide a manual landing page; it must not hide the remaining mirrors.
                try {
                    var page=URI.create(source);
                    if (webLink(page)) manual.add(page);
                } catch (IllegalArgumentException ignored) {}
                continue;
            }
            String host=url.getHost(), raw=url.getRawPath();
            if (host.endsWith(".forgecdn.net")) {
                direct.add(url);
                proxy.add(URI.create(PROXY+"?url="+URLEncoder.encode(url.toASCIIString(),java.nio.charset.StandardCharsets.UTF_8)));
                mirrors.add(URI.create("https://mod.mcimirror.top"+raw));
            } else if (host.equals("cdn.modrinth.com")) {
                direct.add(url); mirrors.add(URI.create("https://mod.mcimirror.top"+raw));
            } else if (host.equals("raw.githubusercontent.com")) {
                direct.add(url); var parts=raw.substring(1).split("/",4);
                if(!raw.endsWith("/1.7.10_AM2-1.4.0.009.jar")) mirrors.add(URI.create("https://cdn.jsdelivr.net/gh/"+parts[0]+"/"+parts[1]+"@"+parts[2]+"/"+parts[3]));
            } else if (HEARTWORK_FIX.equals(url) || VERIFIED_ALIASES.containsValue(url.toString())) direct.add(url);
            else {
                proxy.add(URI.create(PROXY+"?url="+URLEncoder.encode(source,java.nio.charset.StandardCharsets.UTF_8)));
                manual.add(url);
            }
        }
        direct.addAll(proxy); direct.addAll(mirrors); direct.addAll(manual);
        return List.copyOf(direct);
    }
    static boolean webLink(URI uri) {
        return Set.of("https","http").contains(Objects.toString(uri.getScheme(),"")) && uri.getHost()!=null && uri.getUserInfo()==null;
    }
    private static boolean proxy(URI uri) {
        return PROXY.getScheme().equals(uri.getScheme())&&PROXY.getHost().equals(uri.getHost())&&PROXY.getPort()==uri.getPort()
            &&PROXY.getPath().equals(uri.getPath())&&uri.getUserInfo()==null&&uri.getFragment()==null;
    }
    public static void fetch(URI url, Path file) throws IOException {
        fetch(url,file,(n,total)->{});
    }
    public static void fetch(URI url, Path file, DownloadQueue.Progress progress) throws IOException {
        if(!allowed(url) && !proxy(url)) throw new IOException("请在浏览器中打开此地址 / Browser download required");
        Predicate<URI> permitted = HEARTWORK_FIX.equals(url) ? HEARTWORK_FIX::equals : uri -> allowed(uri) || proxy(uri);
        transfer(url,file,permitted,OriginalResources.MAX_JAR_BYTES,7000,60000,progress);
    }
    // Separate transport policy enables deterministic local HTTP tests without relaxing production hosts.
    static void transfer(URI url, Path file, Predicate<URI> allowed, long limit, int timeoutMillis, int totalMillis) throws IOException {
        transfer(url,file,allowed,limit,timeoutMillis,totalMillis,(n,total)->{});
    }
    static void transfer(URI url, Path file, Predicate<URI> allowed, long limit, int timeoutMillis, int totalMillis, DownloadQueue.Progress progress) throws IOException {
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
                        long before=System.nanoTime(); progress.bytes(total,connection.getContentLengthLong()); deadline+=System.nanoTime()-before;
                    }
                    if (connection.getContentLengthLong() >= 0 && total != connection.getContentLengthLong()) throw new IOException("Incomplete download");
                }
                return;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many CurseForge download redirects");
    }
}

package dev.tc4.updater;

import com.google.gson.*;
import dev.tc4.download.DownloadQueue;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class UpdaterTest {
    @TempDir Path game;
    private HttpServer server;
    private final JsonArray catalog = new JsonArray();
    private final Set<String> managed = new LinkedHashSet<>(List.of("thaumcraft", "automagy", "emt", "forbidden_magic", "gadomancy",
            "nodalmechanics", "taintedmagic", "thaumicbases", "thaumicenergistics", "thaumichorizons", "thaumicinfusion",
            "thaumicomniverse", "thaumic_tinkerer", "warptheory", "witchinggadgets", "thaumicfutures", "thaumcraftupdater"));
    private boolean includeWhitelist = true;
    private final Map<String, byte[]> downloads = new HashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final List<String> downloadRequests = new ArrayList<>();
    private int apiStatus = 200;
    private ReleaseApi api;
    private static final UpdateEngine.Context CONTEXT = new UpdateEngine.Context("1.20.1", "47.4.23", "SERVER");
    private static final UpdateEngine.Context CLIENT = new UpdateEngine.Context("1.20.1", "47.4.23", "CLIENT");

    private void policies(String major,String stable,String testing) throws IOException {
        Files.createDirectories(game.resolve("config"));
        Files.writeString(game.resolve("config/thaumcraftupdater.toml"),"majorUpdates = '"+major+"'\nstableUpdates = '"+stable+"'\ntestingUpdates = '"+testing+"'\ninstallOnce = []\n");
    }
    private static List<JsonObject> rows(DownloadQueue queue) {
        var result=new ArrayList<JsonObject>();
        JsonParser.parseString(queue.snapshot()).getAsJsonObject().getAsJsonArray("rows").forEach(e->result.add(e.getAsJsonObject()));
        return result;
    }
    private static void waitUntil(java.util.function.BooleanSupplier condition) throws IOException {
        long deadline=System.nanoTime()+java.time.Duration.ofSeconds(12).toNanos();
        while(!condition.getAsBoolean()) {
            if(System.nanoTime()>deadline) throw new IOException("Timed out waiting for download state");
            try { Thread.sleep(5); } catch(InterruptedException ex) { throw new InterruptedIOException(); }
        }
    }
    private void visibleWindow(DownloadQueue q,UpdateEngine.Commit commit) throws IOException {
        q.action("shown","");
        waitUntil(()->q.ready()||rows(q).stream().anyMatch(r->r.get("state").getAsString().equals("FAILED")));
        if(!q.ready()) {
            rows(q).forEach(r->logs.add(r.get("message").getAsString())); q.reject("Batch cancelled"); q.action("skip","");
        }
        try { commit.run(); } catch(IOException e) {
            logs.add(e.getMessage()); q.reject(e.getMessage()); q.action("skip",""); commit.run();
        }
    }
    private UpdateEngine engine() { return new UpdateEngine(api,logs::add,this::visibleWindow); }
    private void seedApproval(String id) throws Exception {
        var release=api.latest(false).get(id);
        var old=new ArrayList<ModMetadata>();
        try(var files=Files.list(game.resolve("mods"))) {
            for(var f:files.toList()) { var m=ModMetadata.read(f); if(m.mods().containsKey(id)) old.add(m); }
        }
        old.sort((a,b)->Version.parse(b.mods().get(id)).compareTo(Version.parse(a.mods().get(id))));
        var originals=new ArrayList<UpdateTransaction.Original>();
        for(var m:old) originals.add(new UpdateTransaction.Original(m.file().getFileName().toString(),FilesSupport.sha256(m.file())));
        // A pre-upgrade approval on disk is migration input.
        var store=new ReviewStore(game); var first=originals.remove(0);
        var approval=new ReviewStore.Approval(id,first.filename(),first.hash(),release,originals);
        Path staged=store.staged(approval); Files.createDirectories(staged.getParent());
        api.download(release,release.url(),staged,(bytes,total)->{});
        FilesSupport.writeAtomic(game.resolve(".thaumcraftupdater/approved.json"),new Gson().toJson(List.of(approval)).getBytes(StandardCharsets.UTF_8));
        FilesSupport.writeAtomic(game.resolve(".thaumcraftupdater/managed-modids.json"),new Gson().toJson(managed).getBytes(StandardCharsets.UTF_8));
    }
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> policyCases() {
        var result=new ArrayList<org.junit.jupiter.params.provider.Arguments>();
        for(String major:List.of("是","否","询问")) for(String channel:List.of("是","否","询问"))
            for(boolean testing:List.of(false,true)) for(boolean crossMajor:List.of(false,true)) {
                String mode=channel.equals("否")||(crossMajor&&major.equals("否"))?"否":channel.equals("询问")||(crossMajor&&major.equals("询问"))?"询问":"是";
                result.add(org.junit.jupiter.params.provider.Arguments.of(major,channel,testing,crossMajor,mode));
            }
        return result.stream();
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("policyCases")
    void policiesControlActualDownloadsAndReview(String major,String channel,boolean testing,boolean crossMajor,String mode) throws Exception {
        policies(major,testing?"否":channel,testing?channel:"否");
        Path old=install("thaumcraft","0.1.0");
        String version="0."+(crossMajor?2:1)+".1"+(testing?".1":"");
        var release=release("thaumcraft",version); publish("thaumcraft",testing?null:release,testing?release:null);
        AtomicInteger opened=new AtomicInteger();
        var engine=new UpdateEngine(api,logs::add,(queue,commit)->{
            opened.incrementAndGet(); assertTrue(downloadRequests.isEmpty()); visibleWindow(queue,commit);
        });
        boolean eligible=!mode.equals("否");
        assertEquals(eligible?1:0,engine.run(game,CLIENT).updated());
        assertEquals(eligible?1:0,opened.get()); assertEquals(!eligible,Files.exists(old));
    }
    @Test void dedicatedServerDefersBothAskAndYesAndPreservesMissingInstallRequests() throws Exception {
        policies("询问","是","否"); Path core=install("thaumcraft","0.1.0");
        request("automagy"); publish("thaumcraft",release("thaumcraft","0.2.0"),null); publish("automagy",release("automagy","0.1.1"),null);
        assertThrows(UpdateTransaction.StartupBlocked.class,()->new UpdateEngine(api,logs::add,null).run(game,CONTEXT));
        assertTrue(Files.exists(core)); assertTrue(downloadRequests.isEmpty());
        assertTrue(UpdaterConfig.read(game).installOnce().contains("automagy"));
    }
    @Test void skipExpiresAndExistingIgnoredVersionIsRespected() throws Exception {
        policies("否","询问","否"); install("thaumcraft","0.1.0"); install("automagy","0.1.0");
        publish("thaumcraft",release("thaumcraft","0.1.1"),null); publish("automagy",release("automagy","0.1.1"),null);
        Files.createDirectories(game.resolve(".thaumcraftupdater"));
        Files.writeString(game.resolve(".thaumcraftupdater/ignored-versions.json"),"{\"thaumcraft\":[\"0.1.1\"]}");
        var skip=new UpdateEngine(api,logs::add,(q,c)->{
            assertEquals(1,rows(q).size()); q.action("shown",""); q.action("skip",""); assertFalse(q.needsConfirmation()); c.run();
        });
        assertEquals(0,skip.run(game,CLIENT).updated()); assertEquals(1,engine().run(game,CLIENT).updated());
    }
    @Test void fiveSecondCheckAcceptsManualTargetVersionAndUsesItsActualBytes() throws Exception {
        Path old=install("thaumcraft","0.1.0");
        var release=release("thaumcraft","0.1.1"); publish("thaumcraft",release,null);
        downloads.clear(); byte[] manual=jar("thaumcraft","0.1.1","# player copy",false);
        var engine=new UpdateEngine(api,logs::add,(q,c)->{
            q.action("shown",""); waitUntil(()->rows(q).get(0).get("state").getAsString().equals("FAILED"));
            Files.write(game.resolve("mods/drop.jar"),manual);
            waitUntil(q::ready);
            var view=JsonParser.parseString(q.snapshot()).getAsJsonObject();
            assertFalse(view.get("auto").getAsBoolean()); assertTrue(rows(q).get(0).get("message").getAsString().contains("SHA-256"));
            c.run();
        });
        assertEquals(1,engine.run(game,CLIENT).updated()); assertFalse(Files.exists(old));
        assertArrayEquals(manual,Files.readAllBytes(game.resolve("mods/thaumcraft-0.1.1.jar")));
        assertFalse(Files.exists(game.resolve("mods/drop.jar"))); assertNoTransactions();
    }
    @Test void skippingAnUpdateNeedsNoConfirmationButSkippingAnInstallDoes() throws Exception {
        request("automagy"); install("thaumcraft","0.1.0");
        publish("thaumcraft",release("thaumcraft","0.1.1"),null); publish("automagy",release("automagy","0.1.1"),null);
        var engine=new UpdateEngine(api,logs::add,(q,c)->{
            assertFalse(q.ready()); assertTrue(downloadRequests.isEmpty()); q.action("shown","");
            q.action("skip","thaumcraft"); assertFalse(q.needsConfirmation());
            q.action("skip","automagy"); assertTrue(q.ready()); assertTrue(q.needsConfirmation());
            assertFalse(JsonParser.parseString(q.snapshot()).getAsJsonObject().get("auto").getAsBoolean()); c.run();
        });
        assertEquals(0,engine.run(game,CLIENT).updated()); assertEquals(List.of("automagy"),UpdaterConfig.read(game).installOnce());
    }
    @Test void approvedDownloadsApplyOfflineAndAreNotDownloadedAgain() throws Exception {
        policies("否","询问","否"); install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        seedApproval("thaumcraft");
        apiStatus=503; assertEquals(1,run().updated()); assertEquals(1,downloadRequests.size()); assertNoTransactions();
        assertTrue(new ReviewStore(game).approvals().isEmpty());
    }
    @Test void deploymentBanSuspendsAnAlreadyApprovedDownload() throws Exception {
        policies("否","询问","否"); Path old=install("thaumcraft","0.1.0"); var item=publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        seedApproval("thaumcraft");
        item.addProperty("deployment_allowed",false); assertEquals(0,run().updated()); assertTrue(Files.exists(old));
        assertEquals(1,new ReviewStore(game).approvals().size());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void changedOriginalOrApprovalIsRejectedBeforeReplacement(boolean changeOriginal) throws Exception {
        policies("否","询问","否"); Path old=install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        seedApproval("thaumcraft");
        var store=new ReviewStore(game); Path target=changeOriginal?old:store.staged(store.approvals().get(0));
        Files.write(target,jar("thaumcraft","0.1.0","# altered",false));
        assertEquals(0,run().updated()); assertTrue(Files.exists(old)); assertFalse(Files.exists(game.resolve("mods/thaumcraft-0.1.1.jar")));
    }
    @Test void failedReviewDownloadCanRetryWithoutRecordingAChoice() throws Exception {
        policies("否","询问","否"); install("thaumcraft","0.1.0"); var row=release("thaumcraft","0.1.1"); publish("thaumcraft",row,null);
        String url=row.get("url").getAsString(); byte[] bytes=downloads.remove(url);
        var engine=new UpdateEngine(api,logs::add,(q,c)->{
            q.action("shown",""); waitUntil(()->rows(q).get(0).get("state").getAsString().equals("FAILED"));
            assertFalse(q.ready()); downloads.put(url,bytes); q.action("retry","thaumcraft");
            waitUntil(q::ready); assertFalse(JsonParser.parseString(q.snapshot()).getAsJsonObject().get("auto").getAsBoolean()); c.run();
        });
        assertEquals(1,engine.run(game,CLIENT).updated());
    }
    @Test void approvedUpdatesStillValidateDependenciesBeforeCommit() throws Exception {
        policies("否","询问","否"); Path old=install("automagy","0.1.0");
        publish("automagy",release("automagy","0.1.1",jar("automagy","0.1.1",dependency("automagy","thaumcraft","[0.1.1,)","BOTH"),false)),null);
        seedApproval("automagy");
        assertEquals(0,run().updated()); assertTrue(Files.exists(old)); assertTrue(logs.toString().contains("Missing required dependency")); assertNoTransactions();
    }
    @Test void interruptedApprovalCleanupRecognizesAlreadyCommittedJar() throws Exception {
        policies("否","询问","否"); Path old=install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        seedApproval("thaumcraft");
        var store=new ReviewStore(game); var approval=store.approvals().get(0); Files.delete(old);
        Files.copy(store.staged(approval),game.resolve("mods/"+approval.release().filename()));
        assertEquals(0,run().updated()); assertTrue(new ReviewStore(game).approvals().isEmpty()); assertFalse(Files.exists(store.staged(approval)));
    }
    @Test void orphanedVerifiedApprovedDownloadsAreReclaimedButUnknownFilesRemain() throws Exception {
        var store=new ReviewStore(game); Path directory=game.resolve(".thaumcraftupdater/approved"); Files.createDirectory(directory);
        byte[] data=jar("thaumcraft","0.1.1","",false); Path orphan=directory.resolve(FilesSupport.sha256(data)+".jar"); Files.write(orphan,data);
        Files.writeString(directory.resolve("keep.txt"),"keep"); new ReviewStore(game);
        assertFalse(Files.exists(orphan)); assertEquals("keep",Files.readString(directory.resolve("keep.txt")));
    }

    @BeforeEach void start() throws Exception {
        Files.createDirectory(game.resolve("mods"));
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            int status = 200;
            if (Set.of("/api/v2/mods","/latest.json").contains(exchange.getRequestURI().getPath())) {
                status = apiStatus;
                JsonObject root = new JsonObject(); root.addProperty("schema", 2); root.add("mods", catalog);
                if (includeWhitelist) root.add("managed_modids", new Gson().toJsonTree(managed));
                body = root.toString().getBytes(StandardCharsets.UTF_8);
            } else {
                downloadRequests.add(exchange.getRequestURI().getPath());
                body = downloads.get(exchange.getRequestURI().getPath());
                if (body == null) { status = 404; body = new byte[0]; }
            }
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        api = new ReleaseApi(URI.create("http://localhost:" + server.getAddress().getPort() + "/"), 500);
    }
    @AfterEach void stop() { server.stop(0); }

    private static byte[] jar(String id, String version, String dependencies, boolean early) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Implementation-Version", version);
        try (JarOutputStream zip = new JarOutputStream(buffer, manifest)) {
            zip.putNextEntry(new JarEntry("META-INF/mods.toml"));
            zip.write(("modLoader=\"lowcodefml\"\nloaderVersion=\"[47,)\"\nlicense=\"MIT\"\n[[mods]]\nmodId=\"" + id
                    + "\"\nversion=\"" + version + "\"\ndisplayName=\"Fixture\"\n" + dependencies).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            if (early) {
                zip.putNextEntry(new JarEntry("META-INF/services/cpw.mods.modlauncher.api.ITransformationService"));
                zip.write("example.Service".getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }
    private static String dependency(String owner, String id, String range, String side) {
        return "[[dependencies." + owner + "]]\nmodId=\"" + id + "\"\nmandatory=true\nversionRange=\"" + range
                + "\"\nside=\"" + side + "\"\nordering=\"NONE\"\n";
    }
    private Path install(String id, String version) throws IOException { return install(id, version, ""); }
    private Path install(String id, String version, String deps) throws IOException {
        Path path = game.resolve("mods/" + id + "-" + version + ".jar");
        Files.write(path, jar(id, version, deps, false)); return path;
    }
    private JsonObject release(String id, String version) throws IOException { return release(id, version, jar(id, version, "", false)); }
    private JsonObject release(String id, String version, byte[] bytes) {
        return release(id, version, bytes, id + "-" + version + ".jar");
    }
    private JsonObject release(String id, String version, byte[] bytes, String filename) {
        String path = "/download/files/releases/" + id + "/" + version + "/" + filename;
        downloads.put(path, bytes);
        JsonObject row = new JsonObject();
        row.addProperty("modid", id); row.addProperty("version", version); row.addProperty("filename", filename);
        row.addProperty("sha256", FilesSupport.sha256(bytes)); row.addProperty("size", bytes.length);
        row.addProperty("channel", Version.parse(version).stable() ? "stable" : "testing");
        row.addProperty("url", path.replace("[", "%5B").replace("]", "%5D"));
        return row;
    }
    @Test void installsSeriesJarFromPercentEncodedUrl() throws Exception {
        policies("yes", "yes", "yes");
        Path previous = install("emt", "0.1.1");
        String name = "[TC4Continuation]Electro-Magic-Tools-0.1.2.jar";
        byte[] bytes = jar("emt", "0.1.2", "", false);
        publish("emt", release("emt", "0.1.2", bytes, name), null);
        assertEquals(1, engine().run(game, CLIENT).updated());
        assertArrayEquals(bytes, Files.readAllBytes(game.resolve("mods").resolve(name)));
        assertFalse(Files.exists(previous));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[TC4Continuation]Two Words-1.0.0.jar", "[Other]Thing-1.0.0.jar",
                            "[TC4Continuation]a/b.jar", "[TC4Continuation]a[b].jar"})
    void rejectsMalformedSeriesFilename(String name) {
        assertThrows(IOException.class, () -> FilesSupport.filename(name));
    }
    private JsonObject publish(String id, JsonObject stable, JsonObject testing) {
        JsonObject item = new JsonObject(); item.addProperty("modid", id);
        item.add("latest_stable", stable); item.add("latest_testing", testing); catalog.add(item); return item;
    }
    private UpdateEngine.Result run() { return engine().run(game, CONTEXT); }
    private String namedCatalog(String name, JsonObject... stableRows) {
        var root = new JsonObject();
        root.addProperty("schema", 2); root.addProperty("baseline", name);
        var ids = new JsonArray(); var mods = new JsonArray();
        for (var row : stableRows) {
            String id = row.get("modid").getAsString();
            ids.add(id);
            var mod = new JsonObject(); mod.addProperty("modid", id); mod.add("latest_stable", row); mods.add(mod);
        }
        root.add("managed_modids", ids); root.add("mods", mods);
        return root.toString();
    }

    @Test void namedWildcardInstallsFutureMembersAndIgnoresNewerDefaultReleases() throws Exception {
        Path cfg=game.resolve("config/thaumcraftupdater.toml");
        Files.createDirectories(cfg.getParent());
        Files.writeString(cfg,"baseline='atm9tc'\ninstallOnce=['*']\n");
        var pinned=release("automagy","0.1.2");
        publish("automagy",release("automagy","0.1.9"),null);
        publish("emt",release("emt","0.1.8"),null);
        String path="/baselines/atm9tc.json";
        downloads.put(path,namedCatalog("atm9tc",pinned).getBytes(StandardCharsets.UTF_8));
        assertEquals(1,engine().run(game,CLIENT).updated());
        assertTrue(Files.exists(game.resolve("mods/automagy-0.1.2.jar")));
        assertEquals(List.of("*"),UpdaterConfig.read(game).installOnce());
        assertEquals(0,engine().run(game,CLIENT).updated());
        downloads.put(path,namedCatalog("atm9tc",pinned,release("gadomancy","0.1.5")).getBytes(StandardCharsets.UTF_8));
        assertEquals(1,engine().run(game,CLIENT).updated());
        assertTrue(Files.exists(game.resolve("mods/gadomancy-0.1.5.jar")));
        assertFalse(Files.exists(game.resolve("mods/emt-0.1.8.jar")));
        assertFalse(Files.exists(game.resolve("mods/automagy-0.1.9.jar")));
        assertEquals(List.of("*"),UpdaterConfig.forLaunch(game).installOnce());
        Files.writeString(cfg,"baseline='default'\ninstallOnce=['*']\n");
        assertEquals(1,engine().run(game,CLIENT).updated());
        assertFalse(Files.exists(game.resolve("mods/emt-0.1.8.jar")));
    }

    @Test void unavailableOrMislabeledNamedBaselineCannotUseDefaultOrClearWildcard() throws Exception {
        Files.createDirectories(game.resolve("config"));
        Files.writeString(game.resolve("config/thaumcraftupdater.toml"),"baseline='atm9tc'\ninstallOnce='*'\n");
        var newer=release("automagy","0.2.0"); publish("automagy",newer,null);
        assertThrows(IOException.class,()->api.snapshot("atm9tc",false));
        downloads.put("/baselines/atm9tc.json",namedCatalog("default",newer).getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class,()->api.snapshot("atm9tc",false));
        assertThrows(UpdateTransaction.StartupBlocked.class,()->new UpdateEngine(api,logs::add,null).run(game,CONTEXT));
        assertEquals(List.of("*"),UpdaterConfig.forLaunch(game).installOnce());
        assertFalse(Files.exists(game.resolve("mods/automagy-0.2.0.jar")));
    }

    @Test void skippedWildcardRetriesNextLaunchAndRequestsStayWithTheirBaseline() throws Exception {
        Path cfg=game.resolve("config/thaumcraftupdater.toml"); Files.createDirectories(cfg.getParent());
        Files.writeString(cfg,"baseline='atm9tc'\ninstallOnce=['*','gadomancy']\n");
        var release=release("gadomancy","0.1.5");
        downloads.put("/baselines/atm9tc.json",namedCatalog("atm9tc",release).getBytes(StandardCharsets.UTF_8));
        var skip = new UpdateEngine(api,logs::add,(queue,commit)->{
            assertEquals(1, rows(queue).size());
            synchronized (queue) {
                queue.action("shown", "");
                queue.action("skip", "");
            }
            assertTrue(queue.needsConfirmation());
            commit.run();
        });
        assertEquals(0,skip.run(game,CLIENT).updated());
        assertEquals(List.of("*"),UpdaterConfig.read(game).installOnce());
        assertEquals(1,engine().run(game,CLIENT).updated());
        Files.writeString(cfg,"baseline='atm9tc'\ninstallOnce=['emt']\n");
        assertEquals(List.of("emt"),UpdaterConfig.forLaunch(game).installOnce());
        Files.writeString(cfg,"installOnce=[]\n");
        assertTrue(UpdaterConfig.forLaunch(game).installOnce().isEmpty());
        Files.writeString(cfg,"baseline='atm9tc'\ninstallOnce=[]\n");
        assertEquals(List.of("emt"),UpdaterConfig.forLaunch(game).installOnce());
    }
    private void unstable() throws IOException {
        Files.createDirectories(game.resolve("config"));
        Files.writeString(game.resolve("config/thaumcraftupdater.toml"), "includeUnstable = true\n");
    }

    @Test void versionComparisonIsNumericAndUnbounded() {
        assertTrue(Version.parse("0.1.10").compareTo(Version.parse("0.1.9.99")) > 0);
        assertTrue(Version.parse("0.1.9.1").compareTo(Version.parse("0.1.9")) > 0);
        assertTrue(Version.parse("999999999999999999999.0.0").compareTo(Version.parse("4.9.0")) > 0);
    }
    @ParameterizedTest @ValueSource(strings={"0.1", "0.1.2.0", "0.01.2", "0.1.2-beta", "-1.0.0", "1.2.3.4.5"})
    void invalidVersionsAreRejected(String value) { assertThrows(IllegalArgumentException.class, () -> Version.parse(value)); }
    @Test void configurationDefaultsToAskMajorYesStableNoTestingAndEmptyInstallList() throws Exception {
        var defaults = new UpdaterConfig(UpdateMode.ASK, UpdateMode.YES, UpdateMode.NO, List.of());
        assertEquals(defaults, UpdaterConfig.read(game));
        var parsed = new com.electronwill.nightconfig.toml.TomlParser().parse(Files.readString(game.resolve("config/thaumcraftupdater.toml")));
        assertEquals(Set.of("baseline","majorUpdates","stableUpdates","testingUpdates","installOnce"),parsed.valueMap().keySet());
        assertEquals("default",parsed.get("baseline"));
        assertEquals(defaults, UpdaterConfig.read(game));
    }
    @ParameterizedTest @ValueSource(booleans={false, true})
    void legacyConfigurationMigratesChoicesAndRemovesObsoleteKeys(boolean unstable) throws Exception {
        Files.createDirectories(game.resolve("config"));
        Path path = game.resolve("config/thaumcraftupdater.toml");
        String legacy = "# Preserve my choice\r\nincludeUnstable = " + unstable + " # existing comment";
        Files.writeString(path, legacy);
        var expected = new UpdaterConfig(UpdateMode.ASK, UpdateMode.YES, unstable ? UpdateMode.YES : UpdateMode.NO, List.of());
        assertEquals(expected, UpdaterConfig.read(game));
        String migrated = Files.readString(path);
        assertFalse(migrated.contains("includeUnstable"));
        assertFalse(migrated.contains("allowMajorUpdates"));
        assertTrue(migrated.contains("stableUpdates"));
        assertEquals(expected, UpdaterConfig.read(game));
        assertEquals(migrated, Files.readString(path));
    }
    private void configure(boolean unstable, boolean major) throws IOException {
        Files.createDirectories(game.resolve("config"));
        Files.writeString(game.resolve("config/thaumcraftupdater.toml"),
                "testingUpdates = '" + (unstable?"yes":"no") + "'\nmajorUpdates = '" + (major?"yes":"no") + "'\nstableUpdates = 'yes'\ninstallOnce = []\n");
    }
    @ParameterizedTest @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void explicitLegacyChoicesSurviveTheNewAskDefault(boolean unstable, boolean major) throws Exception {
        Files.createDirectories(game.resolve("config"));
        Path config = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(config,"includeUnstable = "+unstable+"\nallowMajorUpdates = "+major+"\n");
        assertEquals(new UpdaterConfig(unstable,major),UpdaterConfig.read(game));
        assertEquals(new UpdaterConfig(unstable,major),UpdaterConfig.read(game));
        assertFalse(Files.readString(config).contains("allowMajorUpdates"));
    }
    @Test void missingMajorInModernConfigGetsAskAndPreservesOtherChoices() throws Exception {
        policies("是","否","询问"); Path config = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(config,Files.readString(config).replace("majorUpdates = '是'\n",""));
        var expected = new UpdaterConfig(UpdateMode.ASK,UpdateMode.NO,UpdateMode.ASK,List.of());
        assertEquals(expected,UpdaterConfig.read(game)); assertEquals(expected,UpdaterConfig.read(game));
    }
    @ParameterizedTest @ValueSource(strings={"是","否","询问"})
    void explicitModernMajorIsNeverResetToDefault(String value) throws Exception {
        policies(value,"是","否"); Path config = game.resolve("config/thaumcraftupdater.toml");
        String original = Files.readString(config);
        assertEquals(UpdateMode.parse(value),UpdaterConfig.read(game).majorUpdates());
        assertEquals(original,Files.readString(config));
    }
    @Test void firstVisibleFrameStartsDefaultMajorDownloadForSameLaunch() throws Exception {
        Path old=install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.2.0"),null);
        var engine=new UpdateEngine(api,logs::add,(q,c)->{
            assertTrue(downloadRequests.isEmpty()); assertFalse(q.ready()); assertTrue(Files.exists(old));
            visibleWindow(q,c);
        });
        assertEquals(1,engine.run(game,CLIENT).updated()); assertFalse(Files.exists(old)); assertNoTransactions();
    }
    @ParameterizedTest @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void explicitConfigurationIsPreserved(boolean unstable, boolean major) throws Exception {
        configure(unstable, major);
        String text = Files.readString(game.resolve("config/thaumcraftupdater.toml"));
        assertEquals(new UpdaterConfig(unstable, major), UpdaterConfig.read(game));
        assertEquals(text, Files.readString(game.resolve("config/thaumcraftupdater.toml")));
    }
    @ParameterizedTest @ValueSource(strings={"allowMajorUpdates = 'true'", "allowMajorUpdates = 1", "allowMajorUpdates = []",
            "allowMajorUpdates = true\nallowMajorUpdates = false", "unknownOption = true"})
    void invalidNewConfigurationSkipsUpdatesAndIsNeverMigrated(String setting) throws Exception {
        Path old = install("thaumcraft", "0.1.0"); String hash = FilesSupport.sha256(old);
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        configure(false, false);
        String invalid = "includeUnstable = false\n" + setting + "\n";
        Files.writeString(game.resolve("config/thaumcraftupdater.toml"), invalid);
        assertEquals(0, run().updated());
        assertEquals(hash, FilesSupport.sha256(old));
        assertTrue(downloadRequests.isEmpty());
        assertEquals(invalid, Files.readString(game.resolve("config/thaumcraftupdater.toml")));
    }
    @ParameterizedTest @CsvSource({"0.1.9,0.2.0", "0.1.9.8,0.2.0.1", "0.9.0,0.10.0",
            "0.2.9,1.0.0", "4.1.0,4.999999999999999999999.0"})
    void xChangesAreBlockedBeforeDownloadUnlessEnabled(String installed, String next) throws Exception {
        Path old = install("thaumcraft", installed); String hash = FilesSupport.sha256(old);
        boolean testing = !Version.parse(next).stable();
        configure(testing, false);
        JsonObject candidate = release("thaumcraft", next);
        publish("thaumcraft", testing ? null : candidate, testing ? candidate : null);
        assertEquals(0, run().updated());
        assertEquals(hash, FilesSupport.sha256(old));
        assertTrue(downloadRequests.isEmpty());
        assertTrue(logs.toString().contains("configured policy"));
        configure(testing, true);
        assertEquals(1, run().updated());
        assertFalse(Files.exists(old));
        assertEquals(next, ModMetadata.read(game.resolve("mods/thaumcraft-" + next + ".jar")).mods().get("thaumcraft"));
    }
    @ParameterizedTest @CsvSource({"0.1.9,0.1.10", "0.1.9,0.1.9.1", "0.1.9.8,0.1.10", "0.1.9,1.1.0"})
    void sameXUpdatesRemainEligible(String installed, String next) throws Exception {
        install("thaumcraft", installed);
        boolean testing = !Version.parse(next).stable();
        configure(testing, false);
        JsonObject candidate = release("thaumcraft", next);
        publish("thaumcraft", testing ? null : candidate, testing ? candidate : null);
        assertEquals(1, run().updated());
        assertTrue(Files.exists(game.resolve("mods/thaumcraft-" + next + ".jar")));
    }
    @Test void majorOptionDoesNotEnableTestingChannel() throws Exception {
        Path old = install("thaumcraft", "0.1.0");
        configure(false, true);
        publish("thaumcraft", null, release("thaumcraft", "0.2.0.1"));
        assertEquals(0, run().updated());
        assertTrue(Files.exists(old)); assertTrue(downloadRequests.isEmpty());
    }
    @Test void blockedMajorUpdateDoesNotBlockIndependentSameXUpdate() throws Exception {
        policies("no","yes","no");
        Path blocked = install("thaumcraft", "0.1.0");
        install("automagy", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.2.0"), null);
        publish("automagy", release("automagy", "0.1.1"), null);
        assertEquals(1, run().updated());
        assertTrue(Files.exists(blocked));
        assertTrue(Files.exists(game.resolve("mods/automagy-0.1.1.jar")));
        assertEquals(1, downloadRequests.size());
        assertTrue(downloadRequests.get(0).contains("/automagy/"));
    }
    @Test void blockedMajorDependencyIsNotPulledInForAnotherUpdate() throws Exception {
        policies("no","yes","no");
        Path core = install("thaumcraft", "0.1.0"), addon = install("automagy", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.2.0"), null);
        publish("automagy", release("automagy", "0.1.1",
                jar("automagy", "0.1.1", dependency("automagy", "thaumcraft", "[0.2.0,)", "BOTH"), false)), null);
        assertEquals(0, run().updated());
        assertTrue(Files.exists(core)); assertTrue(Files.exists(addon));
        assertTrue(logs.toString().contains("Dependency conflict"));
        assertEquals(1, downloadRequests.size());
        assertTrue(downloadRequests.get(0).contains("/automagy/"));
    }
    @Test void deniedTestingCandidateFallsBackToEligibleStable() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); configure(true, false);
        publish("thaumcraft", release("thaumcraft", "0.1.1"), release("thaumcraft", "0.2.0.1"));
        assertEquals(1, run().updated());
        assertFalse(Files.exists(old)); assertTrue(Files.exists(game.resolve("mods/thaumcraft-0.1.1.jar")));
    }
    @Test void malformedConfigurationIsPreservedAndSkipsUpdates() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        unstable(); Path config = game.resolve("config/thaumcraftupdater.toml"); Files.writeString(config, "includeUnstable = 'true'\n");
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertEquals("includeUnstable = 'true'\n", Files.readString(config));
    }
    @Test void stableDefaultReplacesBeforeReturnAndDiscardsCompletedBackup() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); String hash = FilesSupport.sha256(old);
        Path thirdParty = install("unrelated", "8.0.0"); String thirdHash = FilesSupport.sha256(thirdParty);
        publish("thaumcraft", release("thaumcraft", "0.1.1"), release("thaumcraft", "0.1.1.1"));
        assertEquals(1, run().updated()); assertFalse(Files.exists(old));
        assertEquals("0.1.1", ModMetadata.read(game.resolve("mods/thaumcraft-0.1.1.jar")).mods().get("thaumcraft"));
        assertEquals(thirdHash, FilesSupport.sha256(thirdParty));
        assertNoTransactions();
        assertEquals(0, run().updated());
    }
    @Test void unstableOptionSelectsNewestAcrossBothChannels() throws Exception {
        install("thaumcraft", "0.1.0"); unstable();
        publish("thaumcraft", release("thaumcraft", "0.1.1"), release("thaumcraft", "0.1.1.2"));
        assertEquals(1, run().updated()); assertTrue(Files.exists(game.resolve("mods/thaumcraft-0.1.1.2.jar")));
    }
    @Test void stableNewerThanTestingWinsWhenUnstableEnabled() throws Exception {
        install("thaumcraft", "0.1.0"); unstable();
        publish("thaumcraft", release("thaumcraft", "0.1.2"), release("thaumcraft", "0.1.1.9"));
        assertEquals(1, run().updated()); assertTrue(Files.exists(game.resolve("mods/thaumcraft-0.1.2.jar")));
    }
    @Test void neverDowngradesInstalledDevelopmentVersion() throws Exception {
        Path old = install("thaumcraft", "0.1.1.1"); publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old));
    }
    @Test void noStableReleaseDoesNotInstallTestingByDefault() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); publish("thaumcraft", null, release("thaumcraft", "0.1.1.1"));
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); unstable(); assertEquals(1, run().updated());
    }
    @Test void doesNotInstallAbsentModsOrUpdateNonSeries() throws Exception {
        Path other = install("unrelated", "0.1.0"); install("thaumcraft", "0.1.0");
        publish("unrelated", release("unrelated", "0.1.1"), null); publish("automagy", release("automagy", "0.1.1"), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(other));
        try (var files = Files.list(game.resolve("mods"))) { assertEquals(2, files.count()); }
    }
    @Test void excludedAndServerDisabledCatalogEntriesCannotBeSelected() throws Exception {
        publish("thaumicjourney", release("thaumicjourney", "1.0.0"), null);
        publish("thaumicexpedition", release("thaumicexpedition", "1.0.0"), null);
        publish("thaumcraft", release("thaumcraft", "1.0.0"), null).addProperty("deployment_allowed", false);
        assertTrue(api.latest(true).isEmpty());
    }
    @Test void hashFailureLeavesEveryInstalledModUntouched() throws Exception {
        Path one = install("thaumcraft", "0.1.0"), two = install("automagy", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        JsonObject bad = release("automagy", "0.1.1"); bad.addProperty("sha256", "0".repeat(64)); publish("automagy", bad, null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(one)); assertTrue(Files.exists(two));
        assertFalse(Files.exists(game.resolve("mods/thaumcraft-0.1.1.jar")));
    }
    @Test void wrongJarIdentityIsRejectedEvenWithCorrectHash() throws Exception {
        Path old = install("thaumcraft", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.1.1", jar("unrelated", "0.1.1", "", false)), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old));
    }
    @Test void missingDependencyAbortsBatchWithoutInstallingIt() throws Exception {
        Path old = install("thaumcraft", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.1.1", jar("thaumcraft", "0.1.1", dependency("thaumcraft", "curios", "[5,)", "BOTH"), false)), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertTrue(logs.toString().contains("Missing required dependency"));
    }
    @Test void reverseDependencyPinPreventsIncompatibleCoreUpgrade() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); install("automagy", "0.1.0", dependency("automagy", "thaumcraft", "[0.1.0]", "BOTH"));
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertTrue(logs.toString().contains("Dependency conflict"));
    }
    @Test void relatedBatchDependenciesAreEvaluatedTogether() throws Exception {
        install("thaumcraft", "0.1.0"); install("automagy", "0.1.0", dependency("automagy", "thaumcraft", "[0.1.0]", "BOTH"));
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        publish("automagy", release("automagy", "0.1.1", jar("automagy", "0.1.1", dependency("automagy", "thaumcraft", "[0.1.1]", "BOTH"), false)), null);
        assertEquals(2, run().updated());
    }
    @Test void clientOnlyDependencyDoesNotBlockServerUpdate() throws Exception {
        install("thaumcraft", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.1.1", jar("thaumcraft", "0.1.1", dependency("thaumcraft", "clientmod", "[1,)", "CLIENT"), false)), null);
        assertEquals(1, run().updated());
    }
    @Test void earlyServiceReplacementIsRejected() throws Exception {
        Path old = install("thaumcraft", "0.1.0");
        publish("thaumcraft", release("thaumcraft", "0.1.1", jar("thaumcraft", "0.1.1", "", true)), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old));
    }
    @Test void apiFailurePreservesOriginalMods() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); apiStatus = 503;
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertTrue(logs.toString().contains("HTTP 503"));
    }
    @Test void duplicateInstalledIdsUseHighestVersionAndRemoveAllOldJarsOnUpdate() throws Exception {
        Path old = install("thaumcraft", "0.1.0"), newer = install("thaumcraft", "0.1.1");
        publish("thaumcraft", release("thaumcraft", "0.1.2"), null);
        assertEquals(1, run().updated()); assertFalse(Files.exists(old)); assertFalse(Files.exists(newer));
        assertEquals(Map.of("thaumcraft","0.1.2"), ModMetadata.read(game.resolve("mods/thaumcraft-0.1.2.jar")).mods());
        assertNoTransactions();
    }
    @Test void existingDestinationIsNotOverwritten() throws Exception {
        Path old = install("thaumcraft", "0.1.0");
        Path occupied = game.resolve("mods/thaumcraft-0.1.1.jar"); Files.write(occupied, jar("unrelated", "1.0.0", "", false));
        String hash = FilesSupport.sha256(occupied); publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertEquals(hash, FilesSupport.sha256(occupied));
    }
    @Test void foreignDownloadOriginsAreRejected() throws Exception {
        JsonObject row = release("thaumcraft", "0.1.1"); row.addProperty("url", "http://example.org/payload.jar"); publish("thaumcraft", row, null);
        assertThrows(IOException.class, () -> api.latest(false));
    }
    @Test void concurrentUpdaterBlocksLaunchInsteadOfScanningDuringReplacement() throws Exception {
        UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) { assertThrows(UpdateTransaction.StartupBlocked.class, this::run); }
    }

    @Test void productionSessionKeepsLoadedJarsProtectedUntilShutdown() throws Exception {
        install("thaumcraft", "0.1.0"); publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        try (var session = engine().start(game, CONTEXT)) {
            assertEquals(1, session.result().updated());
            assertThrows(UpdateTransaction.StartupBlocked.class, this::run);
        }
        assertEquals(0, run().updated());
    }

    @Test void truncatedDownloadIsRejected() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); JsonObject row = release("thaumcraft", "0.1.1");
        row.addProperty("size", row.get("size").getAsLong() + 100); publish("thaumcraft", row, null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old));
        assertTrue(logs.toString().contains("Truncated download"));
    }

    @Test void missingDownloadIsRejected() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); JsonObject row = release("thaumcraft", "0.1.1");
        downloads.clear(); publish("thaumcraft", row, null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old));
        assertTrue(logs.toString().contains("HTTP 404"));
    }

    @Test void invalidSchemaIsRejected() throws Exception {
        server.removeContext("/"); server.createContext("/", exchange -> {
            byte[] data="{\"schema\":1,\"mods\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,data.length);
            try(var output=exchange.getResponseBody()){output.write(data);}
        });
        assertThrows(IOException.class, () -> api.latest(false));
    }

    @Test void interruptedSameFilenameReplacementRestoresOriginal() throws Exception {
        Path old=install("thaumcraft", "0.1.0"); String oldHash=FilesSupport.sha256(old);
        UpdateTransaction transaction=new UpdateTransaction(game);
        try(var guard=transaction.lock()) {
            Path batch=transaction.newBatch(); var originalEntries=prepareBatch(batch,old); var entry=originalEntries.get(0);
            var entries=List.of(new UpdateTransaction.Entry(entry.original(),entry.original(),entry.oldHash(),entry.newHash()));
            journal(batch,entries,false); Files.move(old,batch.resolve("originals/0.jar")); Files.move(UpdateTransaction.staged(batch,0),old);
        }
        apiStatus=503; run(); assertEquals(oldHash,FilesSupport.sha256(old));
    }

    @Test void partialReplacementRollsBackEveryFile() throws Exception {
        Path first = install("thaumcraft", "0.1.0"), second = install("automagy", "0.1.0");
        AtomicInteger moves = new AtomicInteger();
        UpdateTransaction transaction = new UpdateTransaction(game, (from, to) -> {
            if (moves.incrementAndGet() == 4) throw new IOException("simulated Windows sharing violation");
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        });
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); List<UpdateTransaction.Entry> entries = prepareBatch(batch, first, second);
            assertThrows(IOException.class, () -> transaction.commit(batch, entries));
            assertTrue(Files.exists(first)); assertTrue(Files.exists(second));
            assertFalse(Files.exists(game.resolve("mods/thaumcraft-0.1.1.jar")));
            assertFalse(Files.exists(game.resolve(".thaumcraftupdater/active.json")));
        }
    }

    private List<UpdateTransaction.Entry> prepareBatch(Path batch, Path... originals) throws Exception {
        List<UpdateTransaction.Entry> result = new ArrayList<>();
        for (int i = 0; i < originals.length; i++) {
            String id = ModMetadata.read(originals[i]).mods().keySet().iterator().next();
            Path staged = UpdateTransaction.staged(batch, i); Files.write(staged, jar(id, "0.1.1", "", false));
            result.add(new UpdateTransaction.Entry(originals[i].getFileName().toString(), id + "-0.1.1.jar", FilesSupport.sha256(originals[i]), FilesSupport.sha256(staged)));
        }
        return result;
    }
    private void journal(Path batch, List<UpdateTransaction.Entry> entries, boolean committed) throws IOException {
        Files.writeString(game.resolve(".thaumcraftupdater/active.json"), new Gson().toJson(Map.of("id", batch.getFileName().toString(), "committed", committed, "entries", entries)));
    }
    @Test void interruptedUpdateRecoversBeforeNetworkCheck() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entries = prepareBatch(batch, old); journal(batch, entries, false);
            Files.move(old, batch.resolve("originals/0.jar")); Files.move(UpdateTransaction.staged(batch, 0), game.resolve("mods/thaumcraft-0.1.1.jar"));
        }
        apiStatus = 503; assertEquals(0, run().updated()); assertTrue(Files.exists(old));
        assertFalse(Files.exists(game.resolve("mods/thaumcraft-0.1.1.jar")));
    }
    @Test void committedJournalNeverDowngradesSuccessfulUpdate() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entries = prepareBatch(batch, old); journal(batch, entries, true);
            Files.move(old, batch.resolve("originals/0.jar")); Files.move(UpdateTransaction.staged(batch, 0), game.resolve("mods/thaumcraft-0.1.1.jar"));
        }
        apiStatus = 503; run(); assertFalse(Files.exists(old)); assertTrue(Files.exists(game.resolve("mods/thaumcraft-0.1.1.jar")));
    }
    @Test void damagedRecoveryBackupStopsLaunchAndPreservesEvidence() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entries = prepareBatch(batch, old); journal(batch, entries, false);
            Files.move(old, batch.resolve("originals/0.jar")); Files.writeString(batch.resolve("originals/0.jar"), "damaged");
        }
        assertThrows(UpdateTransaction.StartupBlocked.class, this::run);
        assertTrue(Files.exists(game.resolve(".thaumcraftupdater/active.json")));
    }

    private void request(String... ids) throws IOException {
        configure(false, false);
        Path config = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(config, Files.readString(config).replace("installOnce = []", "installOnce = " + new Gson().toJson(ids)));
    }
    private void assertConsumed() throws IOException {
        assertEquals(List.of(), new com.electronwill.nightconfig.toml.TomlParser()
                .parse(Files.readString(game.resolve("config/thaumcraftupdater.toml"))).get("installOnce"));
    }
    private void assertNoTransactions() throws IOException {
        assertFalse(Files.exists(game.resolve(".thaumcraftupdater/active.json")));
        Path batches = game.resolve(".thaumcraftupdater/transactions");
        if (Files.exists(batches)) try (var files = Files.list(batches)) { assertEquals(0, files.count()); }
    }

    @Test void missingRequestedModInstallsWithNoManagedModsPresent() throws Exception {
        request("nodalmechanics", "nodalmechanics");
        publish("nodalmechanics", release("nodalmechanics", "4.3.2"), release("nodalmechanics", "4.3.2.1"));
        assertEquals(1, run().updated());
        assertTrue(Files.exists(game.resolve("mods/nodalmechanics-4.3.2.jar")));
        assertConsumed(); assertNoTransactions(); assertEquals(1, downloadRequests.size());
        Files.delete(game.resolve("mods/nodalmechanics-4.3.2.jar"));
        assertEquals(0, run().updated()); // A consumed request cannot silently reinstall next launch.
        assertEquals(1, downloadRequests.size());
    }
    @Test void missingModsDirectoryIsCreatedOnlyForRequestedInstall() throws Exception {
        Files.delete(game.resolve("mods")); request("nodalmechanics");
        publish("nodalmechanics", release("nodalmechanics", "0.1.3"), null);
        assertEquals(1, run().updated()); assertConsumed(); assertNoTransactions();
    }
    @Test void requestedInstallAlwaysUsesStableEvenWhenTestingEnabled() throws Exception {
        request("nodalmechanics");
        Path path = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(path, Files.readString(path).replace("testingUpdates = 'no'", "testingUpdates = 'yes'"));
        publish("nodalmechanics", release("nodalmechanics", "0.1.3"), release("nodalmechanics", "0.2.0.1"));
        assertEquals(1, run().updated()); assertConsumed();
        assertTrue(Files.exists(game.resolve("mods/nodalmechanics-0.1.3.jar")));
    }
    @Test void requestedInstallDoesNotForceUpgradeOrDowngradeExistingMod() throws Exception {
        Path old = install("nodalmechanics", "0.1.3.9"); request("nodalmechanics");
        publish("nodalmechanics", release("nodalmechanics", "0.1.3"), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertConsumed();
        assertTrue(downloadRequests.isEmpty());
    }
    @Test void installedModInListStillRespectsMajorUpdatePolicy() throws Exception {
        Path old = install("nodalmechanics", "0.1.3"); request("nodalmechanics");
        publish("nodalmechanics", release("nodalmechanics", "0.2.0"), null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertConsumed();
        assertTrue(downloadRequests.isEmpty());
    }
    @Test void unknownExcludedAndDeploymentDisabledRequestsAreConsumed() throws Exception {
        request("unrelated", "thaumicjourney", "thaumicexpedition", "nodalmechanics");
        for (String id : List.of("unrelated", "thaumicjourney", "thaumicexpedition", "nodalmechanics")) {
            JsonObject item = publish(id, release(id, "0.1.0"), null);
            if (id.equals("nodalmechanics")) item.addProperty("deployment_allowed", false);
        }
        assertEquals(0, run().updated()); assertConsumed(); assertTrue(downloadRequests.isEmpty());
        try (var files = Files.list(game.resolve("mods"))) { assertEquals(0, files.count()); }
    }
    @Test void unpublishedReleaseCanBeSkippedForThisLaunch() throws Exception {
        request("nodalmechanics");
        publish("nodalmechanics", null, release("nodalmechanics", "0.1.3.1"));
        assertEquals(0, run().updated()); assertEquals(List.of("nodalmechanics"),UpdaterConfig.read(game).installOnce()); assertTrue(downloadRequests.isEmpty());
    }
    @Test void networkFailurePreservesInstallRequest() throws Exception {
        request("nodalmechanics"); apiStatus = 503;
        assertEquals(0,run().updated());
        apiStatus = 200; publish("nodalmechanics", release("nodalmechanics", "0.1.3"), null);
        assertEquals(1, run().updated()); assertConsumed();
    }
    @Test void scanFailureStillConsumesRequest() throws Exception {
        request("nodalmechanics"); install("thaumcraft", "broken-version");
        publish("nodalmechanics", release("nodalmechanics", "0.1.3"), null);
        assertEquals(0, run().updated()); assertConsumed();
    }
    @Test void validOneShotListIsConsumedEvenIfAnotherSettingIsInvalid() throws Exception {
        request("nodalmechanics"); Path config = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(config, Files.readString(config).replace("testingUpdates = 'no'", "testingUpdates = 'bad'"));
        assertEquals(0, run().updated()); assertConsumed();
        assertTrue(Files.readString(config).contains("bad"));
    }
    @ParameterizedTest @ValueSource(strings={"'nodalmechanics'", "[1, 2]", "false"})
    void invalidOneShotValueIsClearedWithoutInstallation(String value) throws Exception {
        configure(false, false); Path config = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(config, Files.readString(config).replace("installOnce = []", "installOnce = " + value));
        assertEquals(0, run().updated()); assertConsumed(); assertTrue(downloadRequests.isEmpty());
    }
    @Test void multilineAndQuotedOneShotKeyIsClearedAndOtherSettingsSurvive() throws Exception {
        configure(true, true); Path config = game.resolve("config/thaumcraftupdater.toml");
        Files.writeString(config, "# keep this comment\nincludeUnstable = true\nallowMajorUpdates = true\n\"installOnce\" = [\n 'nodalmechanics', # request\n 'thaumcraft',\n]\n");
        UpdaterConfig consumed = UpdaterConfig.read(game);
        assertEquals(List.of("nodalmechanics", "thaumcraft"), consumed.installOnce());
        assertEquals(UpdateMode.YES,consumed.testingUpdates()); assertEquals(UpdateMode.YES,consumed.majorUpdates()); assertConsumed();
        assertEquals(new UpdaterConfig(true, true), UpdaterConfig.read(game));
    }
    @Test void explicitDependencyAndAddonInstallInOneTransaction() throws Exception {
        request("automagy", "thaumcraft");
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        publish("automagy", release("automagy", "0.1.1", jar("automagy", "0.1.1", dependency("automagy", "thaumcraft", "[0.1.1]", "BOTH"), false)), null);
        assertEquals(2, run().updated()); assertConsumed(); assertNoTransactions();
    }
    @Test void missingDependencyDoesNotInstallUnrequestedDependency() throws Exception {
        request("automagy");
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        publish("automagy", release("automagy", "0.1.1", jar("automagy", "0.1.1", dependency("automagy", "thaumcraft", "[0.1.1]", "BOTH"), false)), null);
        assertEquals(0, run().updated()); assertEquals(List.of("automagy"),UpdaterConfig.read(game).installOnce()); assertNoTransactions();
        try (var files = Files.list(game.resolve("mods"))) { assertEquals(0, files.count()); }
    }
    @Test void additionsAndReplacementsShareDependencyValidation() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); request("automagy");
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        publish("automagy", release("automagy", "0.1.1", jar("automagy", "0.1.1", dependency("automagy", "thaumcraft", "[0.1.1]", "BOTH"), false)), null);
        assertEquals(2, run().updated()); assertFalse(Files.exists(old)); assertConsumed(); assertNoTransactions();
    }
    @Test void invalidNewDownloadLeavesExistingModsAndCleansStaging() throws Exception {
        Path old = install("thaumcraft", "0.1.0"); request("automagy");
        publish("thaumcraft", release("thaumcraft", "0.1.1"), null);
        JsonObject bad = release("automagy", "0.1.1"); bad.addProperty("sha256", "0".repeat(64)); publish("automagy", bad, null);
        assertEquals(0, run().updated()); assertTrue(Files.exists(old)); assertEquals(List.of("automagy"),UpdaterConfig.read(game).installOnce()); assertNoTransactions();
    }
    @Test void installDestinationCollisionLeavesOtherFileUntouched() throws Exception {
        Path other = game.resolve("mods/nodalmechanics-0.1.3.jar");
        Files.write(other, jar("unrelated", "1.0.0", "", false)); String hash = FilesSupport.sha256(other);
        request("nodalmechanics"); publish("nodalmechanics", release("nodalmechanics", "0.1.3"), null);
        assertEquals(0, run().updated()); assertEquals(hash, FilesSupport.sha256(other)); assertEquals(List.of("nodalmechanics"),UpdaterConfig.read(game).installOnce()); assertNoTransactions();
    }
    private UpdateTransaction.Entry prepareInstall(Path batch, int index, String id) throws IOException {
        Path staged = UpdateTransaction.staged(batch,index); Files.write(staged,jar(id,"0.1.1","",false));
        return new UpdateTransaction.Entry(null,id+"-0.1.1.jar",null,FilesSupport.sha256(staged));
    }
    @Test void mixedInstallAndReplacementFailureRollsBackBoth() throws Exception {
        Path old = install("thaumcraft","0.1.0"); String hash = FilesSupport.sha256(old);
        AtomicInteger moves = new AtomicInteger();
        UpdateTransaction transaction = new UpdateTransaction(game,(from,to)-> {
            if (moves.incrementAndGet()==3) throw new IOException("injected failure after installation and original backup");
            Files.move(from,to,StandardCopyOption.ATOMIC_MOVE);
        });
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch();
            Path staged = UpdateTransaction.staged(batch,1); Files.write(staged,jar("thaumcraft","0.1.1","",false));
            var entries = List.of(prepareInstall(batch,0,"automagy"),new UpdateTransaction.Entry(old.getFileName().toString(),"thaumcraft-0.1.1.jar",hash,FilesSupport.sha256(staged)));
            assertThrows(IOException.class,()->transaction.commit(batch,entries));
        }
        assertEquals(hash,FilesSupport.sha256(old)); assertFalse(Files.exists(game.resolve("mods/automagy-0.1.1.jar"))); assertNoTransactions();
    }
    @Test void interruptedInstallationIsRemovedAndItsRequestStaysConsumed() throws Exception {
        UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entry = prepareInstall(batch,0,"automagy");
            journal(batch,List.of(entry),false); Files.move(UpdateTransaction.staged(batch,0),game.resolve("mods/automagy-0.1.1.jar"));
        }
        configure(false,false); assertEquals(0,run().updated());
        assertFalse(Files.exists(game.resolve("mods/automagy-0.1.1.jar"))); assertConsumed(); assertNoTransactions();
    }
    @Test void committedInstallationIsKeptAndCleanedOnRecovery() throws Exception {
        UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entry = prepareInstall(batch,0,"automagy");
            journal(batch,List.of(entry),true); Files.move(UpdateTransaction.staged(batch,0),game.resolve("mods/automagy-0.1.1.jar"));
        }
        apiStatus=503; run(); assertTrue(Files.exists(game.resolve("mods/automagy-0.1.1.jar"))); assertNoTransactions();
    }
    @Test void modifiedInstalledFileBlocksRecoveryAndKeepsEvidence() throws Exception {
        request("nodalmechanics"); UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entry = prepareInstall(batch,0,"automagy");
            journal(batch,List.of(entry),false); Files.writeString(game.resolve("mods/automagy-0.1.1.jar"),"external modification");
        }
        assertThrows(UpdateTransaction.StartupBlocked.class,this::run); assertConsumed();
        assertEquals("external modification",Files.readString(game.resolve("mods/automagy-0.1.1.jar")));
        assertTrue(Files.exists(game.resolve(".thaumcraftupdater/active.json")));
    }
    private Path legacyCompletedBatch(boolean committed) throws Exception {
        Path old = install("thaumcraft","0.1.0"); UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard = transaction.lock()) {
            Path batch = transaction.newBatch(); var entries = prepareBatch(batch,old);
            Files.copy(old,batch.resolve("originals/0.jar"));
            Files.writeString(batch.resolve(committed ? "committed.json" : "rolled-back.json"),new Gson().toJson(Map.of("id",batch.getFileName().toString(),"committed",committed,"entries",entries)));
            return batch;
        }
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void completedLegacyBackupsAreCleanedEvenWithoutDownloads(boolean committed) throws Exception {
        Path batch = legacyCompletedBatch(committed); apiStatus=503; run();
        assertFalse(Files.exists(batch)); assertTrue(Files.exists(game.resolve("mods/thaumcraft-0.1.0.jar"))); assertNoTransactions();
    }
    @Test void interruptedCleanupCanFinishWithMissingBackupDirectories() throws Exception {
        Path batch = legacyCompletedBatch(true);
        Files.delete(batch.resolve("originals/0.jar")); Files.delete(batch.resolve("originals"));
        apiStatus=503; run(); assertFalse(Files.exists(batch)); assertNoTransactions();
    }
    @Test void cleanupNeverDeletesUnknownFilesOrUnfinishedBackups() throws Exception {
        Path batch = legacyCompletedBatch(true); Files.writeString(batch.resolve("originals/keep.txt"),"do not delete");
        UpdateTransaction transaction = new UpdateTransaction(game); Path unfinished;
        try (var guard=transaction.lock()) {
            unfinished=transaction.newBatch(); Files.writeString(unfinished.resolve("originals/0.jar"),"unresolved");
        }
        apiStatus=503; run();
        assertEquals("do not delete",Files.readString(batch.resolve("originals/keep.txt")));
        assertTrue(Files.exists(batch.resolve("originals/0.jar")));
        assertEquals("unresolved",Files.readString(unfinished.resolve("originals/0.jar")));
    }
    @Test void emptyCleanupRemainderIsRemoved() throws Exception {
        Path batch = game.resolve(".thaumcraftupdater/transactions/"+UUID.randomUUID());
        Files.createDirectories(batch); run(); assertFalse(Files.exists(batch));
    }
    @Test void embeddedModCountsAsPresentForOneShotInstallation() throws Exception {
        Path outer = game.resolve("mods/container.jar");
        try (JarOutputStream zip = new JarOutputStream(Files.newOutputStream(outer))) {
            zip.putNextEntry(new JarEntry("META-INF/jarjar/metadata.json"));
            zip.write("{\"jars\":[{\"path\":\"META-INF/jarjar/nodal.jar\"}]}".getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            zip.putNextEntry(new JarEntry("META-INF/jarjar/nodal.jar"));
            zip.write(jar("nodalmechanics","0.1.3","",false)); zip.closeEntry();
        }
        String hash = FilesSupport.sha256(outer); request("nodalmechanics");
        publish("nodalmechanics",release("nodalmechanics","0.1.4"),null);
        assertEquals(0,run().updated()); assertConsumed(); assertEquals(hash,FilesSupport.sha256(outer));
        assertTrue(downloadRequests.isEmpty());
    }
    @Test void requestIsAlreadyClearedBeforeTheFirstNetworkRequest() throws Exception {
        request("nodalmechanics");
        java.util.concurrent.atomic.AtomicBoolean cleared = new java.util.concurrent.atomic.AtomicBoolean();
        server.removeContext("/"); server.createContext("/",exchange -> {
            var config = new com.electronwill.nightconfig.toml.TomlParser().parse(Files.readString(game.resolve("config/thaumcraftupdater.toml")));
            cleared.set(List.of().equals(config.get("installOnce")));
            byte[] body = "{\"schema\":2,\"managed_modids\":[],\"mods\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);
            try (var output=exchange.getResponseBody()) { output.write(body); }
        });
        assertEquals(0,run().updated()); assertTrue(cleared.get());
    }
    @Test void losingConcurrentLaunchDoesNotConsumeTheWinningLaunchRequest() throws Exception {
        request("nodalmechanics"); UpdateTransaction transaction = new UpdateTransaction(game);
        try (var guard=transaction.lock()) { assertThrows(UpdateTransaction.StartupBlocked.class,this::run); }
        assertEquals(List.of("nodalmechanics"),UpdaterConfig.read(game).installOnce()); assertConsumed();
    }

    private Map<String,String> modHashes() throws IOException {
        Map<String,String> result = new TreeMap<>();
        try (var files = Files.list(game.resolve("mods"))) {
            for (Path path : files.toList()) result.put(path.getFileName().toString(),FilesSupport.sha256(path));
        }
        return result;
    }
    @Test void serverCanAddANewManagedModWithoutAClientRelease() throws Exception {
        managed.add("new_project"); Path old=install("new_project","0.1.0");
        publish("new_project",release("new_project","0.1.1"),null);
        assertEquals(1,run().updated()); assertFalse(Files.exists(old));
        assertTrue(Files.exists(game.resolve("mods/new_project-0.1.1.jar")));
    }
    @Test void serverWhitelistRemovalIgnoresEveryLocalDuplicateAndItsNewerRelease() throws Exception {
        managed.remove("thaumcraft"); install("thaumcraft","0.1.0"); install("thaumcraft","0.1.1");
        publish("thaumcraft",release("thaumcraft","0.1.2"),null); var before=modHashes();
        assertEquals(0,run().updated()); assertEquals(before,modHashes()); assertTrue(downloadRequests.isEmpty());
    }
    @Test void missingServerWhitelistNeverFallsBackToHardCodedIds() throws Exception {
        install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        includeWhitelist=false; var before=modHashes();
        assertEquals(0,run().updated()); assertEquals(before,modHashes());
        assertTrue(logs.toString().contains("managed_modids")); assertTrue(downloadRequests.isEmpty());
    }
    @Test void duplicateUnmanagedApiRowsAndTheirInvalidReleasesAreIgnored() throws Exception {
        install("thaumcraft","0.1.0");publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        JsonObject unrelated=new JsonObject(); unrelated.addProperty("modid","unrelated");
        unrelated.addProperty("latest_stable","invalid release");
        catalog.add(unrelated);catalog.add(unrelated.deepCopy());
        assertEquals(1,run().updated());assertEquals(1,downloadRequests.size());
    }
    @Test void excludedIdsCannotBeEnabledEvenByServerWhitelist() throws Exception {
        managed.addAll(UpdateEngine.EXCLUDED);
        request("thaumicjourney","thaumicexpedition");
        for(String id:UpdateEngine.EXCLUDED) publish(id,release(id,"0.1.0"),null);
        assertEquals(0,run().updated()); assertConsumed(); assertTrue(modHashes().isEmpty()); assertTrue(downloadRequests.isEmpty());
    }
    @Test void unrelatedDuplicateMalformedAndEarlyServiceJarsDoNotBlockManagedUpdates() throws Exception {
        install("computercraft","1.113.1"); install("computercraft","1.116.1");
        install("oculus","1.8.0"); install("oculus","1.8.0.1");
        Files.writeString(game.resolve("mods/broken-third-party.jar"),"not a zip");
        Files.write(game.resolve("mods/foreign-service.jar"),jar("foreign_service","2.0.0","",true));
        Map<String,String> untouched=modHashes(); install("thaumcraft","0.1.0");
        publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        assertEquals(1,run().updated()); var after=modHashes();
        untouched.forEach((name,hash)->assertEquals(hash,after.get(name)));
    }
    @Test void unrelatedBrokenManifestDoesNotBlockButManagedBrokenMetadataDoes() throws Exception {
        try(var zip=new JarOutputStream(Files.newOutputStream(game.resolve("mods/unrelated.jar")))) {
            zip.putNextEntry(new JarEntry("META-INF/mods.toml")); zip.write("[[mods]]\nmodId='unrelated'\nversion='1.0.0'".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            zip.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));zip.write("invalid manifest".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
        }
        install("thaumcraft","0.1.0");publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        assertEquals(1,run().updated()); assertTrue(Files.exists(game.resolve("mods/unrelated.jar")));
        managed.add("unrelated"); publish("thaumcraft",release("thaumcraft","0.1.2"),null);
        var before=modHashes(); assertEquals(0,run().updated());assertEquals(before,modHashes());
    }
    @Test void unmanagedReverseDependencyDoesNotParticipateInUpdateChecks() throws Exception {
        install("unrelated","1.0.0",dependency("unrelated","thaumcraft","[0.1.0]","BOTH"));
        install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        assertEquals(1,run().updated());
    }
    @Test void requiredThirdPartyPresenceUsesHighestVersionWithoutChangingItsJars() throws Exception {
        install("curios","5.0.0"); install("curios","6.0.0"); var before=modHashes();
        install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1",jar("thaumcraft","0.1.1",dependency("thaumcraft","curios","[6,)","BOTH"),false)),null);
        assertEquals(1,run().updated());var after=modHashes();before.forEach((name,hash)->assertEquals(hash,after.get(name)));
    }
    @ParameterizedTest @CsvSource({"0.1.9,0.1.10,0.1.10", "0.1.9,0.1.10.1,0.1.10", "0.1.10,0.1.10,0.1.10"})
    void noUpgradePreservesEveryDuplicateByte(String first,String second,String remote) throws Exception {
        install("thaumcraft",first);
        Files.write(game.resolve("mods/duplicate.jar"),jar("thaumcraft",second,"# duplicate",false));
        publish("thaumcraft",release("thaumcraft",remote),null);var before=modHashes();
        assertEquals(0,run().updated());assertEquals(before,modHashes());assertTrue(downloadRequests.isEmpty());
    }
    @Test void policyDenialPreservesAllDuplicates() throws Exception {
        policies("否","否","否");install("thaumcraft","0.1.0");install("thaumcraft","0.1.1");
        publish("thaumcraft",release("thaumcraft","0.1.2"),null);var before=modHashes();
        assertEquals(0,run().updated());assertEquals(before,modHashes());assertTrue(downloadRequests.isEmpty());
    }
    @Test void updateCanReuseTheFilenameOfALowerVersionAndRemoveEveryOtherDuplicate() throws Exception {
        Files.write(game.resolve("mods/thaumcraft-0.1.11.jar"),jar("thaumcraft","0.1.9","",false));
        Files.write(game.resolve("mods/神秘时代-旧包.jar"),jar("thaumcraft","0.1.10","",false));
        publish("thaumcraft",release("thaumcraft","0.1.11"),null);
        assertEquals(1,run().updated()); assertEquals(Set.of("thaumcraft-0.1.11.jar"),modHashes().keySet());assertNoTransactions();
    }
    @Test void onlyNewestManagedDuplicateContributesDependencyConstraints() throws Exception {
        install("automagy","0.1.0",dependency("automagy","thaumcraft","[0.1.0]","BOTH"));
        install("automagy","0.1.1",dependency("automagy","thaumcraft","[0.1.0,)","BOTH"));
        install("thaumcraft","0.1.0"); publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        assertEquals(1,run().updated());assertTrue(Files.exists(game.resolve("mods/automagy-0.1.0.jar")));
    }
    @Test void duplicateApprovalUsesHighestVersionAndCleansAllOriginalsOnNextLaunch() throws Exception {
        policies("否","询问","否"); install("thaumcraft","0.1.9");install("thaumcraft","0.1.10");
        publish("thaumcraft",release("thaumcraft","0.1.11"),null);var before=modHashes();
        seedApproval("thaumcraft"); assertEquals(before,modHashes());
        assertEquals(1,run().updated());assertEquals(Set.of("thaumcraft-0.1.11.jar"),modHashes().keySet());assertNoTransactions();
    }
    @ParameterizedTest @ValueSource(strings={"add","change","remove"})
    void externallyChangedDuplicateSetInvalidatesApproval(String action) throws Exception {
        policies("否","询问","否");Path first=install("thaumcraft","0.1.0");install("thaumcraft","0.1.1");
        publish("thaumcraft",release("thaumcraft","0.1.2"),null);
        seedApproval("thaumcraft");
        if(action.equals("add")) Files.write(game.resolve("mods/extra.jar"),jar("thaumcraft","0.1.0","# extra",false));
        if(action.equals("change")) Files.write(first,jar("thaumcraft","0.1.0","# changed",false));
        if(action.equals("remove")) Files.delete(first);
        var before=modHashes();assertEquals(0,run().updated());assertEquals(before,modHashes());
        assertEquals(1,new ReviewStore(game).approvals().size());
    }
    @ParameterizedTest @ValueSource(ints={1,2,3})
    void everyDuplicateMoveFailureRestoresAllOriginals(int failAt) throws Exception {
        Path first=install("thaumcraft","0.1.0"),second=install("thaumcraft","0.1.1");var before=modHashes();
        AtomicInteger count=new AtomicInteger();
        var txn=new UpdateTransaction(game,(from,to)->{if(count.incrementAndGet()==failAt)throw new IOException("Injected move failure");Files.move(from,to,StandardCopyOption.ATOMIC_MOVE);});
        Path batch=txn.newBatch();byte[] next=jar("thaumcraft","0.1.2","",false);Files.write(UpdateTransaction.staged(batch,0),next);
        var entry=new UpdateTransaction.Entry(second.getFileName().toString(),first.getFileName().toString(),FilesSupport.sha256(second),FilesSupport.sha256(next),
                List.of(new UpdateTransaction.Original(first.getFileName().toString(),FilesSupport.sha256(first))));
        assertThrows(IOException.class,()->txn.commit(batch,List.of(entry)));assertEquals(before,modHashes());assertNoTransactions();
    }
    @Test void interruptedDuplicateRemovalRestoresBothOriginalsBeforeAnyNetworkAccess() throws Exception {
        Path a=install("thaumcraft","0.1.0"),b=install("thaumcraft","0.1.1");var before=modHashes();
        var txn=new UpdateTransaction(game);Path batch=txn.newBatch();byte[] next=jar("thaumcraft","0.1.2","",false);
        Files.write(UpdateTransaction.staged(batch,0),next);
        var entry=new UpdateTransaction.Entry(b.getFileName().toString(),"thaumcraft-0.1.2.jar",FilesSupport.sha256(b),FilesSupport.sha256(next),
                List.of(new UpdateTransaction.Original(a.getFileName().toString(),FilesSupport.sha256(a))));
        journal(batch,List.of(entry),false);Files.move(b,batch.resolve("originals/0.jar"));Files.move(a,batch.resolve("originals/0-1.jar"));
        apiStatus=503;assertEquals(0,run().updated());assertEquals(before,modHashes());assertNoTransactions();
    }
    @Test void selfUpdateStagesWhileLoadedAndHelperReplacesAllOldVersionsAfterward() throws Exception {
        Files.write(game.resolve("mods/updater-old.jar"),jar("thaumcraftupdater","0.2.2","",true));
        Files.write(game.resolve("mods/updater-current.jar"),jar("thaumcraftupdater","0.2.3","",true));
        var before=modHashes();publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4",jar("thaumcraftupdater","0.2.4","",true)),null);
        var result=run();assertEquals(0,result.updated());assertTrue(result.message().contains("self-update staged"));assertEquals(before,modHashes());
        assertTrue(Files.exists(SelfUpdateHelper.pending(game)));SelfUpdateHelper.apply(game);
        assertEquals(Set.of("thaumcraftupdater-0.2.4.jar"),modHashes().keySet());assertFalse(Files.exists(SelfUpdateHelper.pending(game)));assertNoTransactions();
    }
    @Test void selfUpdateAndNormalUpdateHaveDifferentActivationTimes() throws Exception {
        Files.write(game.resolve("mods/self.jar"),jar("thaumcraftupdater","0.2.3","",true));
        install("thaumcraft","0.1.0");install("thaumcraft","0.1.1");
        publish("thaumcraft",release("thaumcraft","0.1.2"),null);
        publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4",jar("thaumcraftupdater","0.2.4","",true)),null);
        assertEquals(1,run().updated());assertEquals(Set.of("self.jar","thaumcraft-0.1.2.jar"),modHashes().keySet());
        SelfUpdateHelper.apply(game);assertEquals(Set.of("thaumcraftupdater-0.2.4.jar","thaumcraft-0.1.2.jar"),modHashes().keySet());assertNoTransactions();
    }
    @Test void approvedSelfUpdateIsPreparedImmediatelyForTheNextLaunch() throws Exception {
        policies("询问","询问","否");Files.write(game.resolve("mods/self.jar"),jar("thaumcraftupdater","0.2.3","",true));
        publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4",jar("thaumcraftupdater","0.2.4","",true)),null);
        var before=modHashes();
        assertFalse(Files.exists(SelfUpdateHelper.pending(game)));
        engine().run(game,CLIENT); assertTrue(Files.exists(SelfUpdateHelper.pending(game))); assertEquals(before,modHashes());
        SelfUpdateHelper.apply(game);assertEquals(Set.of("thaumcraftupdater-0.2.4.jar"),modHashes().keySet());
        assertEquals(0,run().updated());assertTrue(new ReviewStore(game).approvals().isEmpty());
    }
    @Test void selfUpdateHonorsPolicyAndWhitelist() throws Exception {
        policies("否","否","否");Files.write(game.resolve("mods/self.jar"),jar("thaumcraftupdater","0.2.3","",true));
        publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4",jar("thaumcraftupdater","0.2.4","",true)),null);
        var before=modHashes();assertEquals(0,run().updated());assertFalse(Files.exists(SelfUpdateHelper.pending(game)));
        policies("是","是","是");managed.remove("thaumcraftupdater");assertEquals(0,run().updated());
        assertFalse(Files.exists(SelfUpdateHelper.pending(game)));assertEquals(before,modHashes());assertTrue(downloadRequests.isEmpty());
    }
    @Test void selfHelperRefusesChangedOriginalsAndPreservesRecoveryEvidence() throws Exception {
        Path old=install("thaumcraftupdater","0.2.3");publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4"),null);run();
        Files.writeString(old,"external change");var before=modHashes();
        assertThrows(IOException.class,()->SelfUpdateHelper.apply(game));assertEquals(before,modHashes());assertTrue(Files.exists(SelfUpdateHelper.pending(game)));
    }
    @Test void selfHelperCannotModifyFilesWhileAnotherGameOwnsTheDirectory() throws Exception {
        install("thaumcraftupdater","0.2.3");publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4"),null);run();var before=modHashes();
        try(var guard=new UpdateTransaction(game).lock()){assertThrows(UpdateTransaction.StartupBlocked.class,()->SelfUpdateHelper.apply(game));}
        assertEquals(before,modHashes());SelfUpdateHelper.apply(game);assertTrue(Files.exists(game.resolve("mods/thaumcraftupdater-0.2.4.jar")));
    }
    @Test void newSelfVersionCannotSatisfyAnImmediateGameModDependency() throws Exception {
        install("thaumcraftupdater","0.2.3");install("thaumcraft","0.1.0");
        publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4"),null);
        publish("thaumcraft",release("thaumcraft","0.1.1",jar("thaumcraft","0.1.1",dependency("thaumcraft","thaumcraftupdater","[0.2.4,)","BOTH"),false)),null);
        var before=modHashes();assertEquals(0,run().updated());assertEquals(before,modHashes());assertFalse(Files.exists(SelfUpdateHelper.pending(game)));assertNoTransactions();
    }
    @Test void selfCandidateMustRemainCompatibleIfTheNormalUpdateRollsBack() throws Exception {
        install("thaumcraftupdater","0.2.3");install("thaumcraft","0.1.0");
        publish("thaumcraft",release("thaumcraft","0.1.1"),null);
        publish("thaumcraftupdater",release("thaumcraftupdater","0.2.4",jar("thaumcraftupdater","0.2.4",dependency("thaumcraftupdater","thaumcraft","[0.1.1,)","BOTH"),true)),null);
        var before=modHashes();assertEquals(0,run().updated());assertEquals(before,modHashes());
        assertFalse(Files.exists(SelfUpdateHelper.pending(game)));assertNoTransactions();
    }
}

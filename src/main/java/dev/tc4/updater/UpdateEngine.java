package dev.tc4.updater;

import com.google.gson.*;
import dev.tc4.download.*;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public final class UpdateEngine {
    public static final String SELF = "thaumcraftupdater";
    public static final Set<String> EXCLUDED = Set.of("thaumicjourney", "thaumicexpedition");
    public static boolean validModid(String id) { return id != null && id.matches("[a-z][a-z0-9_]{1,63}"); }
    public record Context(String minecraft, String forge, String side) {}
    public record Result(int updated, String message) {}
    public record Session(Result result, UpdateTransaction.Guard guard) implements AutoCloseable {
        @Override public void close() throws IOException { guard.close(); }
    }
    private record Installed(ModMetadata newest, List<ModMetadata> all) {
        List<UpdateTransaction.Original> originals() throws IOException {
            List<UpdateTransaction.Original> result = new ArrayList<>();
            for (ModMetadata mod : all) {
                FilesSupport.regularFile(mod.file());
                result.add(new UpdateTransaction.Original(FilesSupport.localFilename(mod.file().getFileName().toString()), FilesSupport.sha256(mod.file())));
            }
            return List.copyOf(result);
        }
    }
    private record Planned(Installed installed, ReleaseApi.Release release, List<UpdateTransaction.Original> originals, Path prepared) {}
    private final ReleaseApi api;
    private final Consumer<String> log;
    @FunctionalInterface public interface Commit { void run() throws IOException; }
    @FunctionalInterface public interface Window { void show(DownloadQueue queue, Commit commit) throws IOException; }
    private final Window window;
    public UpdateEngine(ReleaseApi api, Consumer<String> log, Window window) { this.api = api; this.log = log; this.window = window; }
    public Result run(Path gameDirectory, Context context) {
        try (Session session = start(gameDirectory, context)) { return session.result; }
        catch (IOException error) { throw new UpdateTransaction.StartupBlocked("Cannot release updater lock", error); }
    }
    public Session start(Path gameDirectory, Context context) {
        UpdateTransaction transaction = new UpdateTransaction(gameDirectory);
        UpdateTransaction.Guard guard = null;
        try {
            guard = transaction.lock();
            UpdaterConfig config = null; IOException configError = null;
            try { config = UpdaterConfig.forLaunch(gameDirectory); }
            catch (IOException error) { configError = error; }
            transaction.recover(); transaction.cleanupCompleted(log);
            Result result;
            try {
                if (configError != null) throw configError;
                result = update(gameDirectory, context, transaction, config);
                config.finishRequests(gameDirectory);
            } catch (UpdateTransaction.StartupBlocked error) { throw error; }
            catch (IOException | RuntimeException error) {
                String reason = "Update skipped; original mods retained: " + error.getMessage();
                log.accept(reason); result = new Result(0, reason);
            }
            return new Session(result, guard);
        } catch (IOException | RuntimeException error) {
            if (guard != null) try { guard.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
            if (error instanceof UpdateTransaction.StartupBlocked blocked) throw blocked;
            throw new UpdateTransaction.StartupBlocked("Cannot lock updater state safely", error);
        }
    }

    private Result update(Path game, Context context, UpdateTransaction transaction, UpdaterConfig config) throws IOException {
        ReviewStore store = new ReviewStore(game, config.baseline());
        Path state = UpdaterConfig.stateDirectory(game, config.baseline());
        boolean installAll = config.installAll();
        if (config.stableUpdates()==UpdateMode.NO && config.testingUpdates()==UpdateMode.NO
                && config.installOnce().isEmpty() && store.approvals().isEmpty()) {
            return new Result(0,"Automatic updates are disabled");
        }
        Map<String, ReleaseApi.Channels> catalog;
        Set<String> managed;
        log.accept("Checking managed mods in release baseline " + config.baseline() + " before Forge mod scanning");
        try {
            var snapshot = api.snapshot(config.baseline(), config.testingUpdates() != UpdateMode.NO);
            managed = snapshot.managed(); catalog = snapshot.releases();
            FilesSupport.writeAtomic(state.resolve("managed-modids.json"),
                    new Gson().toJson(managed.stream().sorted().toList()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            if (!installAll && !store.approvals().isEmpty()) {
                managed = cachedManaged(state); catalog = Map.of();
                log.accept("Release check failed; applying previously approved downloads only with the last server whitelist: " + error.getMessage());
            } else if (window!=null && !config.installOnce().isEmpty()) {
                var requested=config.installOnce().stream().filter(id->("*".equals(id) && config.installAll()) || (validModid(id)&&!EXCLUDED.contains(id))).distinct().toList();
                var recovered=new java.util.concurrent.atomic.AtomicReference<ReleaseApi.Catalog>();
                var waiting=new ArrayList<DownloadQueue.Task>();
                var baselineLinks = api.catalogLinks(config.baseline());
                for (String id:requested) waiting.add(new DownloadQueue.Task() {
                    public DownloadQueue.Item item() {
                        boolean wildcard = "*".equals(id);
                        return new DownloadQueue.Item(id,wildcard ? config.baseline() : id,"等待版本清单 / Waiting for release catalog",game.resolve("mods").toString(),
                                DownloadQueue.Kind.INSTALL,baselineLinks,ManualLink.ordered(wildcard ? List.of() : api.manualPages(id),false),null);
                    }
                    public DownloadQueue.Prepared download(java.net.URI page,DownloadQueue.Progress progress) throws IOException {
                        var snapshot=recovered.get();
                        if(snapshot==null) snapshot=api.catalogForPage(page,config.baseline(),config.testingUpdates()!=UpdateMode.NO);
                        var found=snapshot; return ()->{ recovered.set(found); return ""; };
                    }
                });
                var skip=new LinkedHashSet<String>();
                if(!waiting.isEmpty()) try(var queue=new DownloadQueue(waiting)) {
                    window.show(queue,()->{
                        if(!queue.ready()) throw new IOException("Version check is incomplete");
                        requested.stream().filter(queue::skipped).forEach(skip::add);
                    });
                } catch(IOException|UpdateTransaction.StartupBlocked ex) {
                    config.restoreRequests(game,requested); throw new UpdateTransaction.StartupBlocked("Release catalog check interrupted",ex);
                }
                config.restoreRequests(game,skip);
                if (skip.contains("*")) installAll = false;
                var snapshot=recovered.get();
                if(snapshot==null) return new Result(0,"Manual or skipped first installs");
                var allowed=new HashSet<>(snapshot.managed()); allowed.removeAll(skip);
                managed=Set.copyOf(allowed); catalog=snapshot.releases();
            } else {
                config.restoreRequests(game,config.installOnce());
                if(window==null && (installAll || config.installOnce().stream().anyMatch(id->validModid(id)&&!EXCLUDED.contains(id))))
                    throw new UpdateTransaction.StartupBlocked("Cannot resolve required first installs without the release catalog",error);
                throw error;
            }
        }
        Set<String> requested = new LinkedHashSet<>();
        if (installAll) requested.addAll(new TreeSet<>(managed));
        for (String id : config.installOnce()) {
            if ("*".equals(id)) continue;
            if (managed.contains(id)) requested.add(id);
            else log.accept("Skipping non-whitelisted/excluded installOnce modid: " + id);
        }
        Path modsDirectory = game.resolve("mods");
        if (!Files.exists(modsDirectory) && !requested.isEmpty()) Files.createDirectory(modsDirectory);
        if (!Files.isDirectory(modsDirectory)) return new Result(0, "No mods directory");

        Map<String, List<ModMetadata>> candidates = new LinkedHashMap<>();
        Map<String, String> externalVersions = new HashMap<>();
        Set<String> present = new HashSet<>();
        try (var files = Files.list(modsDirectory)) {
            for (Path jar : files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).sorted().toList()) {
                ModMetadata metadata = ModMetadata.scan(jar, managed);
                present.addAll(metadata.mods().keySet()); present.addAll(metadata.embeddedMods().keySet());
                metadata.embeddedMods().forEach((id, version) -> newestExternal(externalVersions, id, version));
                for (var mod : metadata.mods().entrySet()) {
                    if (managed.contains(mod.getKey())) {
                        FilesSupport.regularFile(jar); Version.parse(mod.getValue());
                        candidates.computeIfAbsent(mod.getKey(), ignored -> new ArrayList<>()).add(metadata);
                    } else newestExternal(externalVersions, mod.getKey(), mod.getValue());
                }
            }
        }
        Map<String, Installed> owners = new LinkedHashMap<>();
        for (var entry : candidates.entrySet()) {
            String id = entry.getKey(); List<ModMetadata> jars = entry.getValue();
            jars.sort((a, b) -> Version.parse(b.mods().get(id)).compareTo(Version.parse(a.mods().get(id))));
            owners.put(id, new Installed(jars.get(0), List.copyOf(jars)));
            if (jars.size() > 1) log.accept("Managed duplicates: " + id + "; using highest version " + jars.get(0).mods().get(id)
                    + " from " + jars.get(0).file().getFileName() + "; all " + jars.size() + " files remain unless updated");
        }
        requested.removeIf(id -> {
            if (!present.contains(id)) return false;
            log.accept("Skipping installOnce for already installed modid: " + id); return true;
        });
        List<Planned> updates = new ArrayList<>(); Set<String> approvedIds = new HashSet<>();
        for (var approval : store.approvals()) {
            if (!managed.contains(approval.modid())) {
                log.accept("Previously approved update suspended by server whitelist: " + approval.modid()); continue;
            }
            Installed old = owners.get(approval.modid());
            if (old != null && old.newest.mods().equals(Map.of(approval.modid(), approval.release().version()))) {
                store.remove(List.of(approval.modid())); continue;
            }
            if (old == null || !old.originals().equals(approval.originals())
                    || Version.parse(approval.release().version()).compareTo(Version.parse(old.newest.mods().get(approval.modid()))) <= 0)
                throw new IOException("Approved update no longer matches installed mod: " + approval.modid());
            requireReplaceable(approval.modid(), old);
            Path prepared = store.staged(approval); FilesSupport.regularFile(prepared);
            if (!FilesSupport.sha256(prepared).equals(approval.release().sha256())) throw new IOException("Approved download checksum mismatch");
            updates.add(new Planned(old, approval.release(), approval.originals(), prepared)); approvedIds.add(approval.modid());
        }
        List<Planned> offered = new ArrayList<>();
        for (var entry : owners.entrySet()) {
            String id = entry.getKey(); Installed old = entry.getValue();
            ReleaseApi.Channels channels = catalog.get(id);
            if (channels == null || approvedIds.contains(id)) continue;
            Version current = Version.parse(old.newest.mods().get(id));
            List<ReleaseApi.Release> releases = new ArrayList<>();
            if (channels.stable() != null) releases.add(channels.stable());
            if (channels.testing() != null) releases.add(channels.testing());
            releases.sort((a, b) -> Version.parse(b.version()).compareTo(Version.parse(a.version())));
            ReleaseApi.Release release = null;
            for (var candidate : releases) {
                Version version = Version.parse(candidate.version());
                if (version.compareTo(current) <= 0 || store.ignored(id, candidate.version())) continue;
                UpdateMode candidateMode = config.mode(current, version);
                if (candidateMode == UpdateMode.NO) { log.accept("Skipping update by configured policy: " + id + " " + candidate.version()); continue; }
                release = candidate; break;
            }
            if (release == null) continue;
            requireReplaceable(id, old);
            offered.add(new Planned(old,release,old.originals(),null));
        }
        var unavailable=new ArrayList<String>();
        for (String id : requested) {
            var channels=catalog.get(id);
            if (channels!=null && channels.stable()!=null) offered.add(new Planned(null,channels.stable(),List.of(),null));
            else { unavailable.add(id); log.accept("No eligible stable release for installOnce modid: "+id); }
        }
        if ((!offered.isEmpty() || !unavailable.isEmpty()) && window == null) {
            for (var plan:offered) log.accept("A download requires the startup GUI: "+plan.release.modid()+" "+api.links(plan.release));
            if (!requested.isEmpty()) {
                config.restoreRequests(game,requested);
                throw new UpdateTransaction.StartupBlocked("Required first installs need a client download window or manual installation in "
                        +modsDirectory+": "+String.join(", ",requested));
            }
            offered.clear(); unavailable.clear();
        }
        if (offered.isEmpty() && unavailable.isEmpty()) {
            if (updates.isEmpty()) return new Result(0,"No downloads for this launch");
            return apply(game,context,transaction,store,owners,externalVersions,approvedIds,updates);
        }
        Path work = Files.createTempDirectory(game.resolve(".thaumcraftupdater"),"download-");
        try {
            var tasks=new LinkedHashMap<Planned,ModDownload>();
            for (var plan:offered) {
                var release=plan.release;
                var task=new ModDownload(api,release,catalog.get(release.modid()).name(),
                        plan.installed==null?null:plan.installed.newest.mods().get(release.modid()),work,modsDirectory,
                        !release.manualPages().isEmpty() && DownloadPreferences.chinese(game));
                tasks.put(plan,task);
            }
            var rows=new ArrayList<DownloadQueue.Task>(tasks.values());
            for(String id:unavailable) rows.add(new DownloadQueue.Task() {
                public DownloadQueue.Item item() { return new DownloadQueue.Item(id,id,"暂无稳定版 / No stable release",modsDirectory.toString(),DownloadQueue.Kind.INSTALL,List.of(),ManualLink.ordered(api.manualPages(id),false),null); }
                public DownloadQueue.Prepared download(java.net.URI uri,DownloadQueue.Progress progress) throws IOException {
                    throw new IOException("No published stable version for "+id);
                }
            });
            var queue=new DownloadQueue(rows);
            try (queue) {
                Result[] result={null};
                window.show(queue,()->{
                    if (!queue.ready()) throw new IOException("Downloads are incomplete");
                    var accepted=new ArrayList<>(updates);
                    for (var pair:tasks.entrySet()) {
                        var plan=pair.getKey(); var task=pair.getValue(); var release=plan.release;
                        if (queue.skipped(release.modid())) continue;
                        release = task.verifiedRelease();
                        var originals=new ArrayList<>(plan.originals);
                        if (task.inMods()!=null) {
                            String filename=task.inMods().getFileName().toString();
                            // Manual replacement can remove the old JAR or overwrite it in place.
                            originals.removeIf(old->old.filename().equals(filename) || !Files.exists(modsDirectory.resolve(old.filename())));
                            originals.add(new UpdateTransaction.Original(filename,release.sha256()));
                        }
                        accepted.add(new Planned(plan.installed,release,List.copyOf(originals),task.staged()));
                    }
                    var deferred=new LinkedHashSet<String>();
                    unavailable.stream().filter(queue::skipped).forEach(deferred::add);
                    for (var plan:tasks.keySet()) if (plan.installed==null && queue.skipped(plan.release.modid())) deferred.add(plan.release.modid());
                    config.restoreRequests(game,deferred);
                    result[0]=accepted.isEmpty()?new Result(0,"Downloads skipped for this launch"):
                            apply(game,context,transaction,store,owners,externalVersions,approvedIds,accepted);
                });
                if (result[0]==null) throw new UpdateTransaction.StartupBlocked("Startup download window closed before continuing");
                return result[0];
            } finally { queue.awaitWorkers(); }
        } catch (UpdateTransaction.StartupBlocked e) {
            config.restoreRequests(game,requested); throw e;
        } catch (IOException e) {
            config.restoreRequests(game,requested);
            throw new UpdateTransaction.StartupBlocked("Cannot complete startup downloads",e);
        } finally {
            try (var paths=Files.list(work)) {
                for (var file:paths.toList()) if(Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(work);
        }
    }

    private Result apply(Path game, Context context, UpdateTransaction transaction, ReviewStore store,
                         Map<String,Installed> owners, Map<String,String> externalVersions, Set<String> approvedIds,
                         List<Planned> updates) throws IOException {
        updates.sort(Comparator.comparing(update -> update.release.modid().equals(SELF)));
        Path batch = transaction.newBatch();
        List<UpdateTransaction.Entry> entries = new ArrayList<>();
        Map<Path, ModMetadata> replacements = new LinkedHashMap<>(); List<ModMetadata> additions = new ArrayList<>();
        List<ModMetadata> selfCandidates = new ArrayList<>();
        Planned self = null; UpdateTransaction.Entry selfEntry = null; Path selfStaged = null;
        try {
            for (int i = 0; i < updates.size(); i++) {
                Planned update = updates.get(i); Path staged = UpdateTransaction.staged(batch, i);
                Files.copy(update.prepared, staged);
                if (!FilesSupport.sha256(staged).equals(update.release.sha256())) throw new IOException("Prepared file changed before commit");
                ModMetadata candidate = ModMetadata.read(staged);
                requireCandidate(candidate, update.release);
                var entry = transactionEntry(update.originals, update.release);
                if (update.release.modid().equals(SELF)) {
                    self = update; selfEntry = entry; selfStaged = staged; selfCandidates.add(candidate);
                } else {
                    if (update.installed == null) additions.add(candidate); else replacements.put(update.installed.newest.file(), candidate);
                    entries.add(entry);
                }
            }
            List<ModMetadata> installed = owners.values().stream().map(value -> value.newest).distinct().toList();
            // Self replacement is independent of the immediate transaction. It must also
            // remain loadable if that transaction fails and restores the current mods.
            if (!selfCandidates.isEmpty()) checkDependencies(installed, Map.of(), List.of(), selfCandidates, externalVersions, context);
            checkDependencies(installed, replacements, additions, selfCandidates, externalVersions, context);
            if (self != null) {
                SelfUpdate.stage(game, self.release.version(), selfEntry, selfStaged); Files.delete(selfStaged);
                log.accept("Self-update prepared: " + self.release.version() + "; applies after game exit, effective next launch");
            }
            if (!entries.isEmpty()) transaction.commit(batch, entries);
            if (!approvedIds.isEmpty()) try { store.remove(approvedIds); }
                catch (IOException error) { log.accept("Approved queue cleanup deferred: " + error.getMessage()); }
            for (Planned update : updates) if (!update.release.modid().equals(SELF)) log.accept(update.installed == null
                    ? "Installed " + update.release.modid() + " " + update.release.version()
                    : "Updated " + update.release.modid() + " " + update.installed.newest.mods().get(update.release.modid()) + " -> " + update.release.version()
                        + "; removed " + update.originals.size() + " old JAR(s)");
            return new Result(entries.size(), "Updated/installed " + entries.size() + " mod(s) for this launch"
                    + (self == null ? "" : "; self-update staged for the next launch"));
        } finally { transaction.discardStaged(batch, updates.size(), log); transaction.cleanupCompleted(log); }
    }

    static UpdateTransaction.Entry transactionEntry(List<UpdateTransaction.Original> originals, ReleaseApi.Release release) {
        var first = originals.isEmpty() ? null : originals.get(0);
        return new UpdateTransaction.Entry(first == null ? null : first.filename(), release.filename(), first == null ? null : first.hash(),
                release.sha256(), originals.isEmpty() ? List.of() : originals.subList(1, originals.size()));
    }
    private static void requireReplaceable(String id, Installed installed) throws IOException {
        for (ModMetadata mod : installed.all) if (mod.mods().size() != 1 || (mod.earlyService() && !id.equals(SELF)))
            throw new IOException("Cannot replace multi-mod/early-service JAR: " + mod.file());
    }
    static void requireCandidate(ModMetadata candidate, ReleaseApi.Release release) throws IOException {
        if (!candidate.mods().equals(Map.of(release.modid(), release.version())) || (candidate.earlyService() && !release.modid().equals(SELF)))
            throw new IOException("Downloaded JAR identity/early-service mismatch: " + release.modid());
    }
    static void checkSelfCandidate(Path game, ReleaseApi.Release release, Path staged, Context context) throws IOException {
        ModMetadata candidate = ModMetadata.read(staged); requireCandidate(candidate, release);
        Map<String, String> versions = new HashMap<>();
        try (var files = Files.list(game.resolve("mods"))) {
            for (Path path : files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).toList()) {
                var metadata = ModMetadata.scan(path, Set.of());
                metadata.mods().forEach((id, version) -> newestExternal(versions, id, version));
                metadata.embeddedMods().forEach((id, version) -> newestExternal(versions, id, version));
            }
        }
        checkDependencies(List.of(), Map.of(), List.of(candidate), List.of(), versions, context);
    }
    private static void newestExternal(Map<String, String> versions, String id, String version) {
        if (version == null) return;
        versions.merge(id, version, (a, b) -> new DefaultArtifactVersion(a).compareTo(new DefaultArtifactVersion(b)) >= 0 ? a : b);
    }
    private static Set<String> cachedManaged(Path state) throws IOException {
        Path file = state.resolve("managed-modids.json"); FilesSupport.regularFile(file);
        Set<String> result = new HashSet<>();
        try (var in = Files.newInputStream(file)) {
            for (var id : JsonParser.parseString(new String(FilesSupport.readBounded(in, 65536), StandardCharsets.UTF_8)).getAsJsonArray()) {
                if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString() || !validModid(id.getAsString())) throw new IOException("Invalid cached whitelist");
                result.add(id.getAsString());
            }
        }
        result.removeAll(EXCLUDED); return result;
    }
    private static void checkDependencies(List<ModMetadata> installed, Map<Path, ModMetadata> replacements, List<ModMetadata> additions,
                                          List<ModMetadata> selfCandidates, Map<String, String> external, Context context) throws IOException {
        Map<String, String> versions = new HashMap<>(external); Set<String> changed = new HashSet<>();
        replacements.values().forEach(mod -> changed.addAll(mod.mods().keySet())); additions.forEach(mod -> changed.addAll(mod.mods().keySet()));
        selfCandidates.forEach(mod -> changed.addAll(mod.mods().keySet()));
        List<ModMetadata> finalMods = new ArrayList<>(installed.stream().map(mod -> replacements.getOrDefault(mod.file(), mod)).toList());
        finalMods.addAll(additions);
        for (ModMetadata mod : finalMods) versions.putAll(mod.mods());
        versions.put("minecraft", context.minecraft); versions.put("forge", context.forge);
        finalMods.addAll(selfCandidates);
        for (ModMetadata mod : finalMods) for (var dependency : mod.dependencies()) {
            if (!changed.contains(dependency.owner()) && !changed.contains(dependency.modid())) continue;
            if (!dependency.side().equals("BOTH") && !dependency.side().equals(context.side)) continue;
            String version = versions.get(dependency.modid());
            if (version == null) {
                if (dependency.mandatory()) throw new IOException("Missing required dependency " + dependency.modid() + " for " + dependency.owner());
                continue;
            }
            if (dependency.range().isBlank()) continue;
            try {
                if (!VersionRange.createFromVersionSpec(dependency.range()).containsVersion(new DefaultArtifactVersion(version)))
                    throw new IOException("Dependency conflict: " + dependency.owner() + " needs " + dependency.modid() + " " + dependency.range() + ", found " + version);
            } catch (org.apache.maven.artifact.versioning.InvalidVersionSpecificationException error) { throw new IOException("Invalid dependency version range", error); }
        }
    }
}

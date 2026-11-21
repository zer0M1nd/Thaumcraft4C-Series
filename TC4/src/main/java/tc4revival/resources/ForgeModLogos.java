package tc4revival.resources;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.niofs.union.UnionFileSystem;
import cpw.mods.niofs.union.UnionFileSystemProvider;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.security.CodeSigner;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

/** Forge 47 reads the logo from the owning mod file, outside Minecraft's resource-pack stack.
 * Publish a normal logoFile and expose that single PNG to both direct mod-file and pack readers. */
final class ForgeModLogos {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    static synchronized void update(Path game, Map<String, byte[]> images) {
        try {
            // These two discovery objects are constructed before the game layer and have no setters.
            Field logoFile = ModInfo.class.getDeclaredField("logoFile");
            Field jar = ModFile.class.getDeclaredField("jar");
            logoFile.setAccessible(true);
            jar.setAccessible(true);
            for (var info : ModList.get().getMods()) {
                if (!(info instanceof ModInfo mod) || !images.containsKey(mod.getModId())) continue;
                var image = images.get(mod.getModId());
                if (image == null) {
                    logoFile.set(mod, Optional.empty());
                    continue;
                }
                try {
                    var file = mod.getOwningFile().getFile();
                    var directory = game.resolve(".thaumcraft-cache/mod-logos").resolve(file.getModInfos().get(0).getModId());
                    Files.createDirectories(directory);
                    var name = "tc4-logo-" + mod.getModId() + "-" + OriginalResources.sha256(image) + ".png";
                    writeImage(directory, name, image);
                    var original = file.getSecureJar();
                    var view = original instanceof LogoResources existing ? existing : new LogoResources(original, directory);
                    view.names.add(name);
                    if (view != original) jar.set(file, view);
                    logoFile.set(mod, Optional.of(name));
                } catch (IOException | RuntimeException failure) {
                    logoFile.set(mod, Optional.empty());
                    LOGGER.warn("Cannot expose original logo for {}", mod.getModId(), failure);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            LOGGER.warn("Cannot publish original mod logos to Forge metadata", failure);
        }
    }

    private static void writeImage(Path directory, String name, byte[] bytes) throws IOException {
        Path file = directory.resolve(name);
        if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && Files.size(file) == bytes.length
                && Arrays.equals(Files.readAllBytes(file), bytes)) return;
        Path temp = Files.createTempFile(directory, ".logo-", ".tmp");
        try {
            Files.write(temp, bytes);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unavailable) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    /** The original jar still supplies the module, packages, services and signatures. The alternate
     * resource root allows callers using getRootPath().resolve(logoFile) to read the same PNG. */
    private static final class LogoResources implements SecureJar {
        private final SecureJar original;
        private final Path directory;
        private final UnionFileSystem view;
        private final Set<String> names = ConcurrentHashMap.newKeySet();

        LogoResources(SecureJar original, Path directory) {
            this.original = original;
            this.directory = directory;
            var root = original.getRootPath().toAbsolutePath();
            var provider = (UnionFileSystemProvider) root.getFileSystem().provider();
            view = provider.newFileSystem((entry, source) -> source.equals("/") || names.contains(entry), root, directory);
        }

        @Override public Path getPath(String first, String... more) {
            var path = original.getPath(first, more);
            var name = path.getFileName();
            if (name == null || path.toString().isEmpty()) return getRootPath();
            if (path.getNameCount() == 1 && names.contains(name.toString())) return directory.resolve(name.toString());
            return path;
        }
        @Override public Path getRootPath() { return view.getRoot(); }
        @Override public Status verifyPath(Path path) {
            if (path.getFileSystem() == view) {
                String relative = view.getRoot().relativize(path.toAbsolutePath()).toString();
                return names.contains(relative) ? Status.NONE : original.verifyPath(original.getPath(relative));
            }
            if (directory.equals(path.getParent()) && names.contains(path.getFileName().toString())) return Status.NONE;
            return original.verifyPath(path);
        }
        @Override public ModuleDataProvider moduleDataProvider() { return original.moduleDataProvider(); }
        @Override public Path getPrimaryPath() { return original.getPrimaryPath(); }
        @Override public CodeSigner[] getManifestSigners() { return original.getManifestSigners(); }
        @Override public Status getFileStatus(String name) { return names.contains(name) ? Status.NONE : original.getFileStatus(name); }
        @Override public Attributes getTrustedManifestEntries(String name) { return original.getTrustedManifestEntries(name); }
        @Override public boolean hasSecurityData() { return original.hasSecurityData(); }
        @Override public Set<String> getPackages() { return original.getPackages(); }
        @Override public List<Provider> getProviders() { return original.getProviders(); }
        @Override public String name() { return original.name(); }
    }

    private ForgeModLogos() {}
}

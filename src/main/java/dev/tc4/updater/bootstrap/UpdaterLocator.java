package dev.tc4.updater.bootstrap;

import dev.tc4.updater.FilesSupport;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;

/** Makes the independent utility visible in Forge's mod list without loading its boot classes twice. */
public final class UpdaterLocator extends AbstractJarFileModLocator {
    @Override public String name() { return "ThaumcraftUpdater metadata"; }
    @Override public void initArguments(Map<String, ?> arguments) {}
    @Override public Stream<Path> scanCandidates() {
        try (var input = UpdaterLocator.class.getResourceAsStream("/META-INF/thaumcraftupdater/thaumcraftupdater-marker.jar")) {
            if (input == null) throw new IOException("Missing bundled updater marker");
            byte[] bytes = FilesSupport.readBounded(input, 1024 * 1024);
            Path directory = UpdaterService.gameDirectory.resolve(".thaumcraftupdater/loader");
            if (Files.isSymbolicLink(directory)) throw new IOException("Symlinked marker directory");
            Files.createDirectories(directory);
            String hash = FilesSupport.sha256(bytes);
            Path marker = directory.resolve(hash + ".jar");
            if (Files.exists(marker)) {
                FilesSupport.regularFile(marker);
                if (!FilesSupport.sha256(marker).equals(hash)) throw new IOException("Updater marker checksum mismatch");
            } else FilesSupport.writeAtomic(marker, bytes);
            return Stream.of(marker);
        } catch (IOException error) { throw new IllegalStateException("Cannot register ThaumcraftUpdater metadata", error); }
    }
}

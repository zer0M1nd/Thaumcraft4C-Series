package dev.tc4.updater.bootstrap;

import cpw.mods.modlauncher.api.IEnvironment;

import dev.tc4.download.DownloadQueue;
import dev.tc4.updater.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Forge discovers JARs before Minecraft can draw a Screen. A minimal, offline Forge window supplies
 * the native widgets while the launcher's original JVM owns the mods transaction.
 */
final class StartupWindow implements UpdateEngine.Window {
    private final Path game, assets;
    private final String forge, version;

    StartupWindow(Path game, IEnvironment environment, String forge) {
        this.game = game;
        this.forge = forge;
        var libraries =
                Path.of(
                                System.getProperty(
                                        "libraryDirectory", game.resolve("libraries").toString()))
                        .toAbsolutePath();
        assets =
                environment
                        .getProperty(IEnvironment.Keys.ASSETSDIR.get())
                        .orElse(libraries.resolveSibling("assets"))
                        .toAbsolutePath();
        version = environment.getProperty(IEnvironment.Keys.VERSION.get()).orElse("1.20.1");
    }

    public void show(DownloadQueue queue, UpdateEngine.Commit commit) throws IOException {
        Path dir = Files.createTempDirectory(game.resolve(".thaumcraftupdater"), "screen-");
        Path log = game.resolve(".thaumcraftupdater/startup-window.log");
        var token = UUID.randomUUID().toString();
        try (var server = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))) {
            Path mods = Files.createDirectory(dir.resolve("mods"));
            try (var in =
                    StartupWindow.class.getResourceAsStream(
                            "/META-INF/thaumcraftupdater/thaumcraftupdater-marker.jar")) {
                if (in == null) throw new IOException("Missing startup GUI");
                Files.copy(in, mods.resolve("startup-ui.jar"));
            }
            // Keep the user's GUI scale, link confirmation and language. Don't copy resource-pack
            // or server settings.
            Path options = game.resolve("options.txt");
            var settings =
                    new ArrayList<>(
                            List.of("onboardAccessibility:false", "soundCategory_master:0.0"));
            if (Files.isRegularFile(options))
                try (var lines = Files.lines(options)) {
                    lines.filter(
                                    line ->
                                            line.matches(
                                                    "(lang|guiScale|chatLinks|chatLinksPrompt):.*"))
                            .forEach(settings::add);
                }
            Files.write(dir.resolve("options.txt"), settings);
            Files.writeString(
                    dir.resolve("connection"),
                    server.getLocalPort() + "\n" + token + "\n" + ProcessHandle.current().pid());
            Process child = launch(dir, log);
            try {
                server.setSoTimeout(300);
                while (child.isAlive()) {
                    Socket connection;
                    try {
                        connection = server.accept();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    try (connection) {
                        connection.setSoTimeout(2000);
                        var in =
                                new DataInputStream(
                                        new BufferedInputStream(connection.getInputStream()));
                        var out =
                                new DataOutputStream(
                                        new BufferedOutputStream(connection.getOutputStream()));
                        if (!token.equals(in.readUTF())) continue;
                        connection.setSoTimeout(0);
                        while (true) {
                            String action = in.readUTF(), id = in.readUTF();
                            if (action.equals("continue") || action.equals("confirmed")) {
                                synchronized (queue) {
                                    if (queue.ready()
                                            && (!queue.needsConfirmation()
                                                    || action.equals("confirmed"))) {
                                        try {
                                            commit.run();
                                        } catch (UpdateTransaction.StartupBlocked e) {
                                            throw e;
                                        } catch (IOException e) {
                                            queue.reject(e.getMessage());
                                            send(out, queue.snapshot());
                                            continue;
                                        }
                                        queue.close();
                                        send(out, "finished");
                                        return;
                                    }
                                }
                            } else if (!action.equals("snapshot")) queue.action(action, id);
                            send(out, queue.snapshot());
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }
                throw new IOException(
                        "Minecraft download window exited before completion. See " + log);
            } finally {
                try {
                    if (!child.waitFor(4, TimeUnit.SECONDS)) child.destroy();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    child.destroy();
                }
            }
        } finally {
            // Only files in this invocation's newly created private game directory belong to us.
            try (var paths = Files.walk(dir)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            } catch (IOException e) {
                System.getLogger("ThaumcraftUpdater")
                        .log(System.Logger.Level.WARNING, "Window cleanup: " + e.getMessage());
            }
        }
    }

    private static void send(DataOutputStream out, String response) throws IOException {
        var bytes = response.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    private Process launch(Path dir, Path log) throws IOException {
        // Forge's default appenders rotate logs relative to the process working directory,
        // which can also hold the original game's live logs. Capture only this child's console.
        Path logging = Files.writeString(dir.resolve("log4j2.xml"), """
                <Configuration status="WARN">
                    <Appenders><Console name="Console"><PatternLayout pattern="[%d{HH:mm:ss}] [%t/%p] [%c]: %m%n"/></Console></Appenders>
                    <Loggers><Root level="info"><AppenderRef ref="Console"/></Root></Loggers>
                </Configuration>
                """);
        var args =
                new ArrayList<>(
                        List.of(
                                "-Xms64m",
                                "-Xmx768m",
                                "-XX:+UseSerialGC",
                                "-Dtc4.downloadWindow=true",
                                "-Dlog4j.configurationFile=" + logging.toUri().toASCIIString()));
        if (System.getProperty("os.name").contains("Mac")) args.add("-XstartOnFirstThread");
        for (String property :
                List.of(
                        "java.library.path",
                        "jna.tmpdir",
                        "org.lwjgl.system.SharedLibraryExtractPath",
                        "io.netty.native.workdir",
                        "libraryDirectory",
                        "legacyClassPath",
                        "ignoreList",
                        "mergeModules",
                        "java.net.preferIPv6Addresses")) {
            String value = System.getProperty(property);
            if (value != null) args.add("-D" + property + "=" + value);
        }
        Collections.addAll(
                args,
                "-cp",
                System.getProperty("java.class.path"),
                "--module-path",
                System.getProperty("jdk.module.path", ""),
                "--add-modules",
                "ALL-MODULE-PATH",
                "--add-opens",
                "java.base/java.util.jar=cpw.mods.securejarhandler",
                "--add-opens",
                "java.base/java.lang.invoke=cpw.mods.securejarhandler",
                "--add-exports",
                "java.base/sun.security.util=cpw.mods.securejarhandler",
                "--add-exports",
                "jdk.naming.dns/com.sun.jndi.dns=java.naming",
                "cpw.mods.bootstraplauncher.BootstrapLauncher",
                "--launchTarget",
                "forgeclient",
                "--fml.forgeVersion",
                forge,
                "--fml.mcVersion",
                "1.20.1",
                "--fml.forgeGroup",
                "net.minecraftforge",
                "--fml.mcpVersion",
                "20230612.114412",
                "--gameDir",
                dir.toString(),
                "--assetsDir",
                assets.toString(),
                "--assetIndex",
                "5",
                "--version",
                version,
                "--username",
                "Downloads",
                "--uuid",
                "00000000000000000000000000000001",
                "--accessToken",
                "0",
                "--width",
                "920",
                "--height",
                "540",
                "--userType",
                "legacy");
        Path file = dir.resolve("startup.args");
        Files.write(
                file,
                args.stream()
                        .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                        .toList(),
                Charset.forName(
                        System.getProperty("native.encoding", Charset.defaultCharset().name())));
        String exe = System.getProperty("os.name").startsWith("Windows") ? "javaw.exe" : "java";
        // Retain the launcher's working directory for relative Forge library paths. No account
        // arguments are inherited.
        return new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", exe).toString(), "@" + file)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
    }
}

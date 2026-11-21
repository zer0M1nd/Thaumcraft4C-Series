package dev.tc4.resourceadapter;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdapterTest {
    @TempDir Path temp;
    private static final String SOURCE = "assets/old/textures/items/Thing.png";
    private static final String TARGET = "assets/ported/textures/item/thing.png";
    private static final String RULE = TARGET + "\t" + SOURCE + "\t*\t-";
    private final List<String> warnings = new ArrayList<>();
    private final PackInstaller installer = new PackInstaller(warnings::add);

    private static byte[] text(String str) { return str.getBytes(StandardCharsets.UTF_8); }
    private static byte[] png(int color) throws IOException {
        var image = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(4, 4, color);
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
    private static byte[] meta(int format) {
        return text("\uFEFF{\"pack\":{\"pack_format\":" + format + ",\"description\":{\"text\":\"Local artist\",\"color\":\"gold\"}}}");
    }
    private static void zip(Path path, Map<String, byte[]> files) throws IOException {
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }
    private static byte[] entry(Path path, String name) throws IOException {
        try (var zip = new ZipFile(path.toFile())) {
            assertNotNull(zip.getEntry(name), name);
            return zip.getInputStream(zip.getEntry(name)).readAllBytes();
        }
    }
    private static ResourceMapping mapping() throws IOException { return ResourceMapping.parse(text(RULE)); }
    private List<PackInstaller.Result> scan() throws IOException { return installer.prepare(temp, mapping()); }

    @Test void arbitraryZipPreservesTextureAnimationDescriptionIconAndAttribution() throws Exception {
        Path source = temp.resolve("作者的包.ZIP");
        zip(source, Map.of("pack.mcmeta", meta(1), SOURCE.toUpperCase(Locale.ROOT), png(3),
                SOURCE + ".mcmeta", text("\uFEFF{animation:{frametime:2}}"), "pack.png", png(7),
                "LICENSE.txt", text("artist's license"), "assets/unrelated/textures/items/x.png", png(4)));
        byte[] original = Files.readAllBytes(source);
        var packs = scan();
        assertEquals(1, packs.size()); assertEquals(1, packs.get(0).textures());
        assertTrue(warnings.isEmpty());
        Path out = packs.get(0).pack();
        assertArrayEquals(png(3), entry(out, TARGET));
        assertArrayEquals(png(7), entry(out, "pack.png"));
        assertArrayEquals(text("artist's license"), entry(out, "upstream/license.txt"));
        assertArrayEquals(text("{\"animation\":{\"frametime\":2}}"), entry(out, TARGET + ".mcmeta"));
        var pack = JsonParser.parseString(new String(entry(out, "pack.mcmeta"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("pack");
        assertEquals(15, pack.get("pack_format").getAsInt());
        assertEquals("Local artist", pack.getAsJsonObject("description").get("text").getAsString());
        try (var zip = new ZipFile(out.toFile())) { assertNull(zip.getEntry("assets/unrelated/textures/items/x.png")); }
        assertArrayEquals(original, Files.readAllBytes(source));
    }

    @Test void wrappedZipAndDirectoryAreIndependentAndKeepStableIds() throws Exception {
        Path folder = temp.resolve("colors");
        Files.createDirectories(folder.resolve("nested").resolve(SOURCE).getParent());
        Files.write(folder.resolve("nested/pack.mcmeta"), meta(3));
        Files.write(folder.resolve("nested").resolve(SOURCE), png(9));
        zip(temp.resolve("colors.zip"), Map.of("release/pack.mcmeta", meta(1), "release/" + SOURCE, png(2),
                "__MACOSX/junk", text("ignored"), "unused/" + SOURCE, png(12)));
        var packs = scan();
        assertEquals(List.of("thaumcraftresourceadapter/colors", "thaumcraftresourceadapter/colors.zip"),
                packs.stream().map(PackInstaller.Result::id).toList());
        assertArrayEquals(png(9), entry(packs.get(0).pack(), TARGET));
        assertArrayEquals(png(2), entry(packs.get(1).pack(), TARGET));
        assertTrue(scan().stream().allMatch(PackInstaller.Result::reused));
        Files.write(folder.resolve("nested").resolve(SOURCE), png(3));
        var changed = scan();
        assertEquals(packs.get(0).id(), changed.get(0).id());
        assertNotEquals(packs.get(0).pack(), changed.get(0).pack());
        assertArrayEquals(png(3), entry(changed.get(0).pack(), TARGET));
        assertTrue(changed.get(1).reused());
        Files.delete(temp.resolve("colors.zip"));
        assertEquals(List.of(changed.get(0).id()), scan().stream().map(PackInstaller.Result::id).toList());
    }

    @Test void noSourceModernUnrelatedAndOldGeneratedPacksProduceNothing() throws Exception {
        assertTrue(scan().isEmpty());
        zip(temp.resolve("Modernity-TC4-1.20.1.zip"), Map.of("pack.mcmeta", meta(15), SOURCE, png(2)));
        zip(temp.resolve("unrelated.zip"), Map.of("pack.mcmeta", meta(1), "assets/elsewhere/x.png", png(2)));
        zip(temp.resolve("assets-only.zip"), Map.of(SOURCE, png(1)));
        Files.createDirectory(temp.resolve("config"));
        Files.writeString(temp.resolve("config/thaumcraftresourceadapter.json"), "{ obsolete sourceUrl and invalid JSON!");
        assertTrue(scan().isEmpty()); assertTrue(warnings.isEmpty());
        assertFalse(Files.exists(temp.resolve(PackInstaller.CACHE)));
    }

    @Test void malformedSourcesLeaveOtherPacksAndUserFilesUsable() throws Exception {
        zip(temp.resolve("ok.zip"), Map.of("pack.mcmeta", meta(3), SOURCE, png(7)));
        Files.writeString(temp.resolve("unreadable.zip"), "not a zip");
        zip(temp.resolve("invalid-png.zip"), Map.of("pack.mcmeta", meta(1), SOURCE, text("not a png")));
        zip(temp.resolve("invalid-sidecar.zip"), Map.of("pack.mcmeta", meta(1), SOURCE, png(1), SOURCE + ".mcmeta", text("{animation:7}")));
        zip(temp.resolve("invalid-pack.zip"), Map.of("pack.mcmeta", text("{broken")));
        zip(temp.resolve("unsafe.zip"), Map.of("pack.mcmeta", meta(1), SOURCE, png(2), "../assets/outside.png", png(3)));
        zip(temp.resolve("ambiguous.zip"), Map.of("pack.mcmeta", meta(1), SOURCE, png(1), SOURCE.toLowerCase(Locale.ROOT), png(2)));
        var packs = scan();
        assertEquals(1, packs.size()); assertEquals("ok.zip", packs.get(0).source());
        assertEquals(6, warnings.size());
        assertFalse(Files.exists(temp.resolve("outside.png")));
        try (var files = Files.list(temp.resolve(PackInstaller.CACHE))) {
            assertFalse(files.anyMatch(p -> p.toString().endsWith(".tmp")));
        }
    }

    @Test void rootTakesPriorityAndTwoWrappedPacksAreNotMerged() throws Exception {
        zip(temp.resolve("modern.zip"), Map.of("pack.mcmeta", meta(15), "old/pack.mcmeta", meta(1), "old/" + SOURCE, png(3)));
        zip(temp.resolve("mixed.zip"), Map.of("one/pack.mcmeta", meta(1), "one/" + SOURCE, png(1),
                "two/pack.mcmeta", meta(3), "two/" + SOURCE, png(3)));
        assertTrue(scan().isEmpty());
    }

    @Test void traditionalZipFilenameEncodingDoesNotHideValidAssets() throws Exception {
        var path = temp.resolve("old-writer.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(path), java.nio.charset.Charset.forName("IBM437"))) {
            for (var file : Map.of("café/pack.mcmeta", meta(1), "café/" + SOURCE, png(7)).entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        assertArrayEquals(png(7), entry(scan().get(0).pack(), TARGET));
    }

    @Test void cacheValidatesPayloadAndRulesAndKeepsOldOutputOnFailure() throws Exception {
        Path source = temp.resolve("input.zip");
        zip(source, Map.of("pack.mcmeta", meta(1), SOURCE, png(8)));
        var original = scan().get(0);
        var damaged = new HashMap<String, byte[]>();
        try (var zip = new ZipFile(original.pack().toFile())) {
            for (var e : zip.stream().toList()) damaged.put(e.getName(), zip.getInputStream(e).readAllBytes());
        }
        damaged.put(TARGET, png(9));
        zip(original.pack(), damaged);
        assertFalse(scan().get(0).reused());
        assertArrayEquals(png(8), entry(original.pack(), TARGET));
        var revised = ResourceMapping.parse(text(RULE + "\n# revised rules"));
        assertFalse(installer.prepare(temp, revised).get(0).reused());
        zip(source, Map.of("pack.mcmeta", meta(1), SOURCE, text("bad PNG")));
        assertTrue(scan().isEmpty());
        assertArrayEquals(png(8), entry(original.pack(), TARGET));
    }

    @Test void oversizedResourcesAreSkippedBeforeReading() throws Exception {
        zip(temp.resolve("huge.zip"), Map.of("pack.mcmeta", meta(1), SOURCE, new byte[PackConverter.MAX_ENTRY + 1]));
        assertTrue(scan().isEmpty());
        assertEquals(1, warnings.size());
        assertFalse(Files.exists(temp.resolve(PackInstaller.CACHE)));
    }

    @Test void everyPortIncludingAllEightNewOnesWorksWithoutBaseThaumcraft() throws Exception {
        var all = ResourceMapping.bundled();
        var expected = Set.of("thaumcraft", "advancedthaumaturgy", "arcane_engineering", "arcanearteries",
                "automagy", "emt", "forbidden_magic", "forgottenrelics", "gadomancy", "heartwork", "infusionbrewing",
                "nodalmechanics", "taintedmagic", "technomancy", "thaumic_tinkerer", "thaumicbases", "thaumicconcilium",
                "thaumicdyes", "thaumicenergistics", "thaumicexploration", "thaumicfutures", "thaumichorizons",
                "thaumicinfusion", "warptheory", "witchinggadgets");
        assertEquals(expected, all.rules().stream().map(row -> row.target().split("/")[1]).collect(java.util.stream.Collectors.toSet()));
        var covered = new HashSet<String>();
        for (var rule : all.rules()) {
            String ns = rule.target().split("/")[1];
            if (!rule.editions().equals("*") || rule.source().startsWith("assets/thaumcraft/") || !covered.add(ns)) continue;
            zip(temp.resolve(ns + ".zip"), Map.of("pack.mcmeta", meta(1), rule.source(), png(4)));
        }
        var result = installer.prepare(temp, all);
        assertEquals(expected.size() - 1, covered.size());
        assertEquals(covered.size(), result.size());
        assertTrue(warnings.isEmpty());
        for (var converted : result) {
            String ns = converted.source().replace(".zip", "");
            try (var zip = new ZipFile(converted.pack().toFile())) {
                assertTrue(zip.stream().anyMatch(e -> e.getName().startsWith("assets/" + ns + "/")), ns);
            }
        }
    }

    @Test void tc4Tc5AndTc6UsingTheSameAtlasPathReachTheirOwnConsumers() throws Exception {
        String atlas = "assets/thaumcraft/textures/misc/particles.png";
        String focus = "assets/thaumcraft/textures/items/wand/focus_shard.png";
        String brass = "assets/thaumcraft/textures/items/ingot_brass.png";
        zip(temp.resolve("one.zip"), Map.of("pack.mcmeta", meta(1), atlas, png(4)));
        zip(temp.resolve("two.zip"), Map.of("pack.mcmeta", meta(1), atlas, png(5), focus, png(1)));
        zip(temp.resolve("three.zip"), Map.of("pack.mcmeta", meta(3), atlas, png(6), brass, png(2)));
        var packs = installer.prepare(temp, ResourceMapping.bundled());
        assertEquals(3, packs.size());
        for (var result : packs) {
            String name = result.source();
            boolean tc4 = name.equals("one.zip"), tc5 = name.equals("two.zip");
            try (var zip = new ZipFile(result.pack().toFile())) {
                assertEquals(tc4, zip.getEntry(atlas) != null);
                assertEquals(tc5, zip.getEntry("assets/thaumicfutures/textures/misc/tc5_particles.png") != null);
                assertEquals(!tc4 && !tc5, zip.getEntry("assets/thaumicfutures/textures/misc/tc6_particles.png") != null);
            }
            if (tc5) assertArrayEquals(png(1), entry(result.pack(), "assets/thaumicfutures/textures/item/focus_shard.png"));
            if (!tc4 && !tc5) assertArrayEquals(png(2), entry(result.pack(), "assets/thaumicfutures/textures/item/alchemical_brass_ingot.png"));
        }
    }

    @Test void renamed112PathsUseNativeAssetsBeforeLegacyFallbacks() throws Exception {
        zip(temp.resolve("mixed.zip"), Map.of("pack.mcmeta", meta(3), "assets/ttinkerer/textures/items/connector.png", png(4),
                "assets/thaumictinkerer/textures/items/connector.png", png(6), "assets/thaumcraft/textures/blocks/leaves_greatwood.png", png(7),
                "assets/thaumicenergistics/textures/item/cell/4k.png", png(8)));
        var pack = installer.prepare(temp, ResourceMapping.bundled()).get(0).pack();
        assertArrayEquals(png(6), entry(pack, "assets/thaumic_tinkerer/textures/item/connector.png"));
        assertArrayEquals(png(7), entry(pack, "assets/thaumcraft/textures/block/greatwood_leaves.png"));
        assertArrayEquals(png(8), entry(pack, "assets/thaumicenergistics/textures/modern/item/essentia_cell_4k.png"));
    }

    @Test void invalidMappingPathsAndDuplicateRulesAreRejected() {
        for (String invalid : List.of(RULE.replace("/item/", "/../"), RULE.replace("assets/old/", "../old/"),
                RULE + "\n" + RULE, RULE.replace("assets/ported/", "assets/Ported/"), RULE.replace("*", "7")))
            assertThrows(IOException.class, () -> ResourceMapping.parse(text(invalid)));
    }

    @Test void twoConcurrentLaunchesPublishOneCopy() throws Exception {
        zip(temp.resolve("pack.zip"), Map.of("pack.mcmeta", meta(1), SOURCE, png(1)));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(this::scan);
            var b = pool.submit(this::scan);
            var first = a.get(5, TimeUnit.SECONDS).get(0);
            var second = b.get(5, TimeUnit.SECONDS).get(0);
            assertNotEquals(first.reused(), second.reused()); assertEquals(first.pack(), second.pack());
        } finally { pool.shutdownNow(); }
    }

    @Test void startupWaitsAndPumpsProgress() throws Exception {
        var release = new CountDownLatch(1);
        var progress = new CountDownLatch(1);
        var task = new StartupTask<>(() -> { assertTrue(release.await(5, TimeUnit.SECONDS)); return 42; });
        var waiting = CompletableFuture.supplyAsync(() -> task.await(progress::countDown));
        assertTrue(progress.await(2, TimeUnit.SECONDS)); assertFalse(waiting.isDone());
        release.countDown(); assertEquals(42, waiting.get(2, TimeUnit.SECONDS));
    }

    @Test void realModernityArchivePreservesBaselineCoreTextures() throws Exception {
        String fixture = System.getProperty("adapter.fixture");
        org.junit.jupiter.api.Assumptions.assumeTrue(fixture != null, "Optional external archive not supplied");
        Files.copy(Path.of(fixture), temp.resolve("locally supplied artwork.zip"));
        var result = installer.prepare(temp, ResourceMapping.bundled()).get(0);
        assertTrue(result.textures() > 700);
        String referencePath = System.getProperty("adapter.reference");
        if (referencePath != null) {
            int compared = 0;
            try (var reference = new ZipFile(referencePath); var actual = new ZipFile(result.pack().toFile())) {
                for (var e : reference.stream().filter(i -> i.getName().startsWith("assets/thaumcraft/")).toList()) {
                    var target = actual.getEntry(e.getName());
                    assertNotNull(target, e.getName());
                    var expected = reference.getInputStream(e).readAllBytes();
                    var converted = actual.getInputStream(target).readAllBytes();
                    if (e.getName().endsWith(".mcmeta")) {
                        assertEquals(JsonParser.parseString(new String(expected, StandardCharsets.UTF_8)),
                                JsonParser.parseString(new String(converted, StandardCharsets.UTF_8)), e.getName());
                    } else assertArrayEquals(expected, converted, e.getName());
                    compared++;
                }
            }
            assertTrue(compared >= 290);
            System.out.println("Modernity: " + result.textures() + " textures, " + compared + " core entries compared.");
        }
    }
}

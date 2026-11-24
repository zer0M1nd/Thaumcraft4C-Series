package tc4revival.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OriginalResourcesTest {
    @Test void legacyExNihiloWithoutMcmodInfoStillRequiresItsExactIdentity() throws Exception {
        var mapping=Map.of("assets/thaumicfutures/textures/item/porcelain_doll.png","assets/exnihilo/textures/items/ItemDoll.png");
        Path valid=annotationArchive("valid-exnihilo.jar","exnihilo","1.38-53","required-after:Forge@[10.13.0.1180,);");
        assertTrue(OriginalResources.read(valid,mapping,"exnihilo","1.7.10",false).resources().containsKey("assets/exnihilo/textures/items/ItemDoll.png"));
        assertThrows(IOException.class,()->OriginalResources.read(valid,mapping,"exnihilo","1.12.2",false));
        for (Path bad:java.util.List.of(annotationArchive("wrong-id.jar","unrelated","1.38-53","required-after:Forge@[10.13.0.1180,);"),
                annotationArchive("wrong-version.jar","exnihilo","2.0","required-after:Forge@[10.13.0.1180,);"),
                annotationArchive("wrong-forge.jar","exnihilo","1.38-53","required-after:Forge@[14.23,);")))
            assertThrows(IOException.class,()->OriginalResources.read(bad,mapping,"exnihilo","1.7.10",false));
    }
    private Path annotationArchive(String fileName,String modid,String version,String dependencies) throws IOException {
        var writer=new org.objectweb.asm.ClassWriter(0);
        writer.visit(org.objectweb.asm.Opcodes.V1_7,org.objectweb.asm.Opcodes.ACC_PUBLIC,"exnihilo/ExNihilo",null,"java/lang/Object",null);
        var annotation=writer.visitAnnotation("Lcpw/mods/fml/common/Mod;",true);
        annotation.visit("modid",modid);annotation.visit("version",version);annotation.visit("dependencies",dependencies);annotation.visitEnd();writer.visitEnd();
        Path file=game.resolve(fileName);
        try(var zip=new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("exnihilo/ExNihilo.class"));zip.write(writer.toByteArray());zip.closeEntry();
            zip.putNextEntry(new ZipEntry("assets/exnihilo/textures/items/ItemDoll.png"));zip.write(new byte[]{1,2,3});zip.closeEntry();
        }
        return file;
    }
    @Test void unrelatedArchiveIsRejectedBeforeInflatingItsLargePayload() throws Exception {
        Path file = game.resolve("unrelated.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("unrelated.class"));
            byte[] block = new byte[1024 * 1024];
            for (int i = 0; i < 33; i++) zip.write(block);
        }
        var error = assertThrows(IOException.class, () -> OriginalResources.read(file, MAPPING));
        assertTrue(error.getMessage().startsWith("Missing "), error.getMessage());
    }
    @TempDir Path game;
    private static final Map<String, String> MAPPING = Map.of("assets/thaumcraft/textures/item/example.png",
            "assets/thaumcraft/textures/items/example.png");

    private Path jar(String name, String text) throws IOException {
        Path file = game.resolve("thaumcraft").resolve(name);
        Files.createDirectories(file.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("mcmod.info"));
            zip.write("[{\"modid\":\"Thaumcraft\",\"mcversion\":\"1.7.10\"}]".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            for (String entry : new String[] {"assets/thaumcraft/textures/items/example.png",
                    "assets/thaumcraft/lang/en_US.lang", "assets/thaumcraft/lang/zh_CN.lang", "assets/thaumcraft/lang/zh_TW.lang"}) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(text.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

    @Test void arbitraryNamesAndCaseAreAcceptedButNestedAndOtherFilesAreIgnored() throws Exception {
        Path expected = jar("player choice.JAR", "a");
        Files.writeString(expected.getParent().resolve("README.txt"), "ignored");
        Files.createDirectories(expected.getParent().resolve("nested"));
        Files.copy(expected, expected.getParent().resolve("nested/other.jar"));
        assertEquals(java.util.List.of(expected), OriginalResources.candidates(expected.getParent()));
        var source = OriginalResources.read(expected, MAPPING);
        assertFalse(source.reference(), "Hash mismatch must be accepted by the reader");
        assertEquals(OriginalResources.sha256(Files.readAllBytes(expected)), source.sha256());
    }

    @Test void missingDirectoryCanBeRetriedInTheSameProcess() throws Exception {
        assertTrue(OriginalResourceSelection.scan(game, MAPPING).usable().isEmpty());
        Path added = jar("download.jar", "a");
        assertNull(OriginalResourceSelection.scan(game, MAPPING).automatic());
        assertEquals(added, scanKnown(added).automatic().file());
    }

    @Test void corruptFilesAreReportedAndDoNotHideAUsableCandidate() throws Exception {
        Path expected = jar("valid.jar", "a");
        Files.writeString(expected.getParent().resolve("bad.jar"), "incomplete download");
        var scan = scanKnown(expected);
        assertEquals(expected, scan.automatic().file());
        assertEquals(1, scan.errors().size());
        assertTrue(scan.errors().get(0).contains("bad.jar"));
    }

    private OriginalResourceSelection.Scan scanKnown(Path... files) throws IOException {
        var hashes=new java.util.HashSet<String>();
        for (Path file : files) hashes.add(OriginalResources.sha256(Files.readAllBytes(file)));
        return OriginalResourceSelection.scan(game, MAPPING, hashes);
    }

    @Test void onlyPinnedVariantsCanUseARememberedSelection() throws Exception {
        Path first = jar("a.jar", "a"), second = jar("b.jar", "b");
        assertNull(OriginalResourceSelection.scan(game,MAPPING).automatic());
        assertEquals(first, scanKnown(first,second).automatic().file());
        OriginalResourceSelection.remember(game, OriginalResources.read(second, MAPPING));
        assertEquals(second, scanKnown(first,second).automatic().file());
        assertEquals(first, scanKnown(first).automatic().file());
    }

    @Test void incompleteArchiveAndUnsafeResourcePathsAreRejected() throws Exception {
        Path file = game.resolve("incomplete.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("assets/thaumcraft/lang/en_US.lang"));
            zip.write(1);
        }
        assertThrows(IOException.class, () -> OriginalResources.read(file, MAPPING));
        for (String path : new String[] {"assets/../outside", "assets/x/../../outside", "assets/x/", "assets/x\\y", "C:/assets/x"})
            assertFalse(OriginalResources.safePath(path), path);
        assertTrue(OriginalResources.safePath("assets/thaumcraft/textures/items/item.png"));
    }

    @Test void matchingReferenceWinsOverModifiedJarAndIdenticalCopiesChooseStableName() throws Exception {
        Path original = Path.of("references/Thaumcraft-1.7.10-4.2.3.5.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(original), "Private original JAR required");
        Path directory = game.resolve("thaumcraft");
        Files.createDirectories(directory);
        Path modified = directory.resolve("0-modified.jar");
        Files.copy(original, modified);
        Files.writeString(modified, "local trailer", java.nio.file.StandardOpenOption.APPEND);
        Files.copy(original, directory.resolve("b-reference.jar"));
        Files.copy(original, directory.resolve("a-reference.JAR"));
        var scan = OriginalResourceSelection.scan(game, Map.of());
        assertEquals(2, scan.usable().size());
        assertEquals(directory.resolve("a-reference.JAR"), scan.automatic().file());
        assertTrue(scan.automatic().reference());
        assertTrue(scan.errors().stream().anyMatch(message -> message.contains("相同")));
        assertTrue(scan.usable().stream().allMatch(OriginalResources.Snapshot::reference));
    }

    @Test void replacingFileCannotChangeAnAlreadyValidatedSnapshot() throws Exception {
        Path file = jar("snapshot.jar", "original bytes");
        var snapshot = OriginalResources.read(file, MAPPING);
        Files.writeString(file, "replacement download");
        assertArrayEquals("original bytes".getBytes(StandardCharsets.UTF_8), snapshot.resources().get(MAPPING.values().iterator().next()));
        assertNotEquals(OriginalResources.sha256(Files.readAllBytes(file)), snapshot.sha256());
    }
}

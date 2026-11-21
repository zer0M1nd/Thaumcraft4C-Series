package tc4revival.resources;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ThaumicExplorationOriginalIdentityTest {
    @TempDir Path temporary;
    private static final String CLASS="flaxbeard/thaumicexploration/ThaumicExploration.class";
    private static final String HASH="ac85715fdf40a162d4b360b962a14d64a373535d002c04e4b24dfa51aeaa4b01";
    private static final String ASSET="assets/thaumicexploration/lang/en_US.lang";
    private static final Map<String,String> MAPPING=Map.of("assets/test/lang/en_us.json",ASSET);
    private Path fixture(String metadata) throws Exception {
        var writer=new org.objectweb.asm.ClassWriter(0);
        writer.visit(org.objectweb.asm.Opcodes.V1_7,org.objectweb.asm.Opcodes.ACC_PUBLIC,CLASS.substring(0,CLASS.length()-6),null,"java/lang/Object",null);
        var annotation=writer.visitAnnotation("Lcpw/mods/fml/common/Mod;",true);
        annotation.visit("modid","ThaumicExploration");annotation.visit("version","0.6.0");annotation.visitEnd();writer.visitEnd();
        var file=temporary.resolve("legacy.jar");
        try(var zip=new ZipOutputStream(Files.newOutputStream(file))){
            zip.putNextEntry(new ZipEntry(CLASS));zip.write(writer.toByteArray());zip.closeEntry();
            zip.putNextEntry(new ZipEntry("mcmod.info"));zip.write(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();
            zip.putNextEntry(new ZipEntry(ASSET));zip.write("item.example.name=Example".getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();
        }
        return file;
    }
    @Test void explicitPinnedIdentitySupersedesStaleMetadata() throws Exception {
        var file=fixture("[{\"modid\":\"ThaumicExploration\",\"mcversion\":\"1.7.2\"}]");
        var identity=new OriginalResources.AnnotationIdentity(CLASS,"0.6.0",OriginalResources.sha256(Files.readAllBytes(file)));
        assertThrows(IOException.class,()->OriginalResources.read(file,MAPPING,"ThaumicExploration","1.7.10",false));
        assertArrayEquals("item.example.name=Example".getBytes(),OriginalResources.read(file,MAPPING,"ThaumicExploration","1.7.10",false,identity).resources().get(ASSET));
    }
    @Test void annotatedIdentityChecksClassModidAndVersionAndAcceptsDifferentArchiveBytes() throws Exception {
        var file=fixture("[{\"modid\":\"ThaumicExploration\",\"mcversion\":\"1.7.10\"}]");
        var hash=OriginalResources.sha256(Files.readAllBytes(file));
        assertEquals(hash,OriginalResources.read(file,MAPPING,"ThaumicExploration","1.7.10",false,
                new OriginalResources.AnnotationIdentity(CLASS,"0.6.0","0".repeat(64))).sha256());
        for(var identity:java.util.List.of(new OriginalResources.AnnotationIdentity(CLASS,"wrong",hash),new OriginalResources.AnnotationIdentity("wrong/Main.class","0.6.0",hash)))
            assertThrows(IOException.class,()->OriginalResources.read(file,MAPPING,"ThaumicExploration","1.7.10",false,identity));
        assertThrows(IOException.class,()->OriginalResources.read(file,MAPPING,"wrong-modid","1.7.10",false,new OriginalResources.AnnotationIdentity(CLASS,"0.6.0",hash)));
    }
    @Test void officialReleaseLoadsDespiteItsStale172Metadata() throws Exception {
        var file=Path.of("../../References/Collaboration/jars/ThaumicExploration-1.7.10-1.1-53.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(file),"Private original JAR required");
        var declaration=Path.of("../Addons/ThaumicExploration/src/main/resources/data/thaumicexploration/thaumcraft/original_resources.json");
        var target=OriginalDownloadTargets.addons("thaumicexploration",declaration).get(0);
        var snapshot=target.read(file);
        assertEquals(HASH,snapshot.sha256());assertTrue(snapshot.resources().get(ASSET).length>1000);
        assertTrue(snapshot.resources().keySet().containsAll(target.mapping().values()));
        var processed=ProcessedOriginalResources.store(temporary,target,snapshot);
        assertEquals(HASH,target.read(processed).sha256());
        try(var paths=Files.walk(processed)){assertFalse(paths.anyMatch(p->p.toString().endsWith(".class")));}
    }
}

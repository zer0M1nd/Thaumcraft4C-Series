package tc4revival.resources;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

class InfusionBrewingOriginalIdentityTest {
    @TempDir Path directory;
    private static final String ENTRY="mariri/infusionbrewing/InfusionBrewing.class";
    private static final String ASSET="assets/mariri/test.png";
    private Path archive(String id,String version,String dependencies) throws IOException {
        var writer=new ClassWriter(0);
        writer.visit(Opcodes.V1_7,Opcodes.ACC_PUBLIC,"mariri/infusionbrewing/InfusionBrewing",null,"java/lang/Object",null);
        var annotation=writer.visitAnnotation("Lcpw/mods/fml/common/Mod;",true);
        annotation.visit("modid",id);annotation.visit("version",version);annotation.visit("dependencies",dependencies);
        annotation.visitEnd();writer.visitEnd();
        var path=directory.resolve(UUID.randomUUID()+".jar");
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))){
            zip.putNextEntry(new ZipEntry(ENTRY));zip.write(writer.toByteArray());zip.closeEntry();
            zip.putNextEntry(new ZipEntry(ASSET));zip.write(new byte[]{1,2,3});zip.closeEntry();
        }
        return path;
    }
    private OriginalResources.Snapshot read(Path file,String mc) throws IOException {
        return OriginalResources.read(file,Map.of("assets/infusionbrewing/test.png",ASSET),"InfusionBrewing",mc,false,
                new OriginalResources.AnnotationIdentity(ENTRY,"1.7.10-1.3a",OriginalResources.sha256(Files.readAllBytes(file))));
    }
    @Test void acceptsVerifiedLegacyAnnotationWithoutExecutingClass() throws Exception {
        assertArrayEquals(new byte[]{1,2,3},read(archive("InfusionBrewing","1.7.10-1.3a","required-after:Thaumcraft"),"1.7.10").resources().get(ASSET));
    }
    @Test void rejectsOtherModVersionAndHash() throws Exception {
        for(var fields:List.of(new String[]{"unrelated","1.7.10-1.3a","required-after:Thaumcraft"},
                new String[]{"InfusionBrewing","1.12.2-1.3a","required-after:Thaumcraft"}))
            assertThrows(IOException.class,()->read(archive(fields[0],fields[1],fields[2]),"1.7.10"));
        var valid=archive("InfusionBrewing","1.7.10-1.3a","required-after:Thaumcraft");
        assertThrows(IOException.class,()->OriginalResources.read(valid,Map.of("assets/infusionbrewing/test.png",ASSET),"InfusionBrewing","1.7.10",false,new OriginalResources.AnnotationIdentity(ENTRY,"1.7.10-1.3a","0".repeat(64))));
        assertThrows(IllegalArgumentException.class,()->new OriginalResources.AnnotationIdentity("../Bad.class","1","0".repeat(64)));
    }
    @Test void readsCompleteActualAuthorArchive() throws Exception {
        var declaration=com.google.gson.JsonParser.parseString(Files.readString(Path.of("../Addons/InfusionBrewing/src/main/resources/data/infusionbrewing/thaumcraft/original_resources.json"))).getAsJsonObject();
        var requirement=AddonResourceRequirements.decode("infusionbrewing",declaration).get(0);
        var original=Path.of("../../References/Collaboration/jars/InfusionBrewing-1.7.10-1.3a.jar");
        var result=OriginalResources.read(original,requirement.mapping(),requirement.legacyModId(),requirement.minecraftVersion(),false,requirement.annotationIdentity());
        assertEquals(requirement.sha256(),result.sha256());
        assertTrue(result.resources().keySet().containsAll(requirement.mapping().values()));
        // Exercise the same discovery/download target path used by the first client startup.
        var target=OriginalDownloadTargets.addons("infusionbrewing",Path.of("../Addons/InfusionBrewing/src/main/resources/data/infusionbrewing/thaumcraft/original_resources.json")).get(0);
        var processed=ProcessedOriginalResources.store(directory,target,target.read(original));
        assertEquals(result.sha256(),target.read(processed).sha256());
        try(var files=Files.walk(processed)){assertFalse(files.anyMatch(p->p.toString().endsWith(".class")));}
        assertTrue(AddonResourceRequirements.scan(directory,List.of(requirement)).missing().isEmpty());
    }
}

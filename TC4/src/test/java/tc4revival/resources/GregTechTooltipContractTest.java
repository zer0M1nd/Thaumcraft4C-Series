package tc4revival.resources;

import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Check the installed third-party bytecode, rather than assuming the tooltip hook's shape. */
class GregTechTooltipContractTest {
    @Test void installedGregTechHasExactlyOneBucketReadAndSkipsEmptyFluid() throws Exception {
        Path jar = Path.of(System.getProperty("tc4.gtceuJar",
                "../Addons/ThaumicTinkerer/.tooling/integration-runtime/gtceu-1.20.1-7.5.3.jar"));
        assertTrue(java.nio.file.Files.isRegularFile(jar), "GTCEu integration fixture: " + jar);
        try (var zip = new ZipFile(jar.toFile());
             var input = zip.getInputStream(zip.getEntry("com/gregtechceu/gtceu/client/TooltipsHandler.class"))) {
            var type = new ClassNode();
            new ClassReader(input).accept(type, 0);
            var method = type.methods.stream().filter(m -> m.name.equals("appendTooltips")).findFirst().orElseThrow();
            int reads = 0;
            boolean emptyGuard = false;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/world/item/BucketItem")
                        && call.name.equals("getFluid") && call.desc.equals("()Lnet/minecraft/world/level/material/Fluid;")) reads++;
                if (instruction instanceof TypeInsnNode check && check.getOpcode() == Opcodes.INSTANCEOF
                        && check.desc.equals("net/minecraft/world/level/material/EmptyFluid")) emptyGuard = true;
            }
            assertEquals(1, reads, "Redirect must match the actual installed GTCEu version exactly once");
            assertTrue(emptyGuard, "GTCEu must skip fluid-stack construction for EMPTY");
        }
    }
}

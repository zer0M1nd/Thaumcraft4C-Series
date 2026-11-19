package tc4revival.resources;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** The export must never trigger the server -> recipe packet -> JEI -> export cycle. */
class JeiExportBoundaryTest {
    @Test void exportCannotRefreshTheServerOrPerformSynchronousFileIo() throws Exception {
        try(var input=getClass().getResourceAsStream("/tc4revival/compat/jei/JeiInferenceExport.class")) {
            assertNotNull(input);var type=new ClassNode();new ClassReader(input).accept(type,0);
            boolean asynchronousSave=false;
            for(var method:type.methods)for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call){
                assertNotEquals("getSingleplayerServer",call.name,"Export must not obtain a server for live refresh");
                assertNotEquals("tc4revival/aspect/sync/ServerAspectData",call.owner);
                assertNotEquals("tc4revival/aspect/inference/AdvancedAspectInference#rebuild",call.owner+"#"+call.name);
                if(call.owner.equals("tc4revival/aspect/inference/InferenceFiles")){
                    assertFalse(call.name.equals("read")||call.name.equals("write"),"Half-gigabyte index IO must stay off render thread");
                    asynchronousSave|=call.name.equals("writeAsync");
                }
            }
            assertTrue(asynchronousSave);
        }
    }
}

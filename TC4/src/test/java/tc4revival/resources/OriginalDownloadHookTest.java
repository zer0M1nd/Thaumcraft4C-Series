package tc4revival.resources;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

/** Catch a changed Forge hook descriptor/order without starting a client or initializing client classes. */
class OriginalDownloadHookTest {
    @Test void supportedForgeEmitsClientPackEventAtTheInjectedBeginCallsite() throws Exception {
        try(var input=getClass().getClassLoader().getResourceAsStream("net/minecraftforge/client/loading/ClientModLoader.class")) {
            assertNotNull(input);
            var calls=new ArrayList<String>();
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                    if(!name.equals("begin"))return null;
                    assertEquals("(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/packs/resources/ReloadableResourceManager;)V",descriptor);
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean iface) {calls.add(owner+"."+name+desc);}
                    };
                }
            },0);
            String hook="net/minecraftforge/fml/ModLoader.postEvent(Lnet/minecraftforge/eventbus/api/Event;)V";
            assertEquals(1,calls.stream().filter(hook::equals).count(),"Mixin injection must find exactly one pack-event dispatch");
            assertTrue(calls.stream().anyMatch(c->c.startsWith("net/minecraftforge/event/AddPackFindersEvent.<init>")));
        }
    }
}

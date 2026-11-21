package conciliummodern;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Inspect bytecode without loading optional-host or client-only classes in the JUnit process. */
class EventSubscriberContractTest {
    @Test void forgeGeneratedListenerNamesAreUniqueAcrossAllProductionSubscribers()throws Exception{
        var wrappers=new HashMap<String,String>();int count=0;
        try(var files=Files.walk(Path.of("build/classes/java/main"))){
            for(var file:files.filter(p->p.toString().endsWith(".class")).toList()){
                var owner=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(owner,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                String simple=owner.name.substring(owner.name.lastIndexOf('/')+1);
                for(var inner:owner.innerClasses)if(inner.name.equals(owner.name)&&inner.innerName!=null)simple=inner.innerName;
                for(var method:owner.methods){
                    if(method.visibleAnnotations==null||method.visibleAnnotations.stream().noneMatch(a->a.desc.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")))continue;
                    var parameters=Type.getArgumentTypes(method.desc);assertEquals(1,parameters.length,owner.name+"."+method.name);
                    String event=parameters[0].getInternalName();event=event.substring(Math.max(event.lastIndexOf('/'),event.lastIndexOf('$'))+1);
                    // Forge EventBus 6.2.33 IEventListenerFactory.getUniqueName omits enclosing class names.
                    String key=owner.name.substring(0,owner.name.lastIndexOf('/')+1)+"__"+simple+"_"+method.name+"_"+event;
                    String actual=owner.name+"."+method.name+method.desc,previous=wrappers.putIfAbsent(key,actual);
                    assertNull(previous,()->"Forge listener wrapper collision: "+key+" maps to both "+previous+" and "+actual);count++;
                }
            }
        }
        assertTrue(count>20,"The production bytecode subscriber scan must not silently inspect an empty output directory");
    }
}

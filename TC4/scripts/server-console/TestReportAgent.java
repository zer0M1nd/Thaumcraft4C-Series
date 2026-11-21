import java.lang.instrument.Instrumentation;
import java.lang.reflect.Proxy;
import java.nio.file.*;

/** Records successful tests as well as failures, which vanilla's console reporter omits. */
public final class TestReportAgent {
    public static void agentmain(String resultFile,Instrumentation instrumentation) throws Exception {
        ClassLoader loader=null;
        for (Class<?> type:instrumentation.getAllLoadedClasses()) if (type.getName().equals("net.minecraftforge.server.ServerLifecycleHooks")) { loader=type.getClassLoader(); break; }
        if (loader==null) throw new IllegalStateException("Forge not loaded");
        var reporter=Class.forName("net.minecraft.gametest.framework.TestReporter",true,loader);
        Object proxy=Proxy.newProxyInstance(loader,new Class<?>[]{reporter},(instance,method,args)->{
            if (method.getName().equals("onTestSuccess") || method.getName().equals("onTestFailed")) {
                Object info=args[0]; String name=(String)info.getClass().getMethod("getTestName").invoke(info);
                Object error=info.getClass().getMethod("getError").invoke(info);
                Files.writeString(Path.of(resultFile),method.getName()+" "+name+" "+error+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            return null;
        });
        Class.forName("net.minecraft.gametest.framework.GlobalTestReporter",true,loader).getMethod("replaceWith",reporter).invoke(null,proxy);
    }
}

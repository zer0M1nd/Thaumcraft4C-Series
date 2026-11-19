import java.lang.instrument.Instrumentation;
import java.nio.file.*;

/** Local development fallback when the host terminal loses Java's standard input. */
public final class ServerConsoleAgent {
    public static void agentmain(String commandFile,Instrumentation instrumentation) throws Exception {
        Class<?> hooks=null;
        for (Class<?> type:instrumentation.getAllLoadedClasses())
            if (type.getName().equals("net.minecraftforge.server.ServerLifecycleHooks")) { hooks=type; break; }
        if (hooks==null) throw new IllegalStateException("Forge server lifecycle not loaded");
        Object server=hooks.getMethod("getCurrentServer").invoke(null);
        if (server==null) throw new IllegalStateException("No active Forge server");
        Object source=server.getClass().getMethod("createCommandSourceStack").invoke(server);
        var enqueue=server.getClass().getMethod("handleConsoleInput",String.class,source.getClass());
        for (String command:Files.readAllLines(Path.of(commandFile)))
            if (!command.isBlank() && !command.startsWith("#")) enqueue.invoke(server,command,source);
    }
}

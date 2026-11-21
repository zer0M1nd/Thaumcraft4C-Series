package tc4validation;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

/** Recovery for an isolated acceptance server launched with a closed stdin pipe. */
public final class StopIsolatedServer {
    public static void agentmain(String expectedDirectory, Instrumentation instrumentation) throws Exception {
        if (!Boolean.getBoolean("tc4.validationHarness") ||
            !Path.of(System.getProperty("user.dir")).toRealPath().equals(Path.of(expectedDirectory).toRealPath()))
            throw new IllegalStateException("Refusing to stop a non-matching validation server");
        for (Class<?> type : instrumentation.getAllLoadedClasses()) {
            if (!type.getName().equals("net.minecraftforge.server.ServerLifecycleHooks")) continue;
            Object server = type.getMethod("getCurrentServer").invoke(null);
            if (server == null) throw new IllegalStateException("No running server");
            server.getClass().getMethod("m_7570_", boolean.class).invoke(server, false);
            return;
        }
        throw new IllegalStateException("Server lifecycle class absent");
    }
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 3) throw new IllegalArgumentException("pid agentJar expectedDirectory");
        var vm = com.sun.tools.attach.VirtualMachine.attach(arguments[0]);
        try { vm.loadAgent(arguments[1], arguments[2]); } finally { vm.detach(); }
    }
}

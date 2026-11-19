package thaumcraft.api.transport;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.util.ExtensionRegistry;

/** Pure port classification shared by tube visuals and actual transfer lookup. */
public final class EssentiaConnections {
    public enum Decision { PASS, CONNECT, DISCONNECT }
    public record Context(BlockEntity neighbor,Direction neighborFace) {}
    private static final ExtensionRegistry<Function<Context,Decision>> RULES=new ExtensionRegistry<>();
    private EssentiaConnections() {}
    public static void register(String id,int priority,Function<Context,Decision> rule){RULES.register(id,priority,rule);}
    public static boolean connects(BlockEntity neighbor,Direction face,boolean fallback){
        if(neighbor==null||face==null)return false;
        for(var entry:RULES.entries()){var decision=Objects.requireNonNull(entry.value().apply(new Context(neighbor,face)));if(decision!=Decision.PASS)return decision==Decision.CONNECT;}
        return fallback;
    }
}

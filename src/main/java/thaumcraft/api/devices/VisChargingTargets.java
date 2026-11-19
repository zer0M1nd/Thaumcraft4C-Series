package thaumcraft.api.devices;

import java.util.*;
import java.util.function.Function;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.aspect.Aspect;
import tc4revival.content.WorkbenchChargerBlockEntity;
import thaumcraft.api.util.ExtensionRegistry;

/** A matched adapter owns its target's slot identities and save/sync; all payment uses the source's tick budget. */
public final class VisChargingTargets {
    public record Context(WorkbenchChargerBlockEntity source,BlockEntity target) {
        public int allowance(Aspect aspect,int requested){return source.chargeAllowance(aspect,requested);}
        public int pay(Aspect aspect,int requested){return source.consumeVis(aspect,requested);}
        public void changed(){target.setChanged();var level=target.getLevel();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(target.getBlockPos(),target.getBlockState(),target.getBlockState(),2);}
    }
    public interface Target {
        /** Recheck inventory identity before drawing; rejected/simulated targets never draw. */
        boolean charge(Context context);
    }
    private static final ExtensionRegistry<Function<Context,Optional<Target>>> ADAPTERS=new ExtensionRegistry<>();
    private VisChargingTargets() {}
    public static void register(String id,int priority,Function<Context,Optional<Target>> adapter){ADAPTERS.register(id,priority,adapter);}
    public static boolean process(WorkbenchChargerBlockEntity source,BlockEntity target){
        if(target==null)return false;var context=new Context(source,target);
        for(var entry:ADAPTERS.entries()){
            var selected=Objects.requireNonNull(entry.value().apply(context));if(selected.isEmpty())continue;
            if(selected.get().charge(context))context.changed();return true;
        }
        return false;
    }
}

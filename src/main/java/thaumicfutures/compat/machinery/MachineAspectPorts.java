package thaumicfutures.compat.machinery;

import com.reborn.modularmachinery.machine.HatchCollection;
import com.reborn.modularmachinery.machine.MachineDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;

public final class MachineAspectPorts {
    private static final Map<HatchCollection, List<AspectPort>> PORTS = Collections.synchronizedMap(new WeakHashMap<>());
    private MachineAspectPorts() {}
    public static void collect(HatchCollection collection, Level level, BlockPos controller, MachineDefinition machine) {
        if (machine == null || collection == HatchCollection.EMPTY) return;
        var ports = new ArrayList<AspectPort>();
        for (BlockPos relative : machine.pattern().positions().keySet()) {
            BlockPos position = controller.offset(relative);
            if (level.hasChunkAt(position) && level.getBlockEntity(position) instanceof AspectPort port
                    && port.kind() != AspectPort.Kind.BRIDGE) ports.add(port);
        }
        ports.sort(java.util.Comparator.comparing(AspectPort::getBlockPos));
        PORTS.put(collection, List.copyOf(ports));
    }
    private static List<AspectPort> matching(HatchCollection collection, AspectPort.Kind kind) {
        return PORTS.getOrDefault(collection, List.of()).stream().filter(port -> {
            Level level = port.getLevel();
            return !port.isRemoved() && port.kind() == kind && level != null && level.hasChunkAt(port.getBlockPos())
                    && level.getBlockEntity(port.getBlockPos()) == port;
        }).toList();
    }
    public static long available(HatchCollection collection, AspectPort.Kind kind, Aspect aspect) {
        if (aspect == null) return 0;
        long amount = 0;
        for (var port : matching(collection, kind)) amount += kind.input() ? port.amount(aspect) : port.space(aspect);
        return amount;
    }
    public static boolean transfer(HatchCollection collection, AspectPort.Kind kind, Aspect aspect, long amount) {
        if (amount < 0 || aspect == null || available(collection, kind, aspect) < amount) return false;
        for (var port : matching(collection, kind)) {
            int request = (int) Math.min(Integer.MAX_VALUE, amount);
            amount -= kind.input() ? port.extract(aspect, request, false) : port.insert(aspect, request, false);
            if (amount == 0) return true;
        }
        return amount == 0;
    }
}

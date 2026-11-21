package thaumicfutures.compat.machinery;

import com.reborn.modularmachinery.machine.HatchCollection;
import com.reborn.modularmachinery.recipe.MachineRecipe;
import java.util.HashMap;

/** Reserve the sum of repeated demands, conservatively including probabilistic outputs. */
public final class AspectRecipeBudget {
    private record Key(AspectPort.Kind kind, String aspect) {}
    private AspectRecipeBudget() {}
    public static boolean fits(MachineRecipe recipe, HatchCollection ports, int phase, int parallel) {
        var required = new HashMap<Key, Long>();
        for (var raw : recipe.requirements()) {
            if (!(raw instanceof AspectRequirement requirement)) continue;
            if(!thaumicfutures.FutureFeatures.Group.MACHINERY.enabled())return false;
            boolean active = switch (phase) {
                case 0 -> requirement.isStartPhase(); case 1 -> requirement.isTickPhase(); default -> requirement.isFinishPhase();
            };
            if (!active) continue;
            var aspect = requirement.aspect();
            if (aspect == null) return false;
            Key key = new Key(requirement.portKind(), requirement.aspectTag);
            long sum = required.getOrDefault(key, 0L) + requirement.amountFor(parallel);
            if (sum < 0 || MachineAspectPorts.available(ports, requirement.portKind(), aspect) < sum) return false;
            required.put(key, sum);
        }
        return true;
    }
    public static int limit(MachineRecipe recipe, HatchCollection ports, int ceiling) {
        if (recipe.requirements().stream().noneMatch(AspectRequirement.class::isInstance)) return ceiling;
        int low = 0, high = Math.max(0, ceiling);
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            if (fits(recipe, ports, 0, middle) && fits(recipe, ports, 1, middle) && fits(recipe, ports, 2, middle)) low = middle;
            else high = middle - 1;
        }
        return low;
    }
}

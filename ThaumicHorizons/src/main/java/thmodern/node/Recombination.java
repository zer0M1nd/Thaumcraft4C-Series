package thmodern.node;

import java.util.*;
import tc4revival.aspect.*;

/** A plan retains which node supplies each component, including reversed component order. */
public final class Recombination {
    public record Plan(Aspect receiver, Aspect donor, Aspect result) {}
    public static List<Plan> candidates(AspectCatalog catalog, AspectAmounts receiver,
            AspectAmounts receiverCharge, AspectAmounts donor, AspectAmounts donorCharge) {
        for (int priority = 0; priority < 4; priority++) {
            var plans = new LinkedHashMap<Aspect, Plan>();
            for (var a : receiver.aspects()) for (var b : donor.aspects()) {
                if (receiver.amount(a) <= 0 || donor.amount(b) <= 0
                        || receiverCharge.amount(a) <= 0 || donorCharge.amount(b) <= 0) continue;
                if (priority < 2 && !a.isPrimal() || (priority == 0 || priority == 2) && !b.isPrimal()) continue;
                catalog.combine(a, b).ifPresent(result -> {
                    if (receiver.amount(result) < Integer.MAX_VALUE)
                        plans.putIfAbsent(result, new Plan(a, b, result));
                });
            }
            if (!plans.isEmpty()) return List.copyOf(plans.values());
        }
        return List.of();
    }
    private Recombination() {}
}

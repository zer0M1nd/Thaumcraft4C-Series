package tc4revival.client;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.*;

/** Per book-opening source index, as in TC4. Never rebuild or enumerate stacks from a render call. */
public class ResearchAspectSources {
    private int version = -1;
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    private final Map<String,List<ItemStack>> sources = new HashMap<>();
    private final Set<String> seen = new HashSet<>();
    private PlayerKnowledge knowledge;
    public void tick() {
        if (!ClientItemAspects.ready()) return;
        if (version != ClientItemAspects.version()) {
            knowledge = ClientKnowledge.displaySnapshot().orElse(null);
            if (knowledge == null) return;
            version = ClientItemAspects.version(); pending.clear(); sources.clear(); seen.clear();
            BrowserQueryStacks.enumerate(pending::addLast);
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) for (var stack : player.getInventory().items) if (!stack.isEmpty()) pending.addLast(stack.copy());
        }
        long start = System.nanoTime();
        for (int i = 0; i < 32 && !pending.isEmpty(); i++) {
            var stack = pending.removeFirst();
            ClientItemAspects.resolve(stack).ifPresent(result -> {
                if (!knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT, result.identity()) || !seen.add(result.identity())) return;
                for (var aspect : result.amounts().aspects()) if (knowledge.knows(aspect))
                    sources.computeIfAbsent(aspect.tag(), ignored -> new ArrayList<>())
                            .add(stack.copyWithCount(result.amounts().amount(aspect)));
            });
            if (System.nanoTime() - start >= 2_000_000L) break;
        }
    }
    public List<ItemStack> get(String aspect) { return sources.getOrDefault(aspect,List.of()); }
}

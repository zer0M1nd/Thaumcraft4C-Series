package tc4revival.research;

import java.util.ArrayList;
import java.util.function.IntUnaryOperator;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.AspectCatalog;

/** Temporary server-session test override; never changes catalog definitions or existing notes. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public class ResearchNoteTestSeeds {
    private static int negativeCount, innerCount;
    public static void set(int negative, int inner) {
        if (negative < 0 || inner < 0 || negative > 32 || inner > 32 || negative + inner > 37)
            throw new IllegalArgumentException("Test seed counts must be 0..32 and total at most 37");
        negativeCount = negative;
        innerCount = inner;
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { set(0, 0); }
    public static boolean enabled() { return negativeCount != 0 || innerCount != 0; }
    public static ResearchDefinition apply(ResearchDefinition base, AspectCatalog catalog, IntUnaryOperator random) {
        if (!enabled() || base.mirror() || base.noteLayout().isPresent()) return base;
        var tags = catalog.all().stream().map(tc4revival.aspect.Aspect::tag).sorted().toList();
        if (tags.isEmpty()) return base;
        var inner = new ArrayList<>(base.noteSeeds().inner());
        var negative = new ArrayList<>(base.noteSeeds().negative());
        for (int i = 0; i < innerCount; i++) inner.add(tags.get(random.applyAsInt(tags.size())));
        for (int i = 0; i < negativeCount; i++) negative.add(tags.get(random.applyAsInt(tags.size())));
        return new ResearchDefinition(base.key(), base.category(), base.aspects(), base.parents(), base.hiddenParents(),
                base.siblings(), base.flags(), base.complexity(), base.warp(), base.display(), base.triggers(),
                base.parentsAny(), new ResearchDefinition.NoteSeeds(inner, negative), base.reference());
    }
    public static int radius(ResearchDefinition definition) {
        // Legacy ResearchItem clamps a declared zero complexity to one before note generation.
        int radius = Math.max(1,definition.complexity()) + 1;
        if (enabled()) {
            int count = definition.noteSeeds().inner().size() + definition.noteSeeds().negative().size();
            while (1 + 3 * (radius - 1) * radius < count) radius++;
        }
        return radius;
    }
}

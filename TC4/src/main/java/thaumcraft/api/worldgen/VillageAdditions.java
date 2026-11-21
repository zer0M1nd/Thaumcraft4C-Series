package thaumcraft.api.worldgen;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/** Register deterministic additions during common setup. Callbacks must respect piece collisions
 * and use the supplied generation context; worldgen may invoke them concurrently. */
public class VillageAdditions {
    private static final CopyOnWriteArrayList<BiConsumer<Structure.GenerationContext, StructurePiecesBuilder>> CALLBACKS = new CopyOnWriteArrayList<>();
    public static void register(BiConsumer<Structure.GenerationContext, StructurePiecesBuilder> callback) { CALLBACKS.add(Objects.requireNonNull(callback)); }
    public static void generate(Structure.GenerationContext context, StructurePiecesBuilder builder) {
        for (var callback : CALLBACKS) callback.accept(context, builder);
    }
}

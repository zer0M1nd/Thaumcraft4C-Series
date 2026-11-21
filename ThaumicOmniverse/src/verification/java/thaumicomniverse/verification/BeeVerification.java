package thaumicomniverse.verification;

import forestry.api.IForestryApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import thaumicfutures.compat.bees.*;
import static thaumicomniverse.verification.ServerVerification.*;

final class BeeVerification {
    static void run(MinecraftServer server) {
        check("36 native bee species and mutation registration", () -> {
            long count = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes().stream()
                    .flatMap(type -> type.getAllSpeciesIds().stream()).filter(key -> key.getNamespace().equals("thaumicomniverse")).count();
            require(count == 36, "Expected 36 native species, got " + count);
            for (var type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
                verifySelfParents(type);
                for (String name : java.util.List.of("air", "fire", "water", "earth", "order", "chaos", "vis", "rejuvenating",
                        "empowering", "nexus", "taint", "pure", "hungry", "wispy", "void")) {
                    var species = type.getSpeciesSafe(ThaumicBeeSpecies.id(name));
                    if (species != null) require(type.getMutations().getAllMutations().stream()
                            .anyMatch(mutation -> mutation.getResult().equals(species)), "No breeding recipe for " + name);
                }
            }
        });
        check("moon interval wrap and noon boundary", () -> {
            var condition = MoonMutationCondition.require(6, 1);
            require(condition.includes(7) && condition.includes(0) && !condition.includes(3), "Wrapped interval");
            require(MoonMutationCondition.phase(29999) == 0 && MoonMutationCondition.phase(30000) == 1, "Noon calendar boundary");
            require(MoonMutationCondition.phase(0) == 0 && MoonMutationCondition.phase(Long.MAX_VALUE) >= 0, "Calendar overflow");
        });
        check("apiary aura charge renewal and NBT", () -> {
            var level = server.overworld(); var pos = new BlockPos(40, 100, 40);
            level.setBlockAndUpdate(pos, BeeAuraContent.APIARY.get().defaultBlockState());
            level.setBlockAndUpdate(pos.east(), BeeAuraContent.PROVIDER.get().defaultBlockState());
            var apiary = (MagicApiary) level.getBlockEntity(pos);
            var provider = (VisAuraProvider) level.getBlockEntity(pos.east());
            var data = new CompoundTag(); data.putIntArray("Charges", new int[] {3, 3, 3}); data.putIntArray("PartialVis", new int[] {100, -1, 5});
            provider.load(data);
            require(java.util.Arrays.equals(provider.saveWithoutMetadata().getIntArray("PartialVis"), new int[] {7, 0, 5}), "Provider NBT clamp");
            long before = level.getGameTime();
            try {
                server.getWorldData().overworldData().setGameTime(100);
                apiary.serverTick(level, pos, apiary.getBlockState());
                require(apiary.boostFlags() == 7, "Missing aura boosts");
                apiary.serverTick(level, pos, apiary.getBlockState());
                require(java.util.Arrays.equals(provider.saveWithoutMetadata().getIntArray("Charges"), new int[] {2, 2, 2}), "Repeated charge drain");
                var copy = new MagicApiary(pos, apiary.getBlockState()); copy.setLevel(level); copy.load(apiary.saveWithoutMetadata());
                require(copy.boostFlags() == 7, "Active boost persistence");
                server.getWorldData().overworldData().setGameTime(1000);
                apiary.serverTick(level, pos, apiary.getBlockState());
                require(apiary.boostFlags() == 7 && provider.saveWithoutMetadata().getIntArray("Charges")[0] == 1, "Expired boost renewal");
            } finally {
                server.getWorldData().overworldData().setGameTime(before);
                level.removeBlock(pos, false); level.removeBlock(pos.east(), false);
            }
        });
    }
    private static <S extends forestry.api.genetics.ISpecies<?>> void verifySelfParents(forestry.api.genetics.ISpeciesType<S, ?> type) {
        for (String name : java.util.List.of("windy", "firey", "watery", "earthy")) {
            var parent = type.getSpeciesSafe(ThaumicBeeMutations.parent(name));
            if (parent == null) continue;
            var matches = type.getMutations().getCombinations(parent, parent);
            require(matches.size() == 1, "Self mutation missing or duplicated: " + name);
            require(Math.abs(matches.get(0).getChance() - .08f) < .00001f, "Wrong self mutation chance");
            var from = type.getMutations().getMutationsFrom(parent);
            require(from.size() == from.stream().distinct().count(), "Duplicate parent recipe listing");
        }
    }
}

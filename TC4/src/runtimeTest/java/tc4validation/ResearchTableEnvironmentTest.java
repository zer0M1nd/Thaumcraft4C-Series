package tc4validation;

import tc4revival.research.ResearchTableBonuses.Source;

/** Requires the real Forge registries and loaded data-pack tags. Not a plain-JVM test. */
public final class ResearchTableEnvironmentTest {
    private static int checks;
    public static int run() {
        checks=0;
        nativeBlockKindsIncludeModernVariantsWithoutLegacyWhitelist();
        currentTagsControlDirtButtonsClimbablesAndFluids();
        return checks;
    }
    private static void assertEquals(Object expected,Object actual) {
        if(!expected.equals(actual))throw new AssertionError("Expected "+expected+", got "+actual);
        checks++;
    }
    private static Source classify(String id) {
        try {
            var m=tc4revival.content.ResearchTableEnvironment.class.getDeclaredMethod("vanilla",String.class);
            m.setAccessible(true);return (Source)m.invoke(null,id);
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    static void nativeBlockKindsIncludeModernVariantsWithoutLegacyWhitelist() {
        for (String block : java.util.List.of("torch", "wall_torch", "potted_fern", "wither_skeleton_wall_skull", "moving_piston",
                "soul_torch", "potted_mangrove_propagule", "piglin_head"))
            assertEquals(Source.primal("ordo", 20), classify("minecraft:" + block));
        for (String block : java.util.List.of("stone", "copper_block", "glass"))
            assertEquals(Source.NONE, classify("minecraft:" + block));
        assertEquals(Source.primal("terra", 20), classify("minecraft:farmland"));
    }
    static void currentTagsControlDirtButtonsClimbablesAndFluids() {
        var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var fluids = net.minecraft.core.registries.BuiltInRegistries.FLUID;
        var oldBlocks = blocks.getTags().collect(java.util.stream.Collectors.toMap(com.mojang.datafixers.util.Pair::getFirst,
                pair -> pair.getSecond().stream().toList()));
        var oldFluids = fluids.getTags().collect(java.util.stream.Collectors.toMap(com.mojang.datafixers.util.Pair::getFirst,
                pair -> pair.getSecond().stream().toList()));
        var tagged = new java.util.HashMap<>(oldBlocks);
        tagged.put(net.minecraft.tags.BlockTags.DIRT, java.util.List.of(net.minecraft.world.level.block.Blocks.GRASS_BLOCK.builtInRegistryHolder(),
                net.minecraft.world.level.block.Blocks.ROOTED_DIRT.builtInRegistryHolder(), net.minecraft.world.level.block.Blocks.PODZOL.builtInRegistryHolder(), net.minecraft.world.level.block.Blocks.COPPER_BLOCK.builtInRegistryHolder()));
        tagged.put(net.minecraft.tags.BlockTags.BUTTONS, java.util.List.of(net.minecraft.world.level.block.Blocks.CHERRY_BUTTON.builtInRegistryHolder()));
        tagged.put(net.minecraft.tags.BlockTags.CLIMBABLE, java.util.List.of(net.minecraft.world.level.block.Blocks.LADDER.builtInRegistryHolder()));
        var wet = new java.util.HashMap<>(oldFluids);
        wet.put(net.minecraft.tags.FluidTags.WATER, java.util.List.of(net.minecraft.world.level.material.Fluids.WATER.builtInRegistryHolder()));
        wet.put(net.minecraft.tags.FluidTags.LAVA, java.util.List.of(net.minecraft.world.level.material.Fluids.LAVA.builtInRegistryHolder()));
        try {
            blocks.bindTags(tagged); fluids.bindTags(wet);
            for (String block : java.util.List.of("grass_block", "rooted_dirt", "podzol", "copper_block"))
                assertEquals(Source.primal("terra", 20), classify("minecraft:" + block));
            for (String block : java.util.List.of("cherry_button", "ladder"))
                assertEquals(Source.primal("ordo", 20), classify("minecraft:" + block));
            assertEquals(Source.primal("aqua", 15), classify("minecraft:water"));
            assertEquals(Source.primal("ignis", 20), classify("minecraft:lava"));
        } finally { blocks.bindTags(oldBlocks); fluids.bindTags(oldFluids); }
        assertEquals(Source.NONE, classify("minecraft:copper_block"));
    }
}

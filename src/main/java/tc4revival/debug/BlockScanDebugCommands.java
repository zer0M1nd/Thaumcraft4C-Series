package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.BlockScanTargets;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.recipe.RecipeAspects;

 class BlockScanDebugCommands {
    private BlockScanDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("block_scan")
                .then(Commands.literal("query").then(Commands.argument("position", BlockPosArgument.blockPos()).executes(context -> {
                    var position = BlockPosArgument.getLoadedBlockPos(context, "position");
                    var state = context.getSource().getLevel().getBlockState(position);
                    var target = BlockScanTargets.target(state);
                    var message = state + " -> " + target.map(t -> t + "; hash=" + BlockScanTargets.hash(t) + "; aspects=" + BlockScanTargets.aspects(t).view()).orElse("no legacy scan target");
                    context.getSource().sendSuccess(() -> Component.literal(message), false);
                    return target.isPresent() ? 1 : 0;
                })))
                .then(Commands.literal("verify").executes(context -> {
                    int states = 0;
                    var identity = LegacyItemMappings.identity();
                    for (var rule : BlockScanTargets.mapping().rules()) {
                        var target = rule.target();
                        if (target.legacyKey() != null) {
                            if (!RecipeAspects.data().items().containsKey(target.legacyKey().item())) throw new IllegalStateException("Unknown old block identity " + target);
                            if (target.modernItem() != null && !target.legacyKey().equals(identity.recipeKey(target.modernItem()))) throw new IllegalStateException("Picked modern item has a different old identity " + target);
                        }
                        BlockScanTargets.aspects(target);
                        BlockScanTargets.hash(target);
                        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(rule.block()));
                        for (var state : block.getStateDefinition().getPossibleStates()) {
                            if (!rule.matches(BlockScanTargets.properties(state))) continue;
                            if (!BlockScanTargets.target(state).orElseThrow().equals(target)) throw new IllegalStateException("Block state identity mismatch " + state);
                            states++;
                        }
                    }
                    int checked = states;
                    context.getSource().sendSuccess(() -> Component.literal("Block scan checks passed: " + BlockScanTargets.mapping().rules().size()
                            + " rules, " + checked + " actual block states, semantic identities and aspect resolution."), false);
                    return 1;
                }));
    }
}

package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemAspects;
import tc4revival.data.LegacyItemDefinitions;

 class ItemAspectDebugCommands {
    private ItemAspectDebugCommands() {}

    static ArgumentBuilder<CommandSourceStack, ?> tree(CommandBuildContext buildContext) {
        return Commands.literal("item_aspects")
                .then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.argument("item", ItemArgument.item(buildContext)).executes(context -> {
                    var stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
                    var result = ItemAspects.direct(stack);
                    String values = result.map(amounts -> amounts.view().entrySet().stream()
                            .map(entry -> entry.getKey().tag() + "=" + entry.getValue()).toList().toString())
                            .orElse("no direct aspects (recipe/stack layers separate)");
                    context.getSource().sendSuccess(() -> Component.literal(ForgeRegistries.ITEMS.getKey(stack.getItem()) + ": " + values), false);
                    return result.isPresent() ? 1 : 0;
                }));
    }

    private static int verify(CommandSourceStack source) {
        var catalog = TCAspects.catalog();
        int checked = 0;
        for (var rule : LegacyItemDefinitions.rules()) for (String id : rule.items()) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            var actual = ItemAspects.direct(new ItemStack(item));
            if (actual.isEmpty() || !actual.get().view().equals(rule.amounts(catalog).view())) {
                source.sendFailure(Component.literal("Direct item aspect check failed: " + id + "; required tag=" + rule.requiredTag()));
                return 0;
            }
            checked++;
        }
        for (var item : List.of(Items.CHERRY_LOG, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON, Items.NETHERITE_INGOT, Items.BLACK_DYE)) {
            if (ItemAspects.direct(new ItemStack(item)).isPresent()) {
                source.sendFailure(Component.literal("Unexpected modern item aspects: " + ForgeRegistries.ITEMS.getKey(item)));
                return 0;
            }
        }
        if (ForgeRegistries.ITEMS.containsKey(new ResourceLocation("thaumcraft", "iron_nugget"))) {
            source.sendFailure(Component.literal("Duplicate TC4 iron nugget"));
            return 0;
        }
        int total = checked;
        source.sendSuccess(() -> Component.literal("Direct item aspect checks passed: " + ItemAspects.directResolver().ruleCount()
                + " rules; " + total + " actual item/tag mappings; vanilla iron nugget; new-content exclusions."), false);
        return 1;
    }
}

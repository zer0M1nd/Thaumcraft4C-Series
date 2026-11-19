package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.aspect.item.LegacyItemMappings;

 class StackAspectDebugCommands {
    private StackAspectDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree(CommandBuildContext buildContext) {
        return Commands.literal("item_stack")
                .then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.literal("verify_special").executes(context -> verifySpecial(context.getSource())))
                .then(Commands.argument("item", ItemArgument.item(buildContext)).executes(context -> {
                    var stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
                    var result = ItemStackAspects.resolve(stack);
                    context.getSource().sendSuccess(() -> Component.literal(ForgeRegistries.ITEMS.getKey(stack.getItem())
                            + ": stack aspects=" + result.map(StackAspectDebugCommands::values)
                            + "; legacy identity=" + ItemStackAspects.identity(stack) + "; scan hash=" + ItemStackAspects.scanHash(stack)
                            + "; includes equipment/enchantments/culling; remaining special-stack providers separate"), false);
                    return result.isPresent() ? 1 : 0;
                }));
    }
    private static int verify(CommandSourceStack source) {
        var expected = Map.of(
                "diamond_pickaxe", Map.of("vitreus", 9, "lucrum", 9, "arbor", 1, "perfodio", 4),
                "diamond_sword", Map.of("vitreus", 6, "lucrum", 6, "telum", 4),
                "chainmail_helmet", Map.of("metallum", 8, "tutamen", 2),
                "golden_hoe", Map.of("metallum", 4, "lucrum", 3, "arbor", 1, "meto", 1),
                "shears", Map.of("metallum", 6, "meto", 3),
                "bow", Map.of("arbor", 2, "bestia", 2, "pannus", 2, "telum", 3, "volatus", 1));
        for (var entry : expected.entrySet()) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", entry.getKey()));
            var result = ItemStackAspects.ordinary(new ItemStack(item));
            if (result.isEmpty() || !values(result.get()).equals(entry.getValue())) {
                source.sendFailure(Component.literal("Stack aspect mismatch: " + entry.getKey() + " actual=" + result.map(StackAspectDebugCommands::values)));
                return 0;
            }
        }
        for (String id : List.of("netherite_sword", "trident", "crossbow")) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", id));
            if (ItemStackAspects.ordinary(new ItemStack(item)).isPresent()) {
                source.sendFailure(Component.literal("Unexpected new equipment adaptation: " + id));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.literal("Ordinary stack checks passed: six legacy equipment examples and three new-equipment exclusions."), false);
        return 1;
    }
    private static int verifySpecial(CommandSourceStack source) {
        int checked = 0;
        for (var entry : LegacyItemMappings.spawnEggs().entrySet()) {
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(entry.getKey())));
            if (!ItemStackAspects.identity(stack).orElseThrow().equals(entry.getValue()) || !ItemStackAspects.resolve(stack).orElseThrow().isEmpty()) throw new IllegalStateException("Spawn egg identity/base mismatch " + entry);
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 7);
            if (!values(ItemStackAspects.resolve(stack).orElseThrow()).equals(Map.of("telum", 7, "praecantatio", 7))) throw new IllegalStateException("Spawn egg enchantment mismatch " + entry);
            if (ItemStackAspects.scanHash(stack).orElseThrow() != LegacyItemMappings.identity().scanHash(entry.getValue())) throw new IllegalStateException("Spawn egg hash mismatch " + entry);
            checked++;
        }
        for (int number : new int[] {-1, 0, 42, 32767, 32768, Integer.MAX_VALUE}) {
            var stack = new ItemStack(net.minecraft.world.item.Items.FILLED_MAP);
            stack.getOrCreateTag().putInt("map", number);
            if (!ItemStackAspects.identity(stack).orElseThrow().equals(new tc4revival.aspect.recipe.RecipeAspectData.Key("minecraft:filled_map", Math.max(0, number)))) throw new IllegalStateException("Map number identity mismatch " + number);
            if (!ItemStackAspects.resolve(stack).orElseThrow().isEmpty()) throw new IllegalStateException("Map unexpectedly has base aspects");
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 7);
            if (!values(ItemStackAspects.resolve(stack).orElseThrow()).equals(Map.of("telum", 7, "praecantatio", 7))) throw new IllegalStateException("Map enchantment mismatch " + number);
        }
        int total = checked;
        source.sendSuccess(() -> Component.literal("Special stack checks passed: " + total + " actual old spawn eggs, six map numbers, enchantment values and scan identities."), false);
        return 1;
    }
    private static Map<String, Integer> values(AspectAmounts amounts) {
        var result = new LinkedHashMap<String, Integer>();
        amounts.view().forEach((aspect, quantity) -> result.put(aspect.tag(), quantity));
        return result;
    }
}

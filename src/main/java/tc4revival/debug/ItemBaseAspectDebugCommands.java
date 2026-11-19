package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.ItemAspects;
import tc4revival.aspect.recipe.RecipeAspects;
import net.minecraftforge.registries.ForgeRegistries;

 class ItemBaseAspectDebugCommands {
    private ItemBaseAspectDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree(CommandBuildContext buildContext) {
        return Commands.literal("item_base")
                .then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.argument("item", ItemArgument.item(buildContext)).executes(context -> {
                    var stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
                    var id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    var session = ItemAspects.baseSession();
                    var raw = session.identity().recipeKey(id);
                    var base = session.base(id);
                    var canonical = session.canonical(id);
                    context.getSource().sendSuccess(() -> Component.literal(id + ": base=" + base.map(ItemBaseAspectDebugCommands::values)
                            + "; canonical=" + canonical.map(ItemBaseAspectDebugCommands::values) + "; legacy=" + raw
                            + (raw == null ? "" : "; scan=" + session.identity().scanKey(raw) + "; hash=" + session.identity().scanHash(raw))
                            + "; plain base only (bonuses/NBT separate)"), false);
                    return base.isPresent() ? 1 : 0;
                }));
    }
    private static int verify(CommandSourceStack source) {
        var session = ItemAspects.baseSession();
        int checked = 0;
        for (var alias : session.identity().items().entrySet()) {
            // Fresh baseline calculation keeps the legacy null/empty cache distinction out of assertion order.
            var expected = RecipeAspects.newEngine().object(session.identity().scanKey(alias.getValue()));
            var actual = ItemAspects.baseSession().base(alias.getKey()).orElse(null);
            if (!values(expected).equals(values(actual))) {
                source.sendFailure(Component.literal("Item base mismatch: " + alias + " expected=" + values(expected) + " actual=" + values(actual)));
                return 0;
            }
            checked++;
        }
        var identity = session.identity();
        int hash = identity.scanHash(identity.recipeKey("minecraft:anvil"));
        for (String id : List.of("minecraft:chipped_anvil", "minecraft:damaged_anvil")) if (identity.scanHash(identity.recipeKey(id)) != hash) {
            source.sendFailure(Component.literal("Anvil scan group mismatch"));
            return 0;
        }
        for (String id : List.of("minecraft:dragon_head", "minecraft:cherry_log", "minecraft:black_dye", "minecraft:granite")) if (session.base(id).isPresent()) {
            source.sendFailure(Component.literal("Unexpected new-content base aspects: " + id));
            return 0;
        }
        int total = checked;
        source.sendSuccess(() -> Component.literal("Item base checks passed: " + total + " actual modern aliases; legacy anvil group/hash; new-content exclusions."), false);
        return 1;
    }
    private static Map<String, Integer> values(AspectAmounts amounts) {
        var result = new LinkedHashMap<String, Integer>();
        if (amounts != null) amounts.view().forEach((aspect, quantity) -> result.put(aspect.tag(), quantity));
        return result;
    }
}

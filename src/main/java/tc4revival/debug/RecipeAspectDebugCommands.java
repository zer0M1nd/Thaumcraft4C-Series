package tc4revival.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspects;

 class RecipeAspectDebugCommands {
    private RecipeAspectDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("recipe_aspects")
                .then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.literal("query").then(Commands.argument("legacy_item", StringArgumentType.string())
                        .then(Commands.argument("metadata", IntegerArgumentType.integer(0, Short.MAX_VALUE))
                                .executes(context -> query(context, false))
                                .then(Commands.argument("canonical", BoolArgumentType.bool()).executes(context -> query(context, BoolArgumentType.getBool(context, "canonical")))))));
    }
    private static int query(CommandContext<CommandSourceStack> context, boolean canonical) {
        var key = new Key(StringArgumentType.getString(context, "legacy_item"), IntegerArgumentType.getInteger(context, "metadata"));
        if (!RecipeAspects.data().items().containsKey(key.item())) {
            context.getSource().sendFailure(Component.literal("Unknown semantic item key: " + key.item()));
            return 0;
        }
        var engine = RecipeAspects.newEngine();
        var result = canonical ? engine.generate(key) : engine.object(key);
        String values = result == null ? "no derived list" : result.view().entrySet().stream().map(entry -> entry.getKey().tag() + "=" + entry.getValue()).toList().toString();
        context.getSource().sendSuccess(() -> Component.literal(key + (canonical ? " canonical=" : " object=") + values), false);
        return result == null ? 0 : 1;
    }
    private static int verify(CommandSourceStack source) {
        var expected = Map.of("minecraft:book", Map.of("cognitio", 3, "pannus", 1), "minecraft:paper", Map.of("cognitio", 1),
                "minecraft:iron_door", Map.of("machina", 2, "motus", 1, "metallum", 18),
                "minecraft:diamond_pickaxe", Map.of("vitreus", 9, "lucrum", 9, "arbor", 1));
        var catalog = TCAspects.catalog();
        for (var entry : expected.entrySet()) {
            var result = RecipeAspects.newEngine().object(new Key(entry.getKey(), 0));
            if (result == null || result.size() != entry.getValue().size() || entry.getValue().entrySet().stream()
                    .anyMatch(quantity -> result.amount(catalog.require(quantity.getKey())) != quantity.getValue())) {
                source.sendFailure(Component.literal("Recipe aspect check failed: " + entry.getKey()));
                return 0;
            }
        }
        var canonical = RecipeAspects.newEngine().generate(new Key("minecraft:paper", 0));
        if (canonical == null || !canonical.isEmpty()) {
            source.sendFailure(Component.literal("Legacy paper canonical empty list was not preserved"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Recipe aspect checks passed: legacy costs; exact/wildcard distinction; live registered aspect catalog; "
                + RecipeAspects.data().recipes().size() + " calculation recipes (gameplay recipe registration is separate)."), false);
        return 1;
    }
}

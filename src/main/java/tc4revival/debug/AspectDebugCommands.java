package tc4revival.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

 class AspectDebugCommands {
    private AspectDebugCommands() {}

    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("aspects")
                .then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.literal("list").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(String.join(", ",
                            TCAspects.catalog().all().stream().map(Aspect::tag).toList())), false);
                    return TCAspects.REGISTRY.get().getValues().size();
                }))
                .then(Commands.literal("combine").then(aspectArgument("first").then(aspectArgument("second")
                        .executes(context -> {
                            var catalog = TCAspects.catalog();
                            var first = catalog.find(StringArgumentType.getString(context, "first"));
                            var second = catalog.find(StringArgumentType.getString(context, "second"));
                            if (first.isEmpty() || second.isEmpty()) return fail(context.getSource(), "Unknown aspect");
                            var result = catalog.combine(first.get(), second.get());
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Combination: " + result.map(Aspect::tag).orElse("none")), false);
                            return result.isPresent() ? 1 : 0;
                        }))))
                .then(Commands.literal("primals").then(aspectArgument("aspect")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10000))
                                .executes(context -> decompose(context.getSource(), StringArgumentType.getString(context, "aspect"),
                                        IntegerArgumentType.getInteger(context, "amount"), AspectCatalog.Reduction.SUM))
                                .then(Commands.literal("maximum").executes(context -> decompose(context.getSource(),
                                        StringArgumentType.getString(context, "aspect"), IntegerArgumentType.getInteger(context, "amount"),
                                        AspectCatalog.Reduction.MAXIMUM))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> aspectArgument(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests((context, builder) ->
                SharedSuggestionProvider.suggest(TCAspects.catalog().all().stream().map(Aspect::tag), builder));
    }

    private static int decompose(CommandSourceStack source, String tag, int quantity, AspectCatalog.Reduction reduction) {
        var catalog = TCAspects.catalog();
        var aspect = catalog.find(tag);
        if (aspect.isEmpty()) return fail(source, "Unknown aspect: " + tag);
        var result = catalog.toPrimals(new AspectAmounts().add(aspect.get(), quantity), reduction);
        source.sendSuccess(() -> Component.literal("Primals (" + reduction + "): " + result.view().entrySet().stream()
                .map(entry -> entry.getKey().tag() + "=" + entry.getValue()).toList() + "; total=" + result.total()), false);
        return Math.toIntExact(result.total());
    }

    private static int verify(CommandSourceStack source) {
        var catalog = TCAspects.catalog();
        if (TCAspects.REGISTRY.get().getValues().size() != 48 || catalog.primals().size() != 6
                || catalog.compounds().size() != 42) return fail(source, "Incorrect registered aspect counts");
        for (Aspect aspect : catalog.all()) {
            var key = TCAspects.REGISTRY.get().getKey(aspect);
            if (key == null || !key.toString().equals("thaumcraft:" + aspect.tag())) return fail(source, "Incorrect aspect registry key");
            if (!aspect.isPrimal()) {
                var first = catalog.require(aspect.components().get(0));
                var second = catalog.require(aspect.components().get(1));
                if (!catalog.combine(first, second).orElseThrow().equals(aspect)
                        || !catalog.combine(second, first).orElseThrow().equals(aspect)) return fail(source, "Combination mismatch");
            }
        }
        var original = new AspectAmounts().add(catalog.require("metallum"), 4).add(catalog.require("aer"), 0);
        var stored = new CompoundTag();
        AspectNbt.write(stored, original);
        var unknown = new CompoundTag();
        unknown.putString("key", "unknown_test_aspect");
        unknown.putInt("amount", 9);
        stored.getList(AspectNbt.DEFAULT_LABEL, Tag.TAG_COMPOUND).add(unknown);
        var loaded = AspectNbt.read(stored, catalog);
        if (!loaded.amounts().view().equals(original.view()) || loaded.problems().size() != 1) {
            return fail(source, "NBT round trip or unknown-entry handling failed");
        }
        var primal = catalog.toPrimals(loaded.amounts(), AspectCatalog.Reduction.SUM);
        if (primal.total() != 12 || primal.amount(catalog.require("terra")) != 8
                || primal.amount(catalog.require("ordo")) != 4 || !primal.contains(catalog.require("aer"))) {
            return fail(source, "Metal decomposition or zero-entry retention failed");
        }
        source.sendSuccess(() -> Component.literal("Aspect checks passed: 48 registered, 6 primal, 42 compound; "
                + "84 ordered combinations; NBT round trip, unknown-entry recovery, zero retention and metal decomposition."), false);
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}

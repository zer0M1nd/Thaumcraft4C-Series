package tc4revival.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchRules;

/** Read-only inspections and in-memory rule checks. Never grants knowledge to real players. */
public  class ResearchDebugCommands {
    private ResearchDebugCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("research")
                .then(Commands.literal("book_actions").executes(context -> ResearchBookDebugCommands.verify(context.getSource())))
                .then(Commands.literal("book_pages").executes(context -> ResearchBookDebugCommands.pages(context.getSource())))
                .then(Commands.literal("book_session").executes(context -> ResearchBookDebugCommands.session(context.getSource())))
                .then(Commands.literal("book_transactions").executes(context -> ResearchBookDebugCommands.transactions(context.getSource())))
                .then(Commands.literal("book_lifecycle").executes(context -> ResearchBookDebugCommands.lifecycle(context.getSource())))
                .then(Commands.literal("book_browser").executes(context -> ResearchBookDebugCommands.browser(context.getSource())))
                .then(Commands.literal("book_nbt").executes(context -> ResearchBookDebugCommands.nbt(context.getSource())))
                .then(Commands.literal("book_items").executes(context -> ResearchBookDebugCommands.items(context.getSource())))
                .then(Commands.literal("materials").executes(context -> ResearchBookDebugCommands.materials(context.getSource())))
                .then(Commands.literal("nuggets").executes(context -> NuggetDebugChecks.run(context.getSource())))
                .then(Commands.literal("smelting").executes(context -> SmeltingDebugChecks.run(context.getSource())))
                .then(Commands.literal("brain").executes(context -> BrainDebugChecks.run(context.getSource())))
                .then(Commands.literal("baubles").executes(context -> BaubleIntegrationChecks.run(context.getSource())))
                .then(Commands.literal("tools").executes(context -> ToolInteractionChecks.run(context.getSource())))
                .then(ResearchClueDebugCommands.tree())
                .then(ResearchNoteDebugCommands.tree())
                .then(ResearchTableDebugCommands.tree())
                .then(Commands.literal("progression").executes(context -> progression(context.getSource()))
                        .then(Commands.literal("inspect").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            var state = KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).orElseThrow();
                            context.getSource().sendSuccess(() -> Component.literal("Research progression: missingAutomatic="
                                    + ResearchCatalogs.progression().missingAutomatic(state) + "; markers=" + state.researchMarkers()
                                    + "; permanent=" + state.warp(PlayerKnowledge.WarpKind.PERMANENT)
                                    + "; sticky=" + state.warp(PlayerKnowledge.WarpKind.STICKY)
                                    + "; temporary=" + state.warp(PlayerKnowledge.WarpKind.TEMPORARY)
                                    + "; counter=" + state.warpCounter()), false);
                            return 1;
                        }))))
                .then(Commands.literal("check").executes(context -> check(context.getSource())))
                .then(Commands.literal("inspect").then(Commands.argument("key", StringArgumentType.word()).executes(context -> {
                    var entry = ResearchCatalogs.catalog().find(StringArgumentType.getString(context, "key"));
                    if (entry.isEmpty()) { context.getSource().sendFailure(Component.literal("Unknown research")); return 0; }
                    context.getSource().sendSuccess(() -> Component.literal(entry.get().toString()), false);
                    return 1;
                })))
                .then(Commands.literal("entity_clues").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("legacy_name", StringArgumentType.word()).executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            var knowledge = KnowledgeSavedData.get(context.getSource().getServer()).snapshot(player.getUUID()).orElseThrow();
                            var clues = ResearchCatalogs.rules().clueCandidates(knowledge,
                                    new ResearchRules.EntityClue(StringArgumentType.getString(context, "legacy_name")), java.util.Map.of());
                            context.getSource().sendSuccess(() -> Component.literal("Entity-only clue candidates: " + clues), false);
                            return clues.size();
                        }))));
    }
    private static int progression(CommandSourceStack source) {
        var progression = ResearchCatalogs.progression();
        var state = new PlayerKnowledge(TCAspects.catalog());
        boolean pass = progression.unlockAutomatic(state, false).size() == 22;
        pass &= progression.unlockAutomatic(state, false).isEmpty() && state.totalWarp() == 0;
        state.rememberResearch("HUNGRYCHEST");
        pass &= progression.completeEligible(state, "GOLEMSTRAW", false).stream()
                .map(tc4revival.research.ResearchProgression.Completion::key).toList()
                .equals(java.util.List.of("GOLEMSTRAW", "COREGATHER", "GOLEMBELL"));
        var warp = progression.completeTrusted(state, "CRIMSON", false).orElseThrow();
        pass &= warp.permanentWarp() == 2 && warp.stickyWarp() == 1 && state.warpCounter() == 3;
        pass &= progression.completeTrusted(state, "CRIMSON", false).isEmpty() && state.totalWarp() == 3;
        boolean wuss = tc4revival.config.TCConfig.SERVER.disableWarp.get();
        var configured = new PlayerKnowledge(TCAspects.catalog());
        progression.completeTrusted(configured, "FOCUSHELLBAT", wuss);
        pass &= configured.totalWarp() == (wuss ? 0 : 2);
        final boolean result = pass;
        source.sendSuccess(() -> Component.literal("Research progression " + (result ? "PASS" : "FAIL")
                + "; automatic=22; direct siblings; odd warp split; duplicate suppression; wuss=" + wuss
                + "; real player knowledge unchanged"), false);
        return result ? 1 : 0;
    }
    private static int check(CommandSourceStack source) {
        var catalog = ResearchCatalogs.catalog();
        var rules = ResearchCatalogs.rules();
        var player = new PlayerKnowledge(TCAspects.catalog());
        var enderman = new ResearchRules.EntityClue("Enderman");
        boolean mirrorEnabled = catalog.find("MIRROR").filter(ResearchCatalogs::enabled).isPresent();
        boolean pass = catalog.categories().size() == 6 && catalog.all().size() == 191
                && rules.clueCandidates(player, enderman, java.util.Map.of()).equals(mirrorEnabled ? java.util.List.of("MIRROR") : java.util.List.of())
                && rules.clueCandidates(player, null, java.util.Map.of("metallum", 10)).isEmpty()
                && rules.clueCandidates(player, new ResearchRules.EntityClue("Cow"), java.util.Map.of("metallum", 1)).equals(java.util.List.of("THAUMIUM"));
        rules.createClue(player, enderman, java.util.Map.of(), bound -> 0);
        pass &= rules.clueCandidates(player, enderman, java.util.Map.of()).isEmpty();
        for (String key : java.util.List.of("MIRROR", "MIRRORHAND", "MIRRORESSENTIA")) {
            player.rememberResearch(key);
            pass &= rules.completed(player, key) == mirrorEnabled;
        }
        boolean wardedEnabled = catalog.find("FOCUSWARDING").filter(ResearchCatalogs::enabled).isPresent();
        for (String key : java.util.List.of("FOCUSWARDING", "WARDEDARCANA")) {
            player.rememberResearch(key);
            pass &= rules.completed(player, key) == wardedEnabled;
        }
        final boolean result = pass;
        source.sendSuccess(() -> Component.literal("Research catalog/rules " + (result ? "PASS" : "FAIL")
                + "; categories=" + catalog.categories().size() + "; research=" + catalog.all().size()
                + "; mirrors=" + mirrorEnabled + "; warded=" + wardedEnabled + "; real player knowledge unchanged"), false);
        return pass ? 1 : 0;
    }
}

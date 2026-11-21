package tc4revival.research;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.network.TCNetwork;
import tc4revival.network.WarpGainPacket;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Public administration commands use native typed arguments, selectors and completion. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID)
public  class ThaumcraftCommands {
    private ThaumcraftCommands() {}

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root=Commands.literal("thaumcraft").requires(source->source.hasPermission(2)).executes(context->help(context.getSource()));
        root.then(Commands.literal("help").executes(context->help(context.getSource())));
        var research=Commands.literal("research").then(Commands.literal("list").executes(context->execute(context.getSource(),"research list")));
        research.then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("research",StringArgumentType.greedyString())
                .suggests((context,builder)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all","reset"),ResearchCatalogs.catalog().all().stream().filter(ResearchCatalogs::enabled).map(ResearchDefinition::key)),builder))
                .executes(context->forTargets(context,"research",StringArgumentType.getString(context,"research")))));
        root.then(research);
        root.then(Commands.literal("aspect").then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("aspect",StringArgumentType.word())
                .suggests((context,builder)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),TCAspects.catalog().all().stream().map(tc4revival.aspect.Aspect::tag)),builder))
                .then(Commands.argument("amount",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,Integer.MAX_VALUE))
                        .executes(context->forTargets(context,"aspect",StringArgumentType.getString(context,"aspect"),Integer.toString(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"amount"))))))));
        var warp=Commands.literal("warp");
        var targets=Commands.argument("targets",EntityArgument.players());
        for(String operation:List.of("add","set")) {
            var amount=Commands.argument("amount",com.mojang.brigadier.arguments.IntegerArgumentType.integer(operation.equals("add")?-100:0,operation.equals("add")?100:Integer.MAX_VALUE));
            amount.executes(context->warp(context,operation,"normal"));
            for(String kind:List.of("normal","permanent","temporary"))amount.then(Commands.literal(kind).executes(context->warp(context,operation,kind)));
            targets.then(Commands.literal(operation).then(amount));
        }
        root.then(warp.then(targets));var node=event.getDispatcher().register(root);
        for(String alias:List.of("thaum","tc"))event.getDispatcher().register(Commands.literal(alias).requires(source->source.hasPermission(2)).executes(context->help(context.getSource())).redirect(node));
    }
    private static int warp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,String operation,String kind) throws CommandSyntaxException {
        return forTargets(context,"warp",operation,Integer.toString(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"amount")),kind.equals("permanent")?"PERM":kind.equals("temporary")?"TEMP":"");
    }
    private static int forTargets(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,String action,String... arguments) throws CommandSyntaxException {
        int count=0;for(var target:EntityArgument.getPlayers(context,"targets")) {
            String[] args=new String[arguments.length+2];args[0]=action;args[1]=target.getScoreboardName();System.arraycopy(arguments,0,args,2,arguments.length);
            count+=apply(context.getSource(),target,action,args);
        }
        return count;
    }

    private static int help(CommandSourceStack source) {
        for (String line : List.of("Aliases: /thaumcraft, /thaum, /tc",
                "/thaumcraft research <list|player> <all|reset|research>",
                "/thaumcraft aspect <player> <aspect|all> <amount>",
                "/thaumcraft warp <targets> <add|set> <amount> [normal|permanent|temporary]",
                "Player selectors are supported; press Tab for names and research keys.")) {
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.DARK_AQUA), false);
        }
        return 1;
    }

    static int execute(CommandSourceStack source, String text) throws CommandSyntaxException {
        String[] args = text.trim().split("\\s+");
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("help")) return help(source);
        if (action.equals("research") && args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            for (var entry : ResearchCatalogs.catalog().all()) if (ResearchCatalogs.enabled(entry)) {
                source.sendSuccess(() -> Component.literal(entry.key()).withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            return 1;
        }
        if (args.length < 3) throw error("Invalid arguments. Use /thaumcraft help.");
        var target = EntityArgument.player().parse(new StringReader(args[1])).findSinglePlayer(source);
        return apply(source, target, action, args);
    }

    static int apply(CommandSourceStack source, ServerPlayer target, String action, String[] args) throws CommandSyntaxException {
        var data = KnowledgeSavedData.get(source.getServer());
        String message;
        switch (action) {
            case "research" -> {
                if (args.length != 3) throw error("Expected research <player> <all|reset|research>.");
                String key = args[2];
                if (!key.equalsIgnoreCase("all") && !key.equalsIgnoreCase("reset")
                        && ResearchCatalogs.catalog().find(key).filter(ResearchCatalogs::enabled).isEmpty())
                    throw error("Research does not exist.");
                var completions = data.update(target.getUUID(), target.getRandom()::nextInt,
                        knowledge -> research(knowledge, key));
                for (var completion : completions) {
                    if (completion.permanentWarp() > 0) TCNetwork.send(target, new WarpGainPacket(WarpKind.PERMANENT, completion.permanentWarp()));
                    if (completion.stickyWarp() > 0) TCNetwork.send(target, new WarpGainPacket(WarpKind.STICKY, completion.stickyWarp()));
                }
                message = key.equalsIgnoreCase("reset") ? " has reset your research."
                        : key.equalsIgnoreCase("all") ? " has given you all research."
                        : " gave you " + key + " research and its requisites.";
            }
            case "aspect" -> {
                if (args.length != 4) throw error("Expected aspect <player> <aspect|all> <amount>.");
                int amount = number(args[3], 1, Integer.MAX_VALUE);
                var aspects = TCAspects.catalog().all().stream().filter(aspect -> args[2].equalsIgnoreCase("all")
                        || args[2].equalsIgnoreCase(aspect.displayName())).toList();
                if (aspects.isEmpty()) throw error("Aspect does not exist.");
                try {
                    data.update(target.getUUID(), target.getRandom()::nextInt, knowledge -> {
                        for (var aspect : aspects) knowledge.changePool(aspect, amount);
                        return true;
                    });
                } catch (ArithmeticException overflow) { throw error("Aspect pool is too large."); }
                message = " gave you " + amount + " " + args[2] + ".";
            }
            case "warp" -> {
                if (args.length < 4 || !(args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("set")))
                    throw error("Expected warp <player> <add|set> <amount> [PERM|TEMP].");
                boolean add = args[2].equalsIgnoreCase("add");
                int amount = number(args[3], add ? -100 : 0, add ? 100 : Integer.MAX_VALUE);
                String type = args.length == 5 ? args[4] : "";
                WarpKind kind = type.equalsIgnoreCase("PERM") ? WarpKind.PERMANENT
                        : type.equalsIgnoreCase("TEMP") ? WarpKind.TEMPORARY : WarpKind.STICKY;
                data.update(target.getUUID(), target.getRandom()::nextInt, knowledge -> {
                    if (add) knowledge.changeWarp(kind, amount); else knowledge.setWarp(kind, amount);
                    return true;
                });
                if (add && amount != 0) TCNetwork.send(target, new WarpGainPacket(kind, amount));
                message = add ? " added " + amount + " warp to your total." : " set your warp to " + amount;
            }
            default -> throw error("Invalid action. Use /thaumcraft help.");
        }
        KnowledgeSync.flushBeforeScreen(target);
        target.sendSystemMessage(Component.literal(source.getTextName() + message).withStyle(ChatFormatting.DARK_PURPLE));
        source.sendSuccess(() -> Component.literal("Success!").withStyle(ChatFormatting.DARK_PURPLE), false);
        return 1;
    }

    static List<ResearchProgression.Completion> research(PlayerKnowledge knowledge, String key) {
        var progression = ResearchCatalogs.progression();
        boolean disableWarp = TCConfig.SERVER.disableWarp.get();
        if (key.equalsIgnoreCase("reset")) {
            for (String marker : List.copyOf(knowledge.researchMarkers())) knowledge.forgetResearch(marker);
            return progression.unlockAutomatic(knowledge, disableWarp);
        }
        var result = new ArrayList<ResearchProgression.Completion>();
        if (key.equalsIgnoreCase("all")) {
            for (var entry : ResearchCatalogs.catalog().all()) if (ResearchCatalogs.enabled(entry))
                progression.completeTrusted(knowledge, entry.key(), disableWarp).ifPresent(result::add);
        } else {
            // Iterative depth-first traversal retains old completion order without stack-overflow risk.
            var pending = new ArrayDeque<String>();
            pending.push(key);
            while (!pending.isEmpty()) {
                String next = pending.pop();
                if (knowledge.researchMarkers().contains(next)) continue;
                progression.completeTrusted(knowledge, next, disableWarp).ifPresent(result::add);
                ResearchCatalogs.catalog().find(next).filter(ResearchCatalogs::enabled).ifPresent(entry -> {
                    var links = new ArrayList<>(entry.parents());
                    links.addAll(entry.hiddenParents());
                    links.addAll(entry.siblings());
                    for (int i = links.size() - 1; i >= 0; i--) pending.push(links.get(i));
                });
            }
        }
        return List.copyOf(result);
    }

    private static int number(String value, int min, int max) throws CommandSyntaxException {
        try {
            int result = Integer.parseInt(value);
            if (result >= min && result <= max) return result;
        } catch (NumberFormatException ignored) { }
        throw error("Number must be between " + min + " and " + max + ".");
    }
    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}

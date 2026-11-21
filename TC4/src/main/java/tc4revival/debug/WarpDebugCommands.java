package tc4revival.debug;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import tc4revival.content.WarpEventRules;
import tc4revival.content.WarpEvents;

public class WarpDebugCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        var root=Commands.literal("warp_effect");
        for(var effect:WarpEventRules.Event.values()) {
            if(effect==WarpEventRules.Event.NONE)continue;
            root.then(Commands.literal(effect.name().toLowerCase(java.util.Locale.ROOT))
                .executes(c->{WarpEvents.force(c.getSource().getPlayerOrException(),effect,60);return 1;})
                .then(Commands.argument("players",EntityArgument.players())
                    .executes(c->{var players=EntityArgument.getPlayers(c,"players");for(var p:players)WarpEvents.force(p,effect,60);return players.size();})
                    .then(Commands.argument("strength",IntegerArgumentType.integer(10,100))
                        .executes(c->{var players=EntityArgument.getPlayers(c,"players");for(var p:players)WarpEvents.force(p,effect,IntegerArgumentType.getInteger(c,"strength"));return players.size();}))));
        }
        return root;
    }
}

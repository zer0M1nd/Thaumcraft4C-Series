package conciliummodern;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator-only recovery of flesh loss; healing remains a separate operation as in the original command. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ConciliumCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("concilium").requires(source->source.hasPermission(2))
            .executes(context->help(context.getSource()))
            .then(Commands.literal("help").executes(context->help(context.getSource())))
            .then(Commands.literal("repairWitchery").requires(source->ConciliumSpellIntegration.witcheryAvailable()).then(Commands.argument("player",EntityArgument.player()).executes(context->{
                var player=EntityArgument.getPlayer(context,"player");if(!ConciliumSpellIntegration.repairWitchery(player)){context.getSource().sendFailure(Component.translatable("thaumicconcilium.command.witchery_failed"));return 0;}
                context.getSource().sendSuccess(()->Component.translatable("thaumicconcilium.command.witchery_repaired",player.getDisplayName()),true);return 1;
            })))
            .then(Commands.literal("restore").then(Commands.argument("player",EntityArgument.player()).executes(context->{
                var player=EntityArgument.getPlayer(context,"player");EtherealState.restoreAllFlesh(player);
                player.sendSystemMessage(Component.translatable("thaumicconcilium.command.restored"));
                if(context.getSource().getEntity()!=player)context.getSource().sendSuccess(()->Component.translatable("thaumicconcilium.command.restored_player",player.getDisplayName()),true);
                return 1;
            }))));
    }
    private static int help(net.minecraft.commands.CommandSourceStack source){source.sendSuccess(()->Component.translatable("thaumicconcilium.command.restore_help"),false);if(ConciliumSpellIntegration.witcheryAvailable())source.sendSuccess(()->Component.literal("/concilium repairWitchery <player>"),false);return 1;}
    private ConciliumCommands(){}
}

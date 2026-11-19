package temodern;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import temodern.essentia.*;

@Mod.EventBusSubscriber(modid = TE.ID)
public final class Commands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("thaumicenergistics")
                .requires(source -> source.hasPermission(2))
                .then(net.minecraft.commands.Commands.literal("escrow").executes(context -> {
                    var data = BusEscrow.get(context.getSource().getLevel());
                    int count = 0;
                    for (var entry : data.entries().entrySet()) for (var resource : entry.getValue().snapshot().entrySet()) {
                        context.getSource().sendSuccess(() -> Component.literal(entry.getKey() + " " + resource.getKey() + " = " + resource.getValue()), false); count++;
                    }
                    return count;
                }).then(net.minecraft.commands.Commands.literal("recover").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var data = BusEscrow.get(player.serverLevel());
                    int recovered = 0;
                    for (var ledger : data.entries().values()) for (String tag : ledger.snapshot().keySet()) {
                        var key = EssentiaKey.parse(tag);
                        if (key == null) continue; // Retain unknown aspects until their data pack is restored.
                        ledger.deliver(tag, amount -> { player.getInventory().placeItemBackInInventory(EssentiaAmpoule.filled(key, amount)); return amount; });
                        data.setDirty(); recovered++;
                    }
                    return recovered;
                }))));
    }
}

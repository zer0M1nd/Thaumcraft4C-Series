package automagy.content;

import automagy.Automagy;
import automagy.logic.ScanReset;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.research.*;

@Mod.EventBusSubscriber(modid=Automagy.ID)
public final class AutomagyCommands {
 @SubscribeEvent public static void register(RegisterCommandsEvent event){
  var root=event.getDispatcher().register(Commands.literal("automagy").requires(s->s.hasPermission(2))
   .then(Commands.literal("scanneditem").then(Commands.argument("player",EntityArgument.player()).then(Commands.literal("reset")
    .then(Commands.literal("held").executes(c->reset(c.getSource(),EntityArgument.getPlayer(c,"player"),false)))
    .then(Commands.literal("ALL").executes(c->reset(c.getSource(),EntityArgument.getPlayer(c,"player"),true)))))));
  event.getDispatcher().register(Commands.literal("tca").requires(s->s.hasPermission(2)).redirect(root));
 }
 private static int reset(net.minecraft.commands.CommandSourceStack source,ServerPlayer player,boolean all) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
  String identity=null;
  if(!all){
   if(player.getMainHandItem().isEmpty())throw error("command.automagy.scan.empty");
   identity=ScanIdentity.item(player.getMainHandItem());
  }
  var data=KnowledgeSavedData.get(player.server);var snapshot=data.snapshot(player.getUUID());
  if(snapshot.isEmpty())throw error("command.automagy.scan.missing");var old=snapshot.get();
  var next=ScanReset.objects(old,tc4revival.aspect.TCAspects.catalog(),identity);
  int removed=old.scannedKeys(PlayerKnowledge.ScanKind.OBJECT).size()-next.scannedKeys(PlayerKnowledge.ScanKind.OBJECT).size();
  if(!all&&removed==0)throw error("command.automagy.scan.missing");
  if(!data.replace(player.getUUID(),data.revision(player.getUUID()),next))throw error("command.automagy.scan.conflict");
  KnowledgeSync.flushBeforeScreen(player);
  var message=Component.translatable("command.automagy.scan.done",player.getDisplayName(),removed);
  source.sendSuccess(()->message,true);if(source.getEntity()!=player)player.sendSystemMessage(message);return removed;
 }
 private static com.mojang.brigadier.exceptions.CommandSyntaxException error(String key){return new SimpleCommandExceptionType(Component.translatable(key)).create();}
 private AutomagyCommands(){}
}

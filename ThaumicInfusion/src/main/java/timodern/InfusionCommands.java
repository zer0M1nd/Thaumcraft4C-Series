package timodern;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
public class InfusionCommands {
    @SubscribeEvent public void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("checkinstability").requires(s->s.hasPermission(2)).executes(c->{var pos=BlockPos.containing(c.getSource().getPosition());c.getSource().sendSuccess(()->Component.translatable("ti.command.instability",pos.getX()>>4,pos.getZ()>>4),false);return 0;}));
        e.getDispatcher().register(Commands.literal("cleandata").requires(s->s.hasPermission(2)).executes(c->{c.getSource().sendSuccess(()->Component.translatable("ti.command.clean.confirm"),false);return 0;}).then(Commands.literal("confirm").executes(c->{var l=c.getSource().getLevel();var d=InfusionData.get(l);int count=d.entries.size();for(long p:java.util.List.copyOf(d.entries.keySet()))InfusionData.remove(l,BlockPos.of(p));c.getSource().sendSuccess(()->Component.translatable("ti.command.clean.done",count,l.dimension().location().toString()),true);return count;})));
    }
}

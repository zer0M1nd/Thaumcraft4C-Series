package emtmodern.item;

import emtmodern.energy.EmtEnergyConfig;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original unrestricted /emt_outputs command, using the modern written-book format. */
@Mod.EventBusSubscriber(modid="emt")
public class EssentiaOutputsCommand {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("emt_outputs").executes(context->give(context.getSource().getPlayerOrException())));}
    public static ItemStack book(){
        var stack=new ItemStack(Items.WRITTEN_BOOK);var tag=stack.getOrCreateTag();tag.putString("author","Tombenpotter");tag.putString("title","Essentia EU Outputs");tag.putBoolean("resolved",true);var pages=new ListTag();
        var lines=new java.util.ArrayList<String>();for(var aspect:tc4revival.aspect.TCAspects.catalog().all())lines.add(aspect.tag()+": "+EmtEnergyConfig.essentiaOutput(aspect));
        // Unlike the author's fixed substring offsets, this remains valid with fewer or added aspects.
        for(int start=0;start<lines.size();start+=8){var page=Component.translatable("gui.EMT.book.aspect.output.essentia.eu").append(":\n").append(String.join("\n",lines.subList(start,Math.min(start+8,lines.size()))));pages.add(StringTag.valueOf(Component.Serializer.toJson(page)));}
        if(pages.isEmpty())pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.empty())));tag.put("pages",pages);return stack;
    }
    public static int give(ServerPlayer player){var book=book();if(!player.getInventory().add(book))player.drop(book,false);return 1;}
}

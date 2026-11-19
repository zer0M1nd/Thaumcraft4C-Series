package temodern.crafting;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Recipe transfer edits ghost slots only. The real recipe is checked when inscribing/encoding. */
public record GhostRecipePacket(int menuId,List<ItemStack> inputs) {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("thaumicenergistics","recipes"),()->"1","1"::equals,"1"::equals);
    public static void register(){CHANNEL.registerMessage(0,GhostRecipePacket.class,(p,b)->{b.writeVarInt(p.menuId);for(var stack:p.inputs)b.writeItem(stack);},b->{int id=b.readVarInt();var list=new java.util.ArrayList<ItemStack>();for(int i=0;i<9;i++)list.add(b.readItem());return new GhostRecipePacket(id,list);},(p,c)->{
        var context=c.get();context.enqueueWork(()->{
            var player=context.getSender();if(player==null||player.containerMenu.containerId!=p.menuId||!player.containerMenu.stillValid(player))return;
            if(player.containerMenu instanceof InscriberMenu menu)menu.setGhostRecipe(p.inputs);
            else if(player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu menu){
                ((ArcaneEncodingMenu)menu).teSetArcane(true);
                for(int i=0;i<9;i++){var stack=p.inputs.get(i);menu.getCraftingMatrix().setItemDirect(i,stack.copyWithCount(stack.isEmpty()?0:1));}
                menu.broadcastChanges();
            }
        });context.setPacketHandled(true);
    },java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));}
}

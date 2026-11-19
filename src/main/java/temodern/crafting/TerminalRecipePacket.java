package temodern.crafting;

import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;

public record TerminalRecipePacket(int menuId,List<Ingredient> inputs) {
    public static void register(){GhostRecipePacket.CHANNEL.registerMessage(1,TerminalRecipePacket.class,
            (p,b)->{b.writeVarInt(p.menuId);for(int i=0;i<9;i++)p.inputs.get(i).toNetwork(b);},
            b->{int id=b.readVarInt();var inputs=new java.util.ArrayList<Ingredient>();for(int i=0;i<9;i++)inputs.add(Ingredient.fromNetwork(b));return new TerminalRecipePacket(id,inputs);},
            (p,c)->{var context=c.get();context.enqueueWork(()->{
                var player=context.getSender();
                if(player!=null&&player.containerMenu.containerId==p.menuId&&player.containerMenu instanceof ArcaneTerminalMenu menu)menu.fillCraftingGrid(p.inputs);
            });context.setPacketHandled(true);},java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));}
}

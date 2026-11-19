package conciliummodern;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Select once, simulate before world mutation, and charge the same source after successful mutation. */
final class GemVisPayment {
    private final ServerPlayer player;private final ItemStack donor;private final Map<Aspect,Integer> costs;
    private GemVisPayment(ServerPlayer player,ItemStack donor,Map<Aspect,Integer> costs){this.player=player;this.donor=donor;this.costs=costs;}
    static GemVisPayment prepare(ServerPlayer player,String aspect){
        var costs=Map.of(TCAspects.catalog().require("aer"),1000,TCAspects.catalog().require(aspect),1000);
        for(var stack:WandInventory.wornBaubles(player))if(stack.getItem() instanceof WandInventory.VisAmulet amulet&&amulet.consumeVis(stack,player,costs,false))return new GemVisPayment(player,stack,costs);
        var inventory=player.getInventory().items;
        for(int i=inventory.size()-1;i>=0;i--){var stack=inventory.get(i);if(stack.getItem() instanceof CastingWandItem wand&&wand.consumeForPlayer(stack,player,costs,false,true,false))return new GemVisPayment(player,stack,costs);}
        return null;
    }
    boolean commit(){
        boolean owned=player.getInventory().items.stream().anyMatch(stack->stack==donor);
        if(!owned)for(var stack:WandInventory.wornBaubles(player))if(stack==donor){owned=true;break;}
        if(!owned)return false;
        if(donor.getItem() instanceof WandInventory.VisAmulet amulet)return amulet.consumeVis(donor,player,costs,true);
        return donor.getItem() instanceof CastingWandItem wand&&wand.consumeForPlayer(donor,player,costs,false,true,true);
    }
}

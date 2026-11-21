package conciliummodern;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.*;
import tc4revival.content.*;
import tc4revival.research.*;
import top.theillusivec4.curios.api.CuriosApi;

/** Shared modern focus contracts; costs are centivis, upgrade identities are stable keys. */
abstract class ConciliumFocus extends WandFocusItem {
    protected static Map<Aspect,Integer> vis(Object... values) {
        var result=new LinkedHashMap<Aspect,Integer>();
        for(int i=0;i<values.length;i+=2)result.put(TCAspects.catalog().require((String)values[i]),(Integer)values[i+1]);
        return result;
    }
    protected boolean has(ItemStack focus,String key){return upgradeLevel(focus,FocusUpgrade.require("thaumicconcilium:"+key).id)>0;}
    protected List<FocusUpgrade> ranks(int rank,List<FocusUpgrade> ordinary,String... special) {
        if(rank<1 || rank>5)return List.of();
        var result=new ArrayList<>(ordinary);
        if(rank==5)for(var key:special)result.add(FocusUpgrade.require("thaumicconcilium:"+key));
        return result;
    }
    static boolean ring(Player player){return CuriosApi.getCuriosInventory(player).resolve()
            .flatMap(inventory->inventory.findFirstCurio(ConciliumItems.BLUSTERING_RING.get())).isPresent();}
    static boolean requireRing(Player player){
        if(ring(player))return true;
        if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("TC.no_ring").withStyle(ChatFormatting.DARK_PURPLE),true);
        return false;
    }
    static int warp(ServerPlayer player,PlayerKnowledge.WarpKind kind){return KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.warp(kind)).orElse(0);}
    static void changeWarp(ServerPlayer player,PlayerKnowledge.WarpKind kind,int amount){
        KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,k->{k.changeWarp(kind,amount);return null;});
    }
}

package ttmodern;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class RecallFocus extends KamiFocus {
    public RecallFocus(){super("focus_recall","TTKCR",0x9cf8ff);}
    @Override public String depthTexture(){return Tinkerer.ID+":item/focus_recall_depth";}
    @Override public java.util.Map<Aspect,Integer> costs(ItemStack focus){return java.util.Map.of(TCAspects.catalog().require("aer"),4000,TCAspects.catalog().require("terra"),4000,TCAspects.catalog().require("ordo"),4000);}
    static ItemStack pearl(Player player){for(int i=0;i<9;i++){var stack=player.getInventory().getItem(i);if(stack.is(Tinkerer.SKY_PEARL.get())&&SkyPearlItem.bound(stack))return stack;}return ItemStack.EMPTY;}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player instanceof net.minecraft.server.level.ServerPlayer server)||Integer.MAX_VALUE-remaining<=60)return;
        var pearl=pearl(player);if(WarpGateEntity.enabled()&&!pearl.isEmpty()&&player.level().dimension().location().equals(SkyPearlItem.dimension(pearl))&&stack.getItem() instanceof CastingWandItem wand){var costs=costs(attached(stack));if(wand.consumeForPlayer(stack,player,costs,false,false,false)&&WarpGateEntity.teleportTo(server,SkyPearlItem.position(pearl)))wand.consumeForPlayer(stack,player,costs,false,false,true);}
        player.stopUsingItem();
    }
}

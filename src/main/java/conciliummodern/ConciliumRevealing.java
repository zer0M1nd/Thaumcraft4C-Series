package conciliummodern;

import net.minecraft.world.entity.player.Player;
import tc4revival.content.RevealingGear;
import top.theillusivec4.curios.api.CuriosApi;

/** Legacy Util.hasGoggles checks the IGoggles interface, not its showNodes result. */
final class ConciliumRevealing {
    static boolean hasGoggles(Player player){
        if(player==null)return false;
        if(player.getMainHandItem().getItem() instanceof RevealingGear)return true;
        for(var stack:player.getArmorSlots())if(stack.getItem() instanceof RevealingGear)return true;
        return CuriosApi.getCuriosInventory(player).resolve().map(inventory->inventory.getCurios().values().stream().anyMatch(handler->{var stacks=handler.getStacks();for(int i=0;i<stacks.getSlots();i++)if(stacks.getStackInSlot(i).getItem() instanceof RevealingGear)return true;return false;})).orElse(false);
    }
}

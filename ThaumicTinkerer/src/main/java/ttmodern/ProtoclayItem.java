package ttmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class ProtoclayItem extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem {
    public ProtoclayItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    static boolean select(Player player,BlockState state){
        if(!player.getInventory().contains(new ItemStack(Tinkerer.PROTOCLAY.get())) && top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().flatMap(curios->curios.findFirstCurio(Tinkerer.PROTOCLAY.get())).isEmpty())return false;
        var held=player.getMainHandItem();if(!(held.getItem() instanceof AwakenedMiningTool current))return false;
        String wanted=null;
        // Shovel materials overlap the broad pickaxe range; prefer the specialized tool.
        for(var candidate:java.util.List.<AwakenedMiningTool>of(Tinkerer.AWAKENED_SHOVEL.get(),Tinkerer.AWAKENED_AXE.get(),Tinkerer.AWAKENED_PICK.get()))if(candidate.effective(state)){wanted=candidate.kind();break;}
        if(wanted==null||wanted.equals(current.kind()))return false;
        var inventory=player.getInventory();for(int slot=0;slot<inventory.items.size();slot++){
            var other=inventory.getItem(slot);if(other.getItem() instanceof AwakenedMiningTool tool&&tool.kind().equals(wanted)){
                inventory.setItem(inventory.selected,other);inventory.setItem(slot,held);inventory.setChanged();player.containerMenu.broadcastChanges();return true;
            }
        }
        return false;
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.LeftClickBlock event){
        if(event.getLevel().isClientSide||event.isCanceled()||!TinkererConfig.KAMI.get())return;
        if(select(event.getEntity(),event.getLevel().getBlockState(event.getPos())))event.setCanceled(true);
    }
}

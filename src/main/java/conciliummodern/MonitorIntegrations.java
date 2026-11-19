package conciliummodern;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Optional libraries are isolated behind loaded-mod checks; sampling never consumes resources. */
public final class MonitorIntegrations {
    public static int blood(Player player){return net.minecraftforge.fml.ModList.get().isLoaded("bloodmagic")?Blood.sample(player):-1;}
    public static int mana(Player player){return net.minecraftforge.fml.ModList.get().isLoaded("botania")?Botany.sample(player):-1;}
    private static int bucket(long current,long maximum){return maximum<=0?-1:Math.max(0,Math.min(9,(int)Math.round((double)current/maximum*10)-1));}
    private static final class Blood{
        static int sample(Player player){var network=wayoftime.bloodmagic.util.helper.NetworkHelper.getSoulNetwork(player);
            int capacity=wayoftime.bloodmagic.util.helper.NetworkHelper.getMaximumForTier(network.getOrbTier());
            return capacity<=1?-1:bucket(network.getCurrentEssence(),capacity);
        }
    }
    private static final class Botany{
        static int sample(Player player){
            var stacks=new java.util.ArrayList<ItemStack>();for(int slot=0;slot<player.getInventory().getContainerSize();slot++)stacks.add(player.getInventory().getItem(slot));
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inventory->{var equipped=inventory.getEquippedCurios();for(int slot=0;slot<equipped.getSlots();slot++)stacks.add(equipped.getStackInSlot(slot));});
            long current=0,maximum=0;
            for(var stack:stacks){var item=vazkii.botania.xplat.XplatAbstractions.INSTANCE.findManaItem(stack);if(item!=null && !item.isNoExport()){current+=Math.max(0,item.getMana());maximum+=Math.max(0,item.getMaxMana());}}
            return bucket(current,maximum);
        }
    }
    private MonitorIntegrations(){}
}

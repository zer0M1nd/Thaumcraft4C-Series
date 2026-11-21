package conciliummodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Loaded only after a matching mod check; third-party classes never occur in public signatures. */
final class SyringeIntegrations {
    static int pour(ServerPlayer player,ItemStack syringe,int type,int offered){
        if(type==1&&net.minecraftforge.fml.ModList.get().isLoaded("botania"))return Botany.pour(player,syringe,offered);
        if(type==2&&net.minecraftforge.fml.ModList.get().isLoaded("bloodmagic"))return Blood.pour(player,offered);
        return 0;
    }
    static void drain(ServerPlayer player,ItemStack syringe,int type){
        if(type==1&&net.minecraftforge.fml.ModList.get().isLoaded("botania"))Botany.drain(player,1000000);
        if(type==2&&net.minecraftforge.fml.ModList.get().isLoaded("bloodmagic"))Blood.drain(player);
    }
    private static final class Blood {
        static int pour(ServerPlayer player,int offered){var network=wayoftime.bloodmagic.util.helper.NetworkHelper.getSoulNetwork(player);
            int capacity=wayoftime.bloodmagic.util.helper.NetworkHelper.getMaximumForTier(network.getOrbTier());
            int amount=Math.min(offered,Math.max(0,capacity-network.getCurrentEssence()));
            if(amount<=0)return 0;int before=network.getCurrentEssence();network.add(amount,capacity);return Math.max(0,network.getCurrentEssence()-before);
        }
        static void drain(ServerPlayer player){wayoftime.bloodmagic.util.helper.NetworkHelper.getSoulNetwork(player).syphonAndDamage(player,1000000);}
    }
    private static final class Botany {
        static java.util.List<ItemStack> inventory(ServerPlayer player){
            var result=new java.util.ArrayList<ItemStack>();for(int slot=0;slot<player.getInventory().getContainerSize();slot++)result.add(player.getInventory().getItem(slot));
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inv->{var slots=inv.getEquippedCurios();for(int i=0;i<slots.getSlots();i++)result.add(slots.getStackInSlot(i));});return result;
        }
        static int pour(ServerPlayer player,ItemStack syringe,int offered){int remaining=offered;
            for(var stack:inventory(player)){if(stack==syringe||stack.isEmpty())continue;var mana=vazkii.botania.xplat.XplatAbstractions.INSTANCE.findManaItem(stack);
                if(mana==null||!mana.canReceiveManaFromItem(syringe))continue;
                int before=mana.getMana(),amount=Math.min(remaining,Math.max(0,mana.getMaxMana()-before));if(amount>0){mana.addMana(amount);remaining-=Math.max(0,Math.min(amount,mana.getMana()-before));}
                if(remaining==0)break;
            }player.getInventory().setChanged();return offered-remaining;
        }
        static void drain(ServerPlayer player,int remaining){
            for(var stack:inventory(player)){var mana=vazkii.botania.xplat.XplatAbstractions.INSTANCE.findManaItem(stack);if(mana==null)continue;
                int amount=Math.min(remaining,Math.max(0,mana.getMana()));mana.addMana(-amount);remaining-=amount;if(remaining<=0)break;
            }player.getInventory().setChanged();
        }
    }
    private SyringeIntegrations(){}
}

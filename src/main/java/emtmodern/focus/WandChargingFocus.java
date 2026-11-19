package emtmodern.focus;

import ic2.api.item.ElectricItem;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

/** Four sequential armor payments are intentional; an incomplete cycle can spend EU. */
public class WandChargingFocus extends ChargeFocus {
    @Override public int color(ItemStack focus){return 268432464;}
    @Override public String sortingPrefix(){return "ELECTRICCHARGING";}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level world,Player player,InteractionHand hand,BlockHitResult hit){player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(player.level().isClientSide||!(stack.getItem() instanceof CastingWandItem wand))return;
        // 1.7 armorInventory[1] is leggings, not the chest battery.
        ItemStack armor=player.getItemBySlot(EquipmentSlot.LEGS);if(armor.isEmpty())return;
        int quarter=emtmodern.energy.EmtFocusConfig.WAND_CHARGE_COST.get()/4;
        for(int i=0;i<4;i++)if(!ElectricItem.manager.use(armor,quarter,player))return;
        int amount=(int)(100*Math.pow(1.1,wand.focusPotency(stack,player)));
        for(var aspect:TCAspects.catalog().primals())wand.addVis(stack,aspect,amount,false,true);
    }
}

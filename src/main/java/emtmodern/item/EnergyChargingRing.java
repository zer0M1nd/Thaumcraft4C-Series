package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Author-version EU production, pooled per ring and distributed in inventory order. */
public class EnergyChargingRing extends Item implements ICurioItem {
    protected final boolean armor;
    public EnergyChargingRing(boolean armor){super(new Properties().stacksTo(1));this.armor=armor;}
    @Override public Component getName(ItemStack stack){return super.getName(stack).copy().withStyle(ChatFormatting.DARK_GREEN);}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(context.entity() instanceof Player player&&!player.level().isClientSide)
            chargeSlots(armor?player.getInventory().armor:player.getInventory().items,armor?RingConfig.ARMOR_OUTPUT.get():RingConfig.INVENTORY_OUTPUT.get());
    }
    public static int chargeSlots(Iterable<ItemStack> slots,int budget){
        int remaining=budget;
        for(ItemStack target:slots)if(target.getItem() instanceof IElectricItem){
            // The original deliberately observes each item's transfer limit and supports up to tier 4.
            double sent=ElectricItem.manager.charge(target,remaining,4,false,false);
            remaining=(int)(remaining-sent);
        }
        return remaining;
    }
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.entity() instanceof Player;}
    @Override public boolean canUnequip(SlotContext context,ItemStack stack){return true;}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
}

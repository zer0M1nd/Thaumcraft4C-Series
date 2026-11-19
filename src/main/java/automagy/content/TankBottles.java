package automagy.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/** Original Automagy water-bottle rules, independently of buckets and other fluid containers. */
public final class TankBottles {
    public static boolean interact(ThirstyTankBlockEntity tile,Player player,InteractionHand hand) {
        var held=player.getItemInHand(hand);
        boolean empty=held.is(Items.GLASS_BOTTLE);
        boolean water=held.is(Items.POTION)&&PotionUtils.getPotion(held)==Potions.WATER;
        if(!empty&&!water)return false;
        int configured=automagy.AutomagyConfig.WATER_BOTTLE.get();
        if(configured==-1)return true;
        int amount=amount(configured);
        if(empty) {
            if(!containsWater(tile))return true;
            var drained=tile.tank.drain(amount,FluidAction.SIMULATE);
            if(amount>0&&drained.getAmount()!=amount)return true;
            if(amount>0)tile.tank.drain(amount,FluidAction.EXECUTE);
        } else if(amount>0) {
            var fluid=new FluidStack(Fluids.WATER,amount);
            if(tile.tank.fill(fluid,FluidAction.SIMULATE)!=amount)return true;
            tile.tank.fill(fluid,FluidAction.EXECUTE);
            // The original only exempts creative players when pouring a positive amount.
            if(player.getAbilities().instabuild)return true;
        } else if(!containsWater(tile))return true;
        var output=empty?PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER):new ItemStack(Items.GLASS_BOTTLE);
        exchangeOne(player,hand,output);
        return true;
    }

    public static int amount(int configured) {
        if(configured!=-2)return configured;
        // A mod may supply a real Forge fluid capability. Unregistered bottles mean zero, as in 1.7.10.
        return FluidUtil.getFluidContained(PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER))
                .filter(fluid->fluid.getFluid().isSame(Fluids.WATER))
                .map(FluidStack::getAmount).orElse(0);
    }

    private static boolean containsWater(ThirstyTankBlockEntity tile) {
        return !tile.tank.isEmpty()&&tile.tank.getFluid().getFluid().isSame(Fluids.WATER);
    }

    private static void exchangeOne(Player player,InteractionHand hand,ItemStack output) {
        var held=player.getItemInHand(hand);
        held.shrink(1);
        boolean partial=player.getInventory().items.stream().anyMatch(stack->
                ItemStack.isSameItemSameTags(stack,output)&&stack.getCount()<stack.getMaxStackSize());
        if(held.isEmpty()&&!partial)player.setItemInHand(hand,output);
        else if(!player.getInventory().add(output))player.drop(output,false);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
}

package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IScribeTools;

/** Ink and IC2 charge are the same reservoir, never separate durability counters. */
public class ElectricScribingTools extends Item implements IElectricItem, IScribeTools {
    public ElectricScribingTools(){super(new Properties().stacksTo(1));}
    @Override public String getDescriptionId(){return "item.EMT.scribingtools.electric.name";}
    @Override public boolean canProvideEnergy(ItemStack stack){return false;}
    @Override public double getMaxCharge(ItemStack stack){return 400;}
    @Override public int getTier(ItemStack stack){return 1;}
    @Override public double getTransferLimit(ItemStack stack){return 5;}
    @Override public int getInk(ItemStack stack){return (int)Math.floor(ElectricItem.manager.getCharge(stack));}
    @Override public void setInk(ItemStack stack,int amount){
        double target=Math.max(0,Math.min(400,amount));
        double current=ElectricItem.manager.getCharge(stack);
        if(target<current)ElectricItem.manager.discharge(stack,current-target,Integer.MAX_VALUE,true,false,false);
        else if(target>current)ElectricItem.manager.charge(stack,target-current,Integer.MAX_VALUE,true,false);
    }
    @Override public boolean isBarVisible(ItemStack stack){return getInk(stack)<400;}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13f*getInk(stack)/400);}
    @Override public int getBarColor(ItemStack stack){return 0x66bbff;}
}

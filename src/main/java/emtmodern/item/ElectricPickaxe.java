package emtmodern.item;

import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;

/** Correct PickaxeItem identity is required by the unmodified Thaumcraft arcane bore. */
public class ElectricPickaxe extends PickaxeItem implements IElectricItem {
    protected ElectricToolBehaviour tool;
    public ElectricPickaxe(ToolProfile profile){super(Tiers.DIAMOND,1,-2.8f,new Properties().durability(27));tool=new ElectricToolBehaviour(profile);}
    public ToolProfile profile(){return tool.profile();}
    public int breakCost(){return tool.breakCost();}
    public boolean powered(ItemStack stack){return tool.powered(stack);}
    public boolean effective(BlockState state){return tool.effective(state);}
    @Override public int getMaxDamage(ItemStack stack){return tool.durability();}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return tool.speed(stack,state);}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return effective(state);}
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity user){return tool.mine(stack,level,user);}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){return tool.hit(stack,target,attacker);}
    protected boolean hasCustomRightClick(){return profile().axe();}
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action){return tool.action(stack,action)||(!hasCustomRightClick()&&profile().shovel()&&net.minecraftforge.common.ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(action));}
    @Override public InteractionResult useOn(UseOnContext context){return hasCustomRightClick()?tool.torch(context):profile().shovel()?tool.shovel(context):InteractionResult.PASS;}
    @Override public boolean isRepairable(ItemStack stack){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return false;}
    @Override public boolean isEnchantable(ItemStack stack){return ElectricToolBehaviour.enchanting()&&!stack.isEnchanted();}
    @Override public int getEnchantmentValue(){return ElectricToolBehaviour.enchanting()?4:0;}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return ElectricToolBehaviour.enchanting();}
    @Override public boolean canProvideEnergy(ItemStack stack){return false;}
    @Override public double getMaxCharge(ItemStack stack){return profile().capacity();}
    @Override public int getTier(ItemStack stack){return profile().tier();}
    @Override public double getTransferLimit(ItemStack stack){return profile().transfer();}
    @Override public boolean isBarVisible(ItemStack stack){return ElectricItem.manager.getCharge(stack)<profile().capacity();}
    @Override public int getBarWidth(ItemStack stack){return (int)Math.round(13*ElectricItem.manager.getCharge(stack)/profile().capacity());}
    @Override public int getBarColor(ItemStack stack){return 0x66bbff;}
}

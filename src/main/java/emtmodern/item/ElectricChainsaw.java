package emtmodern.item;

import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.*;

/** Author's ItemAxe chainsaw: no pickaxe identity and no arcane-bore shortcut. */
public class ElectricChainsaw extends AxeItem implements IElectricItem {
    protected ElectricToolBehaviour tool;
    public ElectricChainsaw(ToolProfile profile){super(Tiers.DIAMOND,3,-3f,new Properties().durability(27));tool=new ElectricToolBehaviour(profile);}
    public ToolProfile profile(){return tool.profile();}
    public boolean powered(ItemStack stack){return tool.powered(stack);}
    public boolean effective(BlockState state){return tool.effective(state);}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return tool.speed(stack,state);}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return effective(state);}
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity user){return tool.mine(stack,level,user);}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){return tool.hit(stack,target,attacker);}
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action){return tool.action(stack,action);}
    @Override public InteractionResult useOn(UseOnContext context){return tool.torch(context);}
    public static boolean shearMode(ItemStack stack){return !stack.hasTag()||!stack.getTag().contains("shearsMode")||stack.getTag().getBoolean("shearsMode");}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!level.isClientSide&&ic2.api.util.Keys.instance!=null&&ic2.api.util.Keys.instance.isModeSwitchKeyDown(player)){stack.getOrCreateTag().putBoolean("shearsMode",!shearMode(stack));player.displayClientMessage(net.minecraft.network.chat.Component.translatable(shearMode(stack)?"message.emt.shears_on":"message.emt.shears_off"),true);}return InteractionResultHolder.pass(stack);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){if(entity instanceof LivingEntity&&!stack.getOrCreateTag().contains("shearsMode"))stack.getOrCreateTag().putBoolean("shearsMode",true);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        if(!shearMode(stack)||!(entity instanceof IForgeShearable target)||!target.isShearable(stack,entity.level(),entity.blockPosition()))return InteractionResult.PASS;
        if(!entity.level().isClientSide)for(var drop:target.onSheared(player,stack,entity.level(),entity.blockPosition(),EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,stack))){var spawned=entity.spawnAtLocation(drop,1);if(spawned!=null)spawned.setDeltaMovement(spawned.getDeltaMovement().add((entity.getRandom().nextFloat()-entity.getRandom().nextFloat())*.1,entity.getRandom().nextFloat()*.05,(entity.getRandom().nextFloat()-entity.getRandom().nextFloat())*.1));}
        return InteractionResult.sidedSuccess(entity.level().isClientSide);
    }
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player){
        var level=player.level();if(!(player instanceof ServerPlayer server)||!shearMode(stack)||!(level.getBlockState(pos).getBlock() instanceof IForgeShearable target)||!target.isShearable(stack,level,pos))return false;
        if(!level.mayInteract(player,pos)||player.blockActionRestricted(level,pos,server.gameMode.getGameModeForPlayer())||ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,pos)<0)return true;
        var state=level.getBlockState(pos);var drops=target.onSheared(player,stack,level,pos,EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,stack));
        if(state.onDestroyedByPlayer(level,pos,player,!player.isCreative(),level.getFluidState(pos))){if(!player.isCreative())for(var drop:drops)Block.popResource(level,pos,drop);ElectricItem.manager.use(stack,profile().breakCost(),player);level.levelEvent(2001,pos,Block.getId(state));}return true;
    }
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

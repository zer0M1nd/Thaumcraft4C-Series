package emtmodern.item;

import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StreamChainsaw extends ElectricChainsaw {
    public static final ToolProfile PROFILE=new ToolProfile("stream_chainsaw","chainsaw_stream",900000,3,900,400,500,12,25,false,true,false,true);
    public StreamChainsaw(){super(PROFILE);}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return effective(state);}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    public static boolean shearMode(ItemStack stack){return !stack.hasTag()||!stack.getTag().contains("shearsMode")||stack.getTag().getBoolean("shearsMode");}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide&&ic2.api.util.Keys.instance!=null&&ic2.api.util.Keys.instance.isModeSwitchKeyDown(player)){
            stack.getOrCreateTag().putBoolean("shearsMode",!shearMode(stack));player.displayClientMessage(net.minecraft.network.chat.Component.translatable(shearMode(stack)?"message.emt.shears_on":"message.emt.shears_off"),true);
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        if(!shearMode(stack)||!(entity instanceof net.minecraftforge.common.IForgeShearable target)||!target.isShearable(stack,entity.level(),entity.blockPosition()))return InteractionResult.PASS;
        // The inherited author chainsaw shears entities without an EU debit.
        if(!entity.level().isClientSide)for(ItemStack drop:target.onSheared(player,stack,entity.level(),entity.blockPosition(),net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,stack)))entity.spawnAtLocation(drop,1);
        return InteractionResult.sidedSuccess(entity.level().isClientSide);
    }
    public static boolean log(BlockState state){return state.is(BlockTags.LOGS);}
    /** Same first-match greedy traversal as TC4, including flattened old log families. */
    public static BlockPos furthest(Level level,BlockPos origin){
        BlockPos last=origin;double distance=0;
        while(true){BlockPos next=null;double nextDistance=distance;
            search:for(int x=-2;x<=2;x++)for(int y=2;y>=-2;y--)for(int z=-2;z<=2;z++){
                BlockPos candidate=last.offset(x,y,z);if(Math.abs(candidate.getX()-origin.getX())>24||Math.abs(candidate.getY()-origin.getY())>48||Math.abs(candidate.getZ()-origin.getZ())>24)return last;
                if(level.isOutsideBuildHeight(candidate)||!level.hasChunkAt(candidate))continue;
                var state=level.getBlockState(candidate);double squared=candidate.distSqr(origin);
                if(state.is(BlockTags.LOGS)&&state.getDestroySpeed(level,candidate)>=0&&squared>distance){next=candidate;nextDistance=squared;break search;}
            }
            if(next==null)return last;last=next;distance=nextDistance;
        }
    }
    public static boolean breakFurthest(ServerPlayer player,BlockPos origin){
        var level=player.serverLevel();BlockPos target=furthest(level,origin);boolean worked=tc4revival.content.ElementalHarvest.harvest(player,target,false,0);
        if(worked)for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){
            BlockPos pos=target.offset(x,y,z);int delay=150+level.random.nextInt(150);if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos))continue;var state=level.getBlockState(pos);
            if(!state.isAir()){level.scheduleTick(pos,state.getBlock(),delay);if(state.isRandomlyTicking())tc4revival.content.LampPlantGrowth.schedule(level,pos,state,delay);}
        }
        return worked;
    }
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player){
        var level=player.level();if(!player.isShiftKeyDown()&&log(level.getBlockState(pos))){
            if(player instanceof ServerPlayer server){breakFurthest(server,pos);tc4revival.network.FocusEffectPacket.elementalBlockBubble(level,pos);level.playSound(null,pos,tc4revival.content.TCSounds.BUBBLE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.15f,1);ElectricItem.manager.use(stack,400,player);}
            return true;
        }
        if(player instanceof ServerPlayer server&&shearMode(stack)&&level.getBlockState(pos).getBlock() instanceof net.minecraftforge.common.IForgeShearable target&&target.isShearable(stack,level,pos)){
            var state=level.getBlockState(pos);if(!level.mayInteract(player,pos)||player.blockActionRestricted(level,pos,server.gameMode.getGameModeForPlayer())||net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,pos)<0)return true;
            var drops=target.onSheared(player,stack,level,pos,net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,stack));
            if(state.onDestroyedByPlayer(level,pos,player,!player.isCreative(),level.getFluidState(pos))){if(!player.isCreative())for(var drop:drops)net.minecraft.world.level.block.Block.popResource(level,pos,drop);ElectricItem.manager.use(stack,400,player);level.levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(state));}
            return true; // Do not let normal harvesting duplicate the original onSheared drops.
        }
        return false;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;var stack=context.getItemInHand();var pos=context.getClickedPos();var level=context.getLevel();
        if(!player.isShiftKeyDown()&&log(level.getBlockState(pos))&&ElectricItem.manager.getCharge(stack)>400){
            if(player instanceof ServerPlayer server){if(breakFurthest(server,pos)){level.playSound(null,pos,tc4revival.content.TCSounds.BUBBLE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.15f,1);ElectricItem.manager.use(stack,400,player);}}
            else player.swing(context.getHand());
        }
        return super.useOn(context);
    }
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return isEnchantable(stack);}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
}

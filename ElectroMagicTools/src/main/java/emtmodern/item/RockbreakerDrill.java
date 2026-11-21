package emtmodern.item;

import ic2.api.item.ElectricItem;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public class RockbreakerDrill extends ElectricPickaxe {
    public static final ToolProfile PROFILE=new ToolProfile("rockbreaker_drill","drill_rockbreaker",900000,3,900,350,400,12,25,true,false,true,false);
    protected static final java.util.Set<Block> EXTRA=java.util.Set.of(Blocks.GLASS,Blocks.GLOWSTONE,Blocks.FARMLAND);
    protected final java.util.Map<ItemStack,Direction> faces=new java.util.WeakHashMap<>();
    public RockbreakerDrill(){super(PROFILE);}
    @Override protected boolean hasCustomRightClick(){return true;}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return effective(state);}
    public boolean areaEffective(BlockState state){return effective(state)||EXTRA.contains(state.getBlock());}
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player){
        HitResult hit=player.pick(10,1,true);if(hit instanceof BlockHitResult block)faces.put(stack,block.getDirection());return false;
    }
    public static java.util.List<BlockPos> plane(BlockPos origin,Direction face){
        var positions=new java.util.ArrayList<BlockPos>(9);
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)positions.add(switch(face.getAxis()){case Y->origin.offset(a,0,b);case Z->origin.offset(a,b,0);case X->origin.offset(0,a,b);});return positions;
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos origin,LivingEntity user){
        if(level.isClientSide)return true;
        if(user.isShiftKeyDown()){if(ElectricItem.manager.canUse(stack,breakCost()))ElectricItem.manager.use(stack,breakCost(),user);return true;}
        if(!areaEffective(state)||!(user instanceof ServerPlayer player))return true;
        Direction face=faces.getOrDefault(stack,Direction.DOWN);faces.remove(stack);
        for(BlockPos target:plane(origin,face)){
            var targetState=level.getBlockState(target);if(!areaEffective(targetState))continue;
            if(harvestNeighbour(player,target,stack)&&ElectricItem.manager.canUse(stack,breakCost()))ElectricItem.manager.use(stack,breakCost(),player);
        }
        return true;
    }
    /** Old adjacent harvesting uses ordinary fortune drops, never silk touch. */
    public boolean harvestNeighbour(ServerPlayer player,BlockPos pos,ItemStack drill){
        var level=player.serverLevel();if(!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||player.blockActionRestricted(level,pos,player.gameMode.getGameModeForPlayer()))return false;
        var state=level.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(level,pos)<0||net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)<0)return false;
        ItemStack lootTool=drill.copy();var enchants=net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(lootTool);enchants.remove(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(enchants,lootTool);
        var drops=player.isCreative()?java.util.List.<ItemStack>of():Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,lootTool);
        state.getBlock().playerWillDestroy(level,pos,state,player);if(!state.onDestroyedByPlayer(level,pos,player,!player.isCreative(),level.getFluidState(pos)))return false;
        state.getBlock().destroy(level,pos,state);level.levelEvent(2001,pos,Block.getId(state));
        for(ItemStack drop:drops){var following=tc4revival.content.TCEntities.FOLLOWING_ITEM.get().create(level);if(following!=null){following.setPos(Vec3.atCenterOf(pos));following.setItem(drop);following.follow(player,3);level.addFreshEntity(following);}}
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();if(player==null)return InteractionResult.PASS;if(!player.isShiftKeyDown())return super.useOn(context);
        if(player instanceof ServerPlayer server){
            ElectricItem.manager.use(context.getItemInHand(),1000,player);var level=context.getLevel();var pos=context.getClickedPos();level.playSound(null,pos,tc4revival.content.TCSounds.WAND_FAIL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,.2f+level.random.nextFloat()*.2f);
            tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->server),new tc4revival.network.ElementalProspectingPacket(level.dimension().location(),pos,tc4revival.content.ElementalProspecting.scan(server.serverLevel(),pos)));
        }else player.swing(context.getHand());return InteractionResult.PASS;
    }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity target){
        if(!player.level().isClientSide&&(!(target instanceof Player)||player.getServer()!=null&&player.getServer().isPvpAllowed()))target.setSecondsOnFire(2);return false;
    }
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return isEnchantable(stack);}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
}

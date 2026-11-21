package emtmodern.item;

import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.*;

/** Shared EU/harvesting behavior, independent of the item's real vanilla tool superclass. */
public class ElectricToolBehaviour {
    protected ToolProfile profile;
    public ElectricToolBehaviour(ToolProfile profile){this.profile=profile;}
    public ToolProfile profile(){return profile;}
    public int breakCost(){return profile.pick()&&LegacyToolConfig.TOOLS_IN_BORE.get()?1:profile.breakCost();}
    public int durability(){return profile.pick()&&LegacyToolConfig.TOOLS_IN_BORE.get()?profile.boreDurability():27;}
    public boolean powered(ItemStack stack){return ElectricItem.manager.canUse(stack,breakCost());}
    public boolean effective(BlockState state){return profile.pick()&&state.is(BlockTags.MINEABLE_WITH_PICKAXE)||profile.axe()&&state.is(BlockTags.MINEABLE_WITH_AXE)||profile.shovel()&&state.is(BlockTags.MINEABLE_WITH_SHOVEL)||profile.shears()&&(state.is(BlockTags.LEAVES)||state.is(BlockTags.WOOL)||state.is(net.minecraft.world.level.block.Blocks.COBWEB));}
    public float speed(ItemStack stack,BlockState state){
        if(!powered(stack))return 1;
        // The author's chainsaws use the axe speed; their shears compatibility only affects drops.
        boolean fast=profile.pick()?effective(state):profile.axe()&&state.is(BlockTags.MINEABLE_WITH_AXE);
        return fast?profile.speed():1;
    }
    public boolean mine(ItemStack stack,Level level,LivingEntity user){if(!level.isClientSide)ElectricItem.manager.use(stack,breakCost(),user);return true;}
    public boolean hit(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.level().isClientSide&&profile.hitCost()>0&&ElectricItem.manager.use(stack,profile.hitCost(),attacker))target.hurt(attacker instanceof Player p?attacker.damageSources().playerAttack(p):attacker.damageSources().mobAttack(attacker),profile.hitDamage());
        return profile.hitCost()==0;
    }
    public boolean action(ItemStack stack,ToolAction action){return (profile.pick()&&action==ToolActions.PICKAXE_DIG)||(profile.axe()&&action==ToolActions.AXE_DIG)||(profile.shovel()&&action==ToolActions.SHOVEL_DIG)||(profile.shears()&&ToolActions.DEFAULT_SHEARS_ACTIONS.contains(action));}
    /** Vanilla shovel transformations, paid in EU instead of the electric item's display durability. */
    public InteractionResult shovel(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();var stack=context.getItemInHand();
        if(context.getClickedFace()==net.minecraft.core.Direction.DOWN||!powered(stack))return InteractionResult.PASS;
        var state=level.getBlockState(pos);
        var flattened=state.getToolModifiedState(context,ToolActions.SHOVEL_FLATTEN,false);
        BlockState changed;
        if(flattened!=null&&level.isEmptyBlock(pos.above())){
            level.playSound(player,pos,net.minecraft.sounds.SoundEvents.SHOVEL_FLATTEN,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            changed=flattened;
        }else if(state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock&&state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)){
            if(!level.isClientSide)level.levelEvent(null,1009,pos,0);
            net.minecraft.world.level.block.CampfireBlock.dowse(player,level,pos,state);
            changed=state.setValue(net.minecraft.world.level.block.CampfireBlock.LIT,false);
        }else return InteractionResult.PASS;
        if(!level.isClientSide){
            level.setBlock(pos,changed,11);
            level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos,net.minecraft.world.level.gameevent.GameEvent.Context.of(player,changed));
            ElectricItem.manager.use(stack,breakCost(),player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public InteractionResult torch(UseOnContext context){
        Player player=context.getPlayer();if(player==null||!profile.axe())return InteractionResult.PASS;
        for(ItemStack candidate:player.getInventory().items){
            if(!(candidate.getItem() instanceof BlockItem item))continue;
            boolean isTorch=profile.pick()?candidate.getDescriptionId().toLowerCase(java.util.Locale.ROOT).contains("torch"):item.getBlock() instanceof TorchBlock;if(!isTorch)continue;
            int count=candidate.getCount();var result=item.place(new BlockPlaceContext(player,context.getHand(),candidate,new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside())));
            if(player.getAbilities().instabuild)candidate.setCount(count);if(result.consumesAction())return result;
        }return InteractionResult.PASS;
    }
    public static boolean enchanting(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
}

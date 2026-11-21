package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/** Real IC2 EU tools. The modern harvest callback performs exactly one debit per mined block. */
public class ElectricTool extends Item implements IElectricItem {
    public static Item create(ToolProfile profile){return profile.pick()?new ElectricPickaxe(profile):new ElectricChainsaw(profile);}
    private final ToolProfile profile;
    public ElectricTool(ToolProfile profile){super(new Properties().stacksTo(1));this.profile=profile;}
    public ToolProfile profile(){return profile;}
    public boolean powered(ItemStack stack){return ElectricItem.manager.canUse(stack,profile.breakCost());}
    public boolean effective(BlockState state){return profile.pick()&&state.is(BlockTags.MINEABLE_WITH_PICKAXE)
        ||profile.axe()&&state.is(BlockTags.MINEABLE_WITH_AXE)||profile.shovel()&&state.is(BlockTags.MINEABLE_WITH_SHOVEL)
        ||profile.shears()&&(state.is(BlockTags.LEAVES)||state.is(BlockTags.WOOL));}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return powered(stack)&&effective(state)?profile.speed():1f;}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return powered(stack)&&effective(state);}
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity user){
        if(!level.isClientSide)ElectricItem.manager.use(stack,profile.breakCost(),user);return true;
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.level().isClientSide&&profile.hitCost()>0&&ElectricItem.manager.use(stack,profile.hitCost(),attacker)){
            target.hurt(attacker instanceof Player p?attacker.damageSources().playerAttack(p):attacker.damageSources().mobAttack(attacker),profile.hitDamage());
        }
        return true;
    }
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action){
        return powered(stack)&&((profile.pick()&&ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(action))
            ||(profile.axe()&&action==ToolActions.AXE_DIG)
            ||(profile.shovel()&&action==ToolActions.SHOVEL_DIG)
            ||(profile.shears()&&ToolActions.DEFAULT_SHEARS_ACTIONS.contains(action)));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        if(!profile.shears()||!(entity instanceof IForgeShearable target)||!powered(stack))return InteractionResult.PASS;
        if(target.isShearable(stack,entity.level(),entity.blockPosition())){
            if(!entity.level().isClientSide&&ElectricItem.manager.use(stack,profile.breakCost(),player))
                for(ItemStack drop:target.onSheared(player,stack,entity.level(),entity.blockPosition(),0))entity.spawnAtLocation(drop,1);
            return InteractionResult.sidedSuccess(entity.level().isClientSide);
        }return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        for(int slot=0;slot<player.getInventory().items.size();slot++){
            ItemStack candidate=player.getInventory().items.get(slot);
            if(!(candidate.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof TorchBlock))continue;
            int count=candidate.getCount();
            var use=new net.minecraft.world.item.context.BlockPlaceContext(player,context.getHand(),candidate,
                new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()));
            InteractionResult result=item.place(use);
            if(player.getAbilities().instabuild)candidate.setCount(count);
            if(result.consumesAction())return result;
        }return InteractionResult.PASS;
    }
    @Override public boolean canProvideEnergy(ItemStack stack){return false;}
    @Override public double getMaxCharge(ItemStack stack){return profile.capacity();}
    @Override public int getTier(ItemStack stack){return profile.tier();}
    @Override public double getTransferLimit(ItemStack stack){return profile.transfer();}
    @Override public boolean isBarVisible(ItemStack stack){return ElectricItem.manager.getCharge(stack)<profile.capacity();}
    @Override public int getBarWidth(ItemStack stack){return (int)Math.round(13*ElectricItem.manager.getCharge(stack)/profile.capacity());}
    @Override public int getBarColor(ItemStack stack){return 0x66bbff;}
}

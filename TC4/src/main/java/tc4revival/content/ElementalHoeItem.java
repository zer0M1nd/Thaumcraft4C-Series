package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.Event;
import tc4revival.network.FocusEffectPacket;

/** Native hoe tool actions plus TC4 area tilling and magical growth. */
public  class ElementalHoeItem extends HoeItem implements RepairableItem {
    public ElementalHoeItem() { super(ElementalToolTier.INSTANCE,-3,0,new Properties().rarity(Rarity.RARE)); }
    @Override public int getEnchantmentValue() { return 5; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer(); if (player==null) return InteractionResult.PASS;
        var level=context.getLevel(); var center=context.getClickedPos();
        if (player.isShiftKeyDown()) return till(context,center) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        boolean did=false;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            var pos=center.offset(x,0,z);
            if (till(context,pos)) { FocusEffectPacket.blockSparkle(level,pos,8401408,2); did=true; }
        }
        if (did) return InteractionResult.SUCCESS;
        if (!level.hasChunkAt(center) || !player.mayUseItemAt(center,context.getClickedFace(),context.getItemInHand())) return InteractionResult.PASS;
        var stack=context.getItemInHand(); var state=level.getBlockState(center);
        var event=new BonemealEvent(player,level,center,state,stack);
        boolean canceled=MinecraftForge.EVENT_BUS.post(event);
        boolean fertilized=!canceled && event.getResult()==Event.Result.ALLOW;
        if (!canceled && !fertilized && !(state.getBlock() instanceof MagicalSaplingBlock) && state.getBlock() instanceof BonemealableBlock growable
                && growable.isValidBonemealTarget(level,center,state,level.isClientSide)) {
            if (level instanceof ServerLevel server && growable.isBonemealSuccess(level,level.random,center,state))
                growable.performBonemeal(server,level.random,center,state);
            fertilized=true;
        }
        if (fertilized) { wear(context,1); FocusEffectPacket.blockSparkle(level,center,0,3); did=true; }
        else if (!canceled && state.getBlock() instanceof MagicalSaplingBlock sapling) {
            int threshold=sapling.isSilverwood()?150:20;
            if (stack.getDamageValue()+threshold<=stack.getMaxDamage()) {
                if (level instanceof ServerLevel server) sapling.growTree(state,server,center,level.random);
                wear(context,sapling.isSilverwood()?25:5);
                FocusEffectPacket.blockSparkle(level,center,0,2); did=true;
            }
        }
        if (did && !level.isClientSide) level.playSound(null,center,TCSounds.WAND.get(),SoundSource.BLOCKS,.75f,.9f+level.random.nextFloat()*.2f);
        return did?InteractionResult.SUCCESS:InteractionResult.PASS;
    }
    private boolean till(UseOnContext context,BlockPos pos) {
        var level=context.getLevel();var player=context.getPlayer();var stack=context.getItemInHand();
        if(stack.isEmpty() || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack))return false;
        var hit=new net.minecraft.world.phys.BlockHitResult(context.getClickLocation().add(pos.subtract(context.getClickedPos()).getX(),pos.subtract(context.getClickedPos()).getY(),pos.subtract(context.getClickedPos()).getZ()),context.getClickedFace(),pos,context.isInside());
        return super.useOn(new UseOnContext(player,context.getHand(),hit)).consumesAction();
    }
    private static void wear(UseOnContext context,int amount) {
        context.getItemInHand().hurtAndBreak(amount,context.getPlayer(),owner->owner.broadcastBreakEvent(context.getHand()));
    }
}

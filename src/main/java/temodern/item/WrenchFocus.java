package temodern.item;

import java.util.List;
import java.util.Map;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.content.FocusUpgrade;
import thaumcraft.api.wands.ItemFocusBasic;

/** Routes real AE2 wrench use through an attached focus, preserving the wand in all exit paths. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=temodern.TE.ID)
public final class WrenchFocus extends ItemFocusBasic {
    @Override public int color(ItemStack stack) { return 0x9977cc; }
    @Override public Map<Aspect,Integer> costs(ItemStack stack) { return Map.of(tc4revival.aspect.TCAspects.catalog().require("ignis"),10,tc4revival.aspect.TCAspects.catalog().require("aer"),10); }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) { return List.of(); }
    @Override public String sortingPrefix() { return "THAUMICENERGISTICS_WRENCH"; }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || hit == null || !level.mayInteract(player, hit.getBlockPos()) || !player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), wand)) return InteractionResultHolder.pass(wand);
        if(!(wand.getItem() instanceof tc4revival.content.CastingWandItem wandItem)
                || !wandItem.consumeForPlayer(wand,player,costs(wand),false,false,false))return InteractionResultHolder.fail(wand);
        if(level.isClientSide)return InteractionResultHolder.success(wand);
        var tool = appeng.core.definitions.AEItems.CERTUS_QUARTZ_WRENCH.stack();
        var original = player.getItemInHand(hand);
        try {
            player.setItemInHand(hand, tool);
            var context = new UseOnContext(player, hand, hit);
            InteractionResult result = appeng.hooks.WrenchHook.onPlayerUseBlock(player,level,hand,hit);
            if (!result.consumesAction()) result=tool.getItem().onItemUseFirst(tool, context);
            if (!result.consumesAction()) result = level.getBlockState(hit.getBlockPos()).use(level, player, hand, hit);
            if (!result.consumesAction()) result = tool.useOn(context);
            if(result.consumesAction()){wandItem.consumeForPlayer(wand,player,costs(wand),false,false,true);effect(player,hit);}
            return new InteractionResultHolder<>(result, wand);
        } finally { player.setItemInHand(hand, original); }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void rotate(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event){
        var player=event.getEntity();var wand=player.getMainHandItem();
        if(!player.isShiftKeyDown()||player.isSpectator()||!(wand.getItem() instanceof tc4revival.content.CastingWandItem wandItem))return;
        var attached=tc4revival.content.WandFocusItem.attached(wand);
        if(!(attached.getItem() instanceof WrenchFocus focus))return;
        event.setCanceled(true);
        if(event.getAction()!=net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START)return;
        var level=player.level();var pos=event.getPos();
        if(level.isClientSide||event.getFace()==null||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,event.getFace(),wand)
                ||!wandItem.consumeForPlayer(wand,player,focus.costs(attached),false,false,false))return;
        var hit=level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(player.getBlockReach())),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        var block=hit;if(block.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK||!block.getBlockPos().equals(pos))return;
        var tool=appeng.core.definitions.AEItems.CERTUS_QUARTZ_WRENCH.stack();
        boolean sneaking=player.isShiftKeyDown();
        try{
            player.setItemInHand(InteractionHand.MAIN_HAND,tool);player.setShiftKeyDown(false);
            var result=appeng.hooks.WrenchHook.onPlayerUseBlock(player,level,InteractionHand.MAIN_HAND,block);
            if(!result.consumesAction()&&appeng.api.parts.PartHelper.getPartHost(level,pos)!=null)
                result=level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,block);
            if(!result.consumesAction()){
                var state=level.getBlockState(pos);var rotated=state.rotate(level,pos,net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
                if(rotated!=state&&rotated.canSurvive(level,pos)){level.setBlockAndUpdate(pos,rotated);result=InteractionResult.SUCCESS;}
            }
            if(result.consumesAction()){wandItem.consumeForPlayer(wand,player,focus.costs(attached),false,false,true);effect(player,block);}
        }finally{player.setItemInHand(InteractionHand.MAIN_HAND,wand);player.setShiftKeyDown(sneaking);}
    }

    private static void effect(Player player,BlockHitResult hit){
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        thaumcraft.api.fx.LegacyEffects.beam(level,player.position().add(0,1.5,0),hit.getBlockPos().getCenter(),tc4revival.aspect.TCAspects.catalog().require("potentia").color(),.2f,6);
    }
}

package thaumicfutures.foci;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

public class BuilderFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
    @Override public int color(ItemStack focus) { return 8776595; }
    @Override public String sortingPrefix() { return "FBU"; }
    @Override public int cooldownMillis(ItemStack focus) { return 250; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) { var a=TCAspects.catalog();return Map.of(a.require("aer"),10,a.require("terra"),10,a.require("ordo"),10); }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        return rank>=1 && rank<=5 ? List.of(FocusUpgrade.FRUGAL,FocusUpgrade.ENLARGE) : List.of();
    }
    public static BlockHitResult target(Player player) {
        var eye=player.getEyePosition();return player.level().clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(64)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
    }
    public static ItemStack picked(ItemStack wand) { return TradeFocusItem.picked(wand); }
    public List<BlockPos> selection(ItemStack wand,BlockHitResult hit,Player player) {
        var pos=hit.getBlockPos();var face=hit.getDirection();var axis=face.getAxis();
        double origin=axis.choose(pos.getX(),pos.getY(),pos.getZ())+.5;
        double coordinate=axis.choose(player.getX(),player.getY(),player.getZ());
        int count=BuilderLayout.length(origin,coordinate,upgradeLevel(attached(wand),FocusUpgrade.ENLARGE.id));
        var out=new ArrayList<BlockPos>(count);for(int i=1;i<=count;i++)out.add(pos.relative(face,i));return out;
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult ignored) {
        var hit=target(player);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        if(player.isShiftKeyDown()) {
            if(!level.isClientSide && level.getBlockEntity(hit.getBlockPos())==null) {
                var state=level.getBlockState(hit.getBlockPos());var picked=state.getCloneItemStack(hit,level,hit.getBlockPos(),player);
                if(picked.getItem() instanceof BlockItem block && !block.getBlock().defaultBlockState().hasBlockEntity()) {
                    picked=new ItemStack(picked.getItem());stack.getOrCreateTag().put("picked",picked.save(new net.minecraft.nbt.CompoundTag()));
                    player.displayClientMessage(Component.translatable("thaumicfutures.focus.builder.selected",picked.getHoverName()),true);
                }
            }
        } else if(!picked(stack).isEmpty() && player instanceof ServerPlayer server) {
            BuilderPlacements.enqueue(server,hand,stack,hit,selection(stack,hit,player),picked(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}

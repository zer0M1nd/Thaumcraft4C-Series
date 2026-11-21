package technomancy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.nodes.NodeApi;

/** Original experimental focus; captured nodes belong to this stack, never the singleton item. */
public class FusionFocus extends WandFocusItem {
    @Override public int color(ItemStack focus){return 0xff5a20;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){var result=new LinkedHashMap<Aspect,Integer>();for(var a:TCAspects.catalog().primals())result.put(a,3000);return result;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return List.of(FocusUpgrade.FRUGAL);}
    @Override public String sortingPrefix(){return "technomancy_fusion";}
    @Override public boolean startBreak(ItemStack wand,BlockPos pos,Player player) {
        var focus=attached(wand);if(!(focus.getItem() instanceof FusionFocus)||!(player.level().getBlockEntity(pos) instanceof AuraNodeBlockEntity node))return false;
        if(player.level().isClientSide)return true;
        var level=(net.minecraft.server.level.ServerLevel)player.level();
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,wand)||net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,pos,level.getBlockState(pos),player)))return true;
        var snapshot=NodeApi.snapshot(node);var storage=focus.getOrCreateTag();
        String type=snapshot.type().name(),modifier=snapshot.modifier()==null?"":snapshot.modifier().name();
        if(storage.contains("nodeType")&&(!type.equals(storage.getString("nodeType"))||!modifier.equals(storage.getString("nodeModifier"))))return true;
        var amounts=AspectNbt.read(storage.getCompound("nodeAspects"),TCAspects.catalog()).amounts();
        for(var e:snapshot.charge().unwrap().view().entrySet())amounts.add(e.getKey(),Math.min(e.getValue(),32767-amounts.amount(e.getKey())));
        var saved=new CompoundTag();AspectNbt.write(saved,amounts);storage.put("nodeAspects",saved);storage.putString("nodeType",type);storage.putString("nodeModifier",modifier);
        if(level.removeBlock(pos,false))attach(wand,focus);return true;
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        var focus=attached(stack);if(!focus.hasTag()||!focus.getTag().contains("nodeType")||!(stack.getItem() instanceof CastingWandItem wand)||hit==null)return InteractionResultHolder.pass(stack);
        var pos=level.getBlockState(hit.getBlockPos()).canBeReplaced()?hit.getBlockPos():hit.getBlockPos().relative(hit.getDirection());
        if(!level.hasChunkAt(pos)||!level.isEmptyBlock(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack)||!wand.consumeForPlayer(stack,player,costs(focus),false,false,false))return InteractionResultHolder.fail(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,hit.getDirection()))return InteractionResultHolder.fail(stack);
        var tag=focus.getTag();AuraNodeType type;AuraNodeModifier modifier;
        try {type=AuraNodeType.valueOf(tag.getString("nodeType"));modifier=tag.getString("nodeModifier").isEmpty()?null:AuraNodeModifier.valueOf(tag.getString("nodeModifier"));}
        catch(IllegalArgumentException invalid){return InteractionResultHolder.fail(stack);}
        var amounts=AspectNbt.read(tag.getCompound("nodeAspects"),TCAspects.catalog()).amounts();
        if(amounts.isEmpty()||!level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3))return InteractionResultHolder.fail(stack);
        if(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){
                level.removeBlock(pos,false);return InteractionResultHolder.fail(stack);
            }
            NodeApi.configure(node,thaumcraft.api.aspects.AspectList.from(amounts),type,modifier,false);
            tag.remove("nodeAspects");tag.remove("nodeType");tag.remove("nodeModifier");attach(stack,focus);
        }
        return InteractionResultHolder.consume(stack);
    }
}

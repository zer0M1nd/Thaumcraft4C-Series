package thmodern.golem;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.wands.ItemFocusBasic;

public final class AnimationFocusItem extends ItemFocusBasic {
    @Override public int color(ItemStack focus){return 15054592;}
    @Override public String ornamentTexture(){return "focus_whatever_orn";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return costs(1000);}
    private Map<Aspect,Integer> costs(int cost){var result=new LinkedHashMap<Aspect,Integer>();thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.LEGACY_SIX).forEach(a->result.put(a,cost));return result;}
    @Override public String sortingPrefix(){return "ANIMATION";}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        if(rank<1||rank>5)return List.of();var list=new ArrayList<>(List.of(FocusUpgrade.FRUGAL,FocusUpgrade.EXTEND));
        if(rank==3)list.add(FocusUpgrade.require("thaumichorizons:berserk"));
        if(rank==5)list.add(FocusUpgrade.require("thaumichorizons:detonation"));return List.copyOf(list);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(hit==null||hit.getType()!=HitResult.Type.BLOCK||!(stack.getItem() instanceof CastingWandItem wand))return InteractionResultHolder.pass(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!(player instanceof ServerPlayer server)||!player.mayUseItemAt(hit.getBlockPos(),hit.getDirection(),stack)||!AnimatedGolems.eligible(server.serverLevel(),hit.getBlockPos()))return InteractionResultHolder.fail(stack);
        var costs=costs(GolemProfiles.select(level.getBlockState(hit.getBlockPos())).visCost());
        var focus=WandFocusItem.attached(stack);
        boolean created=AnimatedGolems.animate(server,hit.getBlockPos(),600+upgradeLevel(focus,FocusUpgrade.EXTEND.id)*200,
                getUpgradeLevel(focus,"thaumichorizons:berserk")>0,getUpgradeLevel(focus,"thaumichorizons:detonation")>0,new AnimatedGolems.Payment(){
                    private net.minecraft.nbt.CompoundTag before;
                    @Override public boolean commit(){if(player.getItemInHand(hand)!=stack)return false;before=stack.hasTag()?stack.getTag().copy():null;return wand.consumeForPlayer(stack,player,costs,false,false,true);}
                    @Override public void refund(){stack.setTag(before);}
                });
        return created?InteractionResultHolder.consume(stack):InteractionResultHolder.fail(stack);
    }
}

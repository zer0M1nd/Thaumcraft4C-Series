package emtmodern.focus;

import emtmodern.ElectroMagicTools;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
import java.util.*;

public class ShieldFocus extends thaumcraft.api.wands.ItemFocusBasic {
    @Override public int color(ItemStack focus){return 2003199;}
    @Override public String sortingPrefix(){return "SHIELD";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return Map.of(TCAspects.catalog().require("ordo"),5,TCAspects.catalog().require("aqua"),5,TCAspects.catalog().require("aer"),5);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return List.of();}
    @Override public boolean isFoil(ItemStack stack){return true;}
    protected boolean consume(ItemStack stack,Player player){return player.getAbilities().instabuild||stack.getItem() instanceof CastingWandItem wand&&wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,true,true);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        player.startUsingItem(hand);
        if(!level.isClientSide&&consume(stack,player)){var shield=new ShieldEntity(ElectroMagicTools.SHIELD_ENTITY.get(),level);shield.owner(player);level.addFreshEntity(shield);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){if(player.level().isClientSide)return;for(var effect:new ArrayList<>(player.getActiveEffects()))player.removeEffect(effect.getEffect());if(!consume(stack,player))player.stopUsingItem();}
}

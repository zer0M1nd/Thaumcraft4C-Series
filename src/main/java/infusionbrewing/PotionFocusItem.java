package infusionbrewing;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.wands.ItemFocusBasic;

public class PotionFocusItem extends ItemFocusBasic {
    @Override public int color(ItemStack focus){return 0xff60ff;}
    @Override public String sortingPrefix(){return "POTION";}
    @Override public String sortingKey(ItemStack focus){return PotionData.read(focus).map(p->"POTION:"+p.legacyId()+":"+p.amplifier()).orElse("POTION:empty");}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return List.of();}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){
        int scale=PotionData.read(focus).map(p->p.amplifier()+1).orElse(1);
        return Map.of(TCAspects.catalog().require("ignis"),100*scale,TCAspects.catalog().require("perditio"),50*scale);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!(stack.getItem() instanceof CastingWandItem wand))return InteractionResultHolder.fail(stack);
        var focus=attached(stack);var optional=PotionData.read(focus);if(optional.isEmpty())return InteractionResultHolder.fail(stack);
        var data=optional.get();
        if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true))return InteractionResultHolder.fail(stack);
        var projectile=new ThrownPotion(level,player);projectile.setItem(new PotionData(data.legacyId(),BrewingRules.instant(data.legacyId())?1:2400,data.amplifier()).potion(true));
        projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),-20,.5f,1);
        level.addFreshEntity(projectile);
        level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
        return InteractionResultHolder.success(stack);
    }
    @Override public void appendHoverText(ItemStack focus,Level level,List<Component> lines,TooltipFlag flags){PotionTooltips.append(focus,lines);super.appendHoverText(focus,level,lines,flags);}
}

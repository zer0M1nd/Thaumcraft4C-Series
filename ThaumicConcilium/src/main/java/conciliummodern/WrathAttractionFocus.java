package conciliummodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

public final class WrathAttractionFocus extends ConciliumFocus implements thaumcraft.api.IWarpingGear {
    @Override public int getWarp(ItemStack stack,Player player){return 3;}
    @Override public String sortingPrefix(){return "CONCILIUM_WRATH_ATTRACTION";}
    @Override public String depthTexture(){return "thaumicconcilium:item/wrath_attraction_depth";}
    @Override public int color(ItemStack focus){return 0xff4500;}
    @Override public int cooldownMillis(ItemStack focus){return 50;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return vis("ignis",500,"perditio",1000);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return ranks(rank,List.of(FocusUpgrade.FRUGAL,FocusUpgrade.ENLARGE),"burnout");}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide && stack.getItem() instanceof CastingWandItem wand && wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true)){
            var actor=thaumcraft.api.wands.WandActors.resolve(player);
            if(actor instanceof net.minecraft.world.entity.LivingEntity living){
                boolean burnout=has(attached(stack),"burnout");
                double range=5+5*wand.focusUpgrade(stack,FocusUpgrade.ENLARGE);
                for(var mob:level.getEntitiesOfClass(Monster.class,actor.getBoundingBox().inflate(range),e->!e.isAlliedTo(actor))){
                    mob.setTarget(living);if(burnout)mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,3));
                }
                WrathGlyph.spawn(level,actor,burnout);
            }
            level.playSound(null,actor.blockPosition(),ConciliumSounds.IRA.get(),net.minecraft.sounds.SoundSource.PLAYERS,.9f,1);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}

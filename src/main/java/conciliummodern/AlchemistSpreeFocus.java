package conciliummodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

public final class AlchemistSpreeFocus extends ConciliumFocus {
    @Override public String sortingPrefix(){return "CONCILIUM_SPREE";}
    @Override public int color(ItemStack focus){return 0xeeeeee;}
    @Override public String depthTexture(){return "thaumicconcilium:item/alchemist_spree_depth";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return vis("aqua",100,"perditio",200,"ordo",200);}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        if(rank<1 || rank>5)return List.of();
        var result=new ArrayList<>(List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,FocusUpgrade.ENLARGE));
        if(rank==3)result.add(FocusUpgrade.require("thaumicconcilium:primal_essence"));return result;
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!requireRing(player))return InteractionResultHolder.fail(wand);player.startUsingItem(hand);return InteractionResultHolder.consume(wand);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player.level() instanceof ServerLevel level) || !(stack.getItem() instanceof CastingWandItem wand))return;
        var focus=attached(stack);
        if(!ring(player) || !wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){player.stopUsingItem();return;}
        var resolved=thaumcraft.api.wands.WandActors.resolve(player);
        LivingEntity actor=resolved instanceof LivingEntity living?living:player;
        DistortionField.refresh(player,actor);
        if(player.getTicksUsingItem()%10==0)level.playSound(null,actor.blockPosition(),ConciliumSounds.BUBBLES.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
        float potency=wand.focusPotency(stack,player);boolean primal=has(focus,"primal_essence");
        // The upstream enlarge choice did not change this conversion volume.
        for(var projectile:level.getEntitiesOfClass(Projectile.class,actor.getBoundingBox().inflate(3,1.5,3),Entity::isAlive)){
            Entity replacement=null;
            if(projectile instanceof Snowball)replacement=new FrostShardEntity(level,actor,1.5f,1+potency,primal?1+(int)potency:0,false,false);
            else if(projectile instanceof ThrownEgg){replacement=TCEntities.TAINT_SPIDER.get().create(level);if(replacement!=null)replacement.moveTo(projectile.getX(),projectile.getY(),projectile.getZ(),projectile.getYRot(),0);}
            else if(projectile instanceof EmberEntity ember){
                // Public vanilla serialization exposes the original ember damage without changing the base mod API.
                var tag=new CompoundTag();ember.saveWithoutId(tag);float damage=tag.getFloat("damage");
                if(!Float.isFinite(damage))continue;
                replacement=new ExplosiveOrbEntity(level,actor,(1+potency)*damage,primal);
                replacement.setDeltaMovement(replacement.getDeltaMovement().add(level.random.nextGaussian()*.1,0,level.random.nextGaussian()*.1));
            }else if(projectile instanceof PrimalOrbEntity)replacement=new PechBlastEntity(level,actor,potency,(int)(potency*60),primal);
            else if(projectile instanceof EldritchOrbEntity){ConciliumShadowbeam.cast(player,actor,(potency+1)*3);projectile.discard();}
            else if(projectile instanceof ClearWaterProjectile){
                var bottle=new ThickTaintProjectile(ConciliumEntities.THICK_TAINT.get(),level);bottle.setOwner(actor);bottle.setPos(actor.getX(),actor.getEyeY()-.1,actor.getZ());
                bottle.shootFromRotation(actor,actor.getXRot(),actor.getYRot(),-20,.5f,1);replacement=bottle;
            }
            if(replacement!=null && level.addFreshEntity(replacement)){
                projectile.discard();if(replacement instanceof TaintSpiderEntity)ConciliumArcNetwork.taint(level,replacement.position());
            }
        }
    }
}

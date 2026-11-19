package conciliummodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class EtherealManipulatorFocus extends WandFocusItem {
    @Override public String sortingPrefix(){return "CONCILIUM_ETHEREAL";}
    @Override public String ornamentTexture(){return "thaumicconcilium:item/ethereal_manipulator_orn";}
    @Override public int color(ItemStack stack){return 0xff00ff;}
    @Override public boolean costPerTick(ItemStack stack){return true;}
    @Override public java.util.Map<Aspect,Integer> costs(ItemStack stack){return java.util.Map.of(TCAspects.catalog().require("ignis"),30,TCAspects.catalog().require("ordo"),10,TCAspects.catalog().require("aer"),30);}
    @Override public java.util.List<FocusUpgrade> upgradesForRank(ItemStack stack,int rank){return rank>=1 && rank<=5?java.util.List.of(FocusUpgrade.FRUGAL):java.util.List.of();}
    public static float range(ItemStack wand){var focus=attached(wand);float range=focus.hasTag() && focus.getTag().contains("Range",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)?focus.getTag().getFloat("Range"):5;return Float.isFinite(range)?net.minecraft.util.Mth.clamp(range,1,16):5;}
    public static void range(ItemStack wand,float range){var focus=attached(wand);if(!(focus.getItem() instanceof EtherealManipulatorFocus))return;focus.getOrCreateTag().putFloat("Range",range);attach(wand,focus);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){if(!level.isClientSide)range(wand,5);player.startUsingItem(hand);return InteractionResultHolder.consume(wand);}
    @Override public void release(ItemStack wand,Level level,Player player,int remaining){if(!level.isClientSide)range(wand,5);}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(player.level().isClientSide || !(stack.getItem() instanceof CastingWandItem wand))return;
        var actor=thaumcraft.api.wands.WandActors.resolve(player);float range=range(stack);var start=actor.getEyePosition();var end=start.add(actor.getLookAngle().scale(range));
        Entity target=null;double nearest=Double.POSITIVE_INFINITY;
        // Original nonCollide ray selects the nearest entity first, then checks whether it can be moved.
        // This includes unpickable items/projectiles while preserving obstruction by a nearer living entity.
        for(var entity:actor.level().getEntities(actor,actor.getBoundingBox().expandTowards(end.subtract(start)).inflate(3),Entity::isAlive)){
            if(entity.distanceToSqr(actor)<1 || !FocusTargeting.visible(actor,entity.getEyePosition()))continue;var bounds=entity.getBoundingBox().inflate(Math.max(.8,entity.getPickRadius()));
            var hit=bounds.clip(start,end);double distance=bounds.contains(start)?0:hit.map(start::distanceToSqr).orElse(Double.POSITIVE_INFINITY);
            if(distance<nearest){nearest=distance;target=entity;}
        }
        if(!(target instanceof ItemEntity || target instanceof Projectile))return;
        if(!wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true)){player.stopUsingItem();return;}
        var destination=actor.position().add(0,actor.getBbHeight()/2+.5,0).add(actor.getLookAngle().scale(range-1));
        var motion=destination.subtract(target.position().add(0,target.getBbHeight()/2,0));
        if(motion.lengthSqr()>1)motion=motion.normalize();
        target.setDeltaMovement(motion.scale(.3333f));target.hurtMarked=true;
        thaumcraft.api.fx.LegacyEffects.sparkle(actor.level(),target.position(),0xffffff,1,1,0);
    }
}

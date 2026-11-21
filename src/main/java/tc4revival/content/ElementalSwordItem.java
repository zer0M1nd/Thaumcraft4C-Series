package tc4revival.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolActions;

public  class ElementalSwordItem extends ThaumiumSwordItem {
    public ElementalSwordItem() { super(ElementalToolTier.INSTANCE); }
    @Override public int getUseDuration(ItemStack stack) { return LegacySwordRules.USE_DURATION; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var sword = player.getItemInHand(hand);
        var offhand = player.getOffhandItem();
        if (hand == InteractionHand.MAIN_HAND && (offhand.canPerformAction(ToolActions.SHIELD_BLOCK)
                || offhand.getUseAnimation() == UseAnim.BLOCK)) {
            return InteractionResultHolder.pass(sword);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(sword);
    }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining) {
        if (!(user instanceof Player player)) return;
        int elapsed=getUseDuration(stack)-remaining;
        var velocity=player.getDeltaMovement(); double y=velocity.y;
        if (y<0) { y/=(double)1.2f; player.fallDistance/=1.2f; }
        y+=(double).08f; if (y>.5) y=(double).2f;
        player.setDeltaMovement(velocity.x,y,velocity.z);
        if (player instanceof ServerPlayer server) ElementalSwordCombat.resetFloating(server);
        if (level.isClientSide) tc4revival.client.ElementalSwordEffects.emit(player);
        for (var target:level.getEntities(player,player.getBoundingBox().inflate(2.5))) {
            if (target instanceof Player || !target.isAlive() || target==player.getVehicle()) continue;
            var offset=target.position().subtract(player.position());
            target.setDeltaMovement(target.getDeltaMovement().add(offset.scale(1/2.5/(offset.length()+.1))));
        }
        if (!level.isClientSide && elapsed%20==0) level.playSound(null,player.getX(),player.getY(),player.getZ(),TCSounds.WIND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,.9f+level.random.nextFloat()*.2f);
        if (elapsed%20==0) stack.hurtAndBreak(1,player,owner->owner.broadcastBreakEvent(player.getUsedItemHand()));
    }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity primary) {
        if (primary.isAlive() && !player.level().isClientSide) {
            var nearby=player.level().getEntities(player,primary.getBoundingBox().inflate(1.2,1.1,1.2)); int count=0;
            if (nearby.size()>1) for (var target:nearby) {
                if (!(target instanceof Mob mob) || !mob.isAlive() || target==primary || player.isAlliedTo(mob)) continue;
                if (mob instanceof GolemEntity golem && golem.ownerName().equals(player.getGameProfile().getName())) continue;
                if (mob instanceof TamableAnimal tame && tame.isOwnedBy(player)) continue;
                ElementalSwordCombat.attack(player,mob); count++;
            }
            if (count>0) player.level().playSound(null,primary.getX(),primary.getY(),primary.getZ(),TCSounds.SWING.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,.9f+player.getRandom().nextFloat()*.2f);
        }
        return false;
    }
}

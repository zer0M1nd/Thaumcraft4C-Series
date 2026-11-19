package tc4revival.content;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

 class PechPickupGoal extends Goal {
    private final PechEntity pech;
    private ItemEntity target;
    private int delay,penalty;
    PechPickupGoal(PechEntity pech) { this.pech=pech; setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean canUse() {
        if (pech.tickCount%tc4revival.config.TCConfig.SERVER.golemDelayTicks()!=0 || --delay>0) return false;
        target=pech.level().getEntitiesOfClass(ItemEntity.class,pech.getBoundingBox().inflate(16),item->item.isAlive()
                && !item.getPersistentData().getBoolean("PechDrop") && pech.canPickup(item.getItem()) && pech.distanceToSqr(item)<=256).stream()
                .min(java.util.Comparator.comparingDouble(pech::distanceToSqr)).orElse(null);
        return target!=null;
    }
    @Override public boolean canContinueToUse() { return target!=null && target.isAlive() && !pech.getNavigation().isDone() && pech.distanceToSqr(target)<256; }
    @Override public void start() { pech.getNavigation().moveTo(target,pech.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)*1.5); delay=0; }
    @Override public void stop() { target=null; }
    @Override public void tick() {
        if (target==null) return;
        pech.getLookControl().setLookAt(target,30,30);
        if (pech.getSensing().hasLineOfSight(target) && --delay<=0) {
            delay=penalty+4+pech.getRandom().nextInt(4);
            pech.getNavigation().moveTo(target,pech.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)*1.5);
            var path=pech.getNavigation().getPath(); var end=path==null ? null : path.getEndNode();
            penalty=end!=null && target.distanceToSqr(end.x,end.y,end.z)<1 ? 0 : Math.min(1000,penalty+10);
        }
        if (pech.distanceToSqr(target)<=1.5) {
            delay=0; int old=target.getItem().getCount(); var left=pech.pickup(target.getItem());
            if (left.isEmpty()) target.discard(); else target.setItem(left);
            if (left.getCount()!=old) pech.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP,.2f,((pech.getRandom().nextFloat()-pech.getRandom().nextFloat())*.7f+1)*2);
        }
    }
}

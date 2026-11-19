package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

/** The door-based village collection was removed; home POIs supply shelter positions. */
 class TaintedVillageGoals {
    private TaintedVillageGoals() {}
    private record Aggressor(BlockPos center,java.util.UUID entity,long expires) {}
    private static final java.util.Map<ServerLevel,java.util.List<Aggressor>> threats=new java.util.WeakHashMap<>();
    static void reportAttack(TaintedAnimalEntity mob,net.minecraft.world.entity.LivingEntity attacker) {
        if(!(mob.level() instanceof ServerLevel level) || attacker==null || !mob.hasRestriction())return;
        var list=threats.computeIfAbsent(level,ignored->new java.util.ArrayList<>());
        list.removeIf(entry->entry.entity().equals(attacker.getUUID()) && entry.center().equals(mob.getRestrictCenter()));
        list.add(new Aggressor(mob.getRestrictCenter(),attacker.getUUID(),level.getGameTime()+300));
    }
    static net.minecraft.world.entity.LivingEntity nearestAggressor(net.minecraft.world.entity.animal.IronGolem golem) {
        if(!(golem.level() instanceof ServerLevel level))return null;
        var list=threats.get(level);if(list==null)return null;
        list.removeIf(entry->entry.expires()<level.getGameTime() || !(level.getEntity(entry.entity()) instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive());
        return list.stream().filter(entry->entry.center().closerToCenterThan(golem.position(),32))
                .map(entry->(net.minecraft.world.entity.LivingEntity)level.getEntity(entry.entity()))
                .filter(golem::canAttack).min(java.util.Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }
    static  class Defend extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final net.minecraft.world.entity.animal.IronGolem golem;
        private net.minecraft.world.entity.LivingEntity attacker;
        Defend(net.minecraft.world.entity.animal.IronGolem golem) { super(golem,false);this.golem=golem; }
        @Override public boolean canUse() { attacker=nearestAggressor(golem);return attacker!=null; }
        @Override public void start() { golem.setTarget(attacker);super.start(); }
    }

    static void updateHome(TaintedAnimalEntity mob) {
        var data=mob.getPersistentData();
        int remaining=data.getInt("tc4VillageCheck")-1;
        if(remaining>0) { data.putInt("tc4VillageCheck",remaining);return; }
        data.putInt("tc4VillageCheck",70+mob.getRandom().nextInt(50));
        if(!(mob.level() instanceof ServerLevel level))return;
        var home=level.getPoiManager().findClosest(type->type.is(PoiTypes.HOME),mob.blockPosition(),32,PoiManager.Occupancy.ANY);
        if(home.isEmpty()) { mob.clearRestriction();return; }
        var homes=level.getPoiManager().getInRange(type->type.is(PoiTypes.HOME),home.get(),32,PoiManager.Occupancy.ANY)
                .map(net.minecraft.world.entity.ai.village.poi.PoiRecord::getPos).toList();
        long x=0,y=0,z=0;for(var pos:homes) { x+=pos.getX();y+=pos.getY();z+=pos.getZ(); }
        var center=new BlockPos((int)(x/homes.size()),(int)(y/homes.size()),(int)(z/homes.size()));
        double radius=32;for(var pos:homes)radius=Math.max(radius,Math.sqrt(pos.distSqr(center))+1);
        mob.restrictTo(center,(int)(radius*.6));
    }
    static  class Shelter extends Goal {
        private final TaintedAnimalEntity mob;
        private BlockPos target;
        Shelter(TaintedAnimalEntity mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if(!(mob.level() instanceof ServerLevel level) || (level.isDay() && !level.isRaining())
                    || mob.getRandom().nextInt(50)!=0 || !level.dimensionType().hasSkyLight())return false;
            target=level.getPoiManager().findClosest(type->type.is(PoiTypes.HOME),mob.blockPosition(),32,PoiManager.Occupancy.ANY).orElse(null);
            return target!=null && !target.closerToCenterThan(mob.position(),2);
        }
        @Override public void start() { mob.getNavigation().moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,1); }
        @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone(); }
    }
    static  class DoorRestriction extends Goal {
        private final TaintedAnimalEntity mob;
        DoorRestriction(TaintedAnimalEntity mob) { this.mob=mob; }
        @Override public boolean canUse() { return !mob.level().isDay() && mob.hasRestriction() && mob.getRestrictCenter().closerToCenterThan(mob.position(),2); }
        @Override public void start() { ((GroundPathNavigation)mob.getNavigation()).setCanOpenDoors(false); }
        @Override public void stop() { ((GroundPathNavigation)mob.getNavigation()).setCanOpenDoors(true); }
    }
}

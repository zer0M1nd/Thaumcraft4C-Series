package thaumicexploration;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import tc4revival.content.SmallTaintacleEntity;
public class TaintacleMinion extends SmallTaintacleEntity {
    public TaintacleMinion(EntityType<? extends TaintacleMinion> type,Level level){super(type,level);}
    @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof Monster&&!(target instanceof TaintacleMinion)?target:null);}
    @Override public void tick(){
        if(!level().isClientSide&&(getTarget()==null||!getTarget().isAlive())){
            var target=level().getEntitiesOfClass(Monster.class,getBoundingBox().inflate(16,8,16),e->!(e instanceof TaintacleMinion)&&e.isAlive()).stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(target);
        }
        super.tick();
    }
}

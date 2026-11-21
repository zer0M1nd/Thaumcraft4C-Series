package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public  class GolemEssentiaEmptyGoal extends Goal {
    private final GolemWorkHost host;
    private BlockPos target;
    public GolemEssentiaEmptyGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean canUse() {
        if (!host.workEnabled() || host.essentia()==null || host.essentiaAmount()<=0) return false;
        target=GolemEssentiaTargets.find(host);
        return target!=null && GolemWorkRange.reaches(host,target);
    }
    @Override public boolean canContinueToUse() { return false; }
    @Override public void start() {
        if (!(host.mob().level() instanceof ServerLevel level) || target==null || !level.hasChunkAt(target)) return;
        host.mob().getNavigation().stop();
        var entity=level.getBlockEntity(target); int remaining=host.essentiaAmount(); boolean changed=false;
        if (entity instanceof EssentiaJarBlockEntity jar) {
            remaining=jar.addToContainer(host.essentia(),remaining); changed=true;
        } else if (entity instanceof EssentiaReservoirBlockEntity reservoir) {
            if (GolemEssentiaTargets.accepts(host,reservoir,reservoir.facing())) {
                int added=reservoir.addEssentia(host.essentia(),remaining,reservoir.facing());
                if (added>0) { remaining-=added; changed=true; }
            }
        } else if (entity instanceof EssentiaTransport transport) {
            for (var face:host.markers().faces(target,level.dimension().location(),(byte)-1)) {
                if (host.markers().inDimension(level.dimension().location()).stream().noneMatch(marker->marker.pos().equals(target) && marker.face()==face && GolemEssentiaFilters.matches(host,host.essentia(),marker.color()))) continue;
                if (!GolemEssentiaTargets.accepts(host,transport,face)) continue;
                int added=transport.addEssentia(host.essentia(),remaining,face);
                if (added<=0) continue;
                remaining-=added; changed=true; break;
            }
        }
        if (!(entity instanceof EssentiaTransport) && !(entity instanceof EssentiaJarBlockEntity)) {
            for(var marker:host.markers().inDimension(level.dimension().location())) {
                if(!marker.pos().equals(target) || !GolemEssentiaFilters.matches(host,host.essentia(),marker.color()))continue;
                var storage=GolemEssentiaAccess.at(level,target,marker.face(),true);
                if(storage==null)continue;
                int added=storage.insert(host.essentia(),remaining,false);
                if(added>0){remaining-=added;changed=true;break;}
            }
        }
        if (changed) {
            host.essentia(remaining==0?null:host.essentia(),remaining);
            level.playSound(null,host.mob(),SoundEvents.GENERIC_SWIM,SoundSource.NEUTRAL,.2f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
            level.sendBlockUpdated(target,level.getBlockState(target),level.getBlockState(target),2);
        }
    }
}

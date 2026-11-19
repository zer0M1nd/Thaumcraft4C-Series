package tc4revival.content;

import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Pull directly from home, or select the fullest alembic above a non-transport home block. */
public  class GolemEssentiaGatherGoal extends Goal {
    private final GolemWorkHost host;
    private long nextCheck;
    private int height;
    private tc4revival.aspect.Aspect select(thaumcraft.api.aspects.IEssentiaStorage storage) {
        if(storage==null || host.essentiaAmount()>=host.configuration().carryLimit())return null;
        return storage.contents().view().entrySet().stream()
                .filter(e->e.getValue()>0 && GolemEssentiaFilters.matches(host,e.getKey(),-2)
                        && (host.essentiaAmount()==0 || Objects.equals(host.essentia(),e.getKey()))
                        && storage.extract(e.getKey(),1,true)>0)
                .map(java.util.Map.Entry::getKey).sorted(java.util.Comparator.comparing(tc4revival.aspect.Aspect::tag)).findFirst().orElse(null);
    }
    public GolemEssentiaGatherGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    private tc4revival.aspect.Aspect select(EssentiaTransport transport,Direction face) {
        if(host.essentiaAmount()>=host.configuration().carryLimit())return null;
        var candidates=new java.util.LinkedHashSet<tc4revival.aspect.Aspect>();
        if(transport instanceof thaumcraft.api.aspects.IEssentiaStorage storage)candidates.addAll(storage.contents().aspects());
        else if(transport instanceof RevealedAspectContainer container)candidates.addAll(container.revealedAspects().aspects());
        else { candidates.add(transport.essentiaType(face));candidates.add(transport.essentiaType(null)); }
        for(var aspect:candidates) if(aspect!=null && available(transport,aspect,face)>0 && GolemEssentiaFilters.matches(host,aspect,-2)
                && (host.essentiaAmount()==0 || Objects.equals(host.essentia(),aspect)))return aspect;
        return null;
    }
    private int available(EssentiaTransport transport,tc4revival.aspect.Aspect aspect,Direction face) {
        return transport instanceof thaumcraft.api.aspects.IEssentiaStorage storage?storage.contents().amount(aspect):
                transport instanceof RevealedAspectContainer container?container.revealedAspects().amount(aspect):transport.essentiaAmount(face);
    }
    @Override public boolean canUse() {
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || !host.mob().getNavigation().isDone()
                || nextCheck>level.getGameTime()) return false;
        var pos=host.homeInventory();
        if (!level.hasChunkAt(pos) || !GolemWorkRange.reaches(host,pos,6)) return false;
        height=0; var entity=level.getBlockEntity(pos);
        if(!(entity instanceof EssentiaTransport) && select(GolemEssentiaAccess.at(level,pos,host.homeFacing(),false))!=null) {
            nextCheck=level.getGameTime()+20;return true;
        }
        if (entity instanceof EssentiaTransport transport) {
            var face=entity instanceof AlembicBlockEntity || entity instanceof EssentiaJarBlockEntity ? Direction.UP : entity instanceof EssentiaReservoirBlockEntity reservoir ? reservoir.facing() : host.homeFacing();
            if (transport.canOutput(face) && select(transport,face)!=null) {
                nextCheck=level.getGameTime()+20; return true;
            }
        }
        if (entity!=null) {
            height=-1; int largest=0;
            for (int y=5;y>=0;y--) if (level.getBlockEntity(pos.above(y)) instanceof AlembicBlockEntity alembic) {
                if (!GolemEssentiaFilters.matches(host,alembic.aspect(),-2)) continue;
                if (host.essentiaAmount()!=0 && (host.essentia()!=null && !host.essentia().equals(alembic.aspect()) || host.essentiaAmount()>=host.configuration().carryLimit())) continue;
                if (alembic.amount()<=largest) continue;
                largest=alembic.amount(); height=y; nextCheck=level.getGameTime()+20;
            }
            return height>=0;
        }
        return false;
    }
    @Override public boolean canContinueToUse() { return false; }
    @Override public void start() {
        if (!(host.mob().level() instanceof ServerLevel level)) return;
        var pos=host.homeInventory().above(height); if (!level.hasChunkAt(pos)) return;
        var entity=level.getBlockEntity(pos);
        if (!(entity instanceof EssentiaTransport transport)) {
            var storage=GolemEssentiaAccess.at(level,pos,host.homeFacing(),false);var aspect=select(storage);
            if(aspect==null)return;
            int taken=storage.extract(aspect,host.configuration().carryLimit()-host.essentiaAmount(),false);
            if(taken>0){host.essentia(aspect,host.essentiaAmount()+taken);host.transport().progress();}
            nextCheck=level.getGameTime()+2;return;
        }
        var face=entity instanceof AlembicBlockEntity || entity instanceof EssentiaJarBlockEntity?Direction.UP:host.homeFacing();
        if (entity instanceof EssentiaReservoirBlockEntity reservoir) face=reservoir.facing();
        if (!transport.canOutput(face)) return;
        var aspect=select(transport,face);
        if (aspect==null) return;
        int available=available(transport,aspect,face);
        int requested=Math.min(available,host.configuration().carryLimit()-host.essentiaAmount());
        int taken=transport.takeEssentia(aspect,requested,face);
        if (taken>0) {
            host.essentia(aspect,host.essentiaAmount()+taken);
            level.playSound(null,host.mob(),SoundEvents.GENERIC_SWIM,SoundSource.NEUTRAL,.05f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        } else host.essentia(null,host.essentiaAmount());
        nextCheck=level.getGameTime()+2;
    }
}

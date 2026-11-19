package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

public class MovingLamp implements MovementBehaviour {
    @Override public void tick(MovementContext context){
        if(!ArcaneContraptions.enabled()||!(context.world instanceof ServerLevel server)||context.position==null||context.blockEntityData==null)return;
        if(!(context.temporaryData instanceof Work))context.temporaryData=new Work(context);
        ((Work)context.temporaryData).tick(server);
    }
    private static class Work implements ArcaneLampFuel.Host {
        private final MovementContext context;
        private final ArcaneLampFuel fuel;
        private final boolean fertility;
        private final List<BlockPos> columns=new ArrayList<>();
        private BlockPos last;
        private boolean dirty;
        Work(MovementContext context){
            this.context=context;fertility=((ArcaneLampBlock)context.state.getBlock()).kind()==ArcaneLampBlock.Kind.FERTILITY;
            fuel=new ArcaneLampFuel(this,fertility);fuel.load(context.blockEntityData);
        }
        public Direction facing(){return context.state.getValue(ArcaneLampBlock.FACING);}
        public void changed(){dirty=true;}
        public EssentiaTransport neighbor(Direction side){return new EssentiaTransport(){
            public boolean connectable(Direction s){return true;} public boolean canInput(Direction s){return false;}public boolean canOutput(Direction s){return true;}
            public Aspect suctionType(Direction s){return null;}public int suction(Direction s){return 0;}public int minimumSuction(){return 0;}
            public Aspect essentiaType(Direction s){return null;}public int essentiaAmount(Direction s){return (int)Math.min(Integer.MAX_VALUE,MountedArcaneStorage.of(context.contraption).contents().total());}
            public int takeEssentia(Aspect a,int n,Direction s){return MountedArcaneStorage.of(context.contraption).extract(a,n,false);}
            public int addEssentia(Aspect a,int n,Direction s){return 0;}
        };}
        void tick(ServerLevel level){
            fuel.tick();var origin=BlockPos.containing(context.position);
            if(fertility){if(fuel.charges()>1&&level.getGameTime()%300==0)breed(level,origin);}
            else if(fuel.charges()>0)grow(level,origin);
            if(dirty){fuel.save(context.blockEntityData);ArcaneMovement.sync(context);dirty=false;}
            if(context.state.getValue(ArcaneLampBlock.ACTIVE)!=(fuel.charges()>0))
                ArcaneMovement.state(context,context.state.setValue(ArcaneLampBlock.ACTIVE,fuel.charges()>0));
        }
        void grow(ServerLevel level,BlockPos origin){
            if(!origin.equals(last)){columns.clear();last=origin;}
            if(columns.isEmpty()){
                for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)columns.add(origin.offset(x,6,z));
                for(int n=columns.size();n>1;n--)Collections.swap(columns,n-1,level.random.nextInt(n));
            }
            var column=columns.remove(columns.size()-1);
            for(int y=Math.min(column.getY(),level.getMaxBuildHeight()-1);y>=Math.max(origin.getY()-6,level.getMinBuildHeight());y--){
                var pos=new BlockPos(column.getX(),y,column.getZ());
                if(!level.hasChunkAt(pos)||origin.distSqr(pos)>=36||!LampPlantGrowth.candidate(level,pos))continue;
                fuel.consume(1);LampPlantGrowth.schedule(level,pos,level.getBlockState(pos));
                tc4revival.network.FocusEffectPacket.blockSparkle(level,pos,0x40ff40);return;
            }
        }
        void breed(ServerLevel level,BlockPos origin){
            var animals=level.getEntitiesOfClass(Animal.class,new AABB(origin).inflate(7));
            for(var first:animals){
                if(first.getAge()!=0||first.isInLove())continue;
                var same=animals.stream().filter(a->a.getClass()==first.getClass()).toList();if(same.size()>7)continue;
                for(var second:same)if(second!=first&&second.getAge()==0&&!second.isInLove()){
                    fuel.consume(2);first.setInLove(null);second.setInLove(null);return;
                }
            }
        }
    }
}

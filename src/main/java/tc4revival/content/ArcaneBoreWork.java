package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import tc4revival.aspect.*;

/** Server work loop; the block entity supplies its live base and synchronized inventory. */
public  class ArcaneBoreWork {
    public record Base(BlockPos pos,Direction output,ArcaneBoreBaseTransport transport) {}
    public interface Host {
        ServerLevel level();
        BlockPos pos();
        Direction facing();
        Base base();
        ItemStack focus();
        ItemStack pick();
        ServerPlayer worker();
        void changed();
        void targetSelected(BlockPos target);
        void blockBroken(BlockPos target,int stateId);
    }
    private final Host host;
    private final ArcaneBoreScan scan=new ArcaneBoreScan();
    private final ArcaneBoreRepair repair=new ArcaneBoreRepair();
    private final ArcaneBoreRotation rotation=new ArcaneBoreRotation();
    private BlockPos target;
    private int countdown;
    private float acceleration;
    public ArcaneBoreWork(Host host) { this.host=host; }
    public float acceleration() { return acceleration; }
    public ArcaneBoreRotation rotation() { return rotation; }
    public void orient(boolean initial) { rotation.face(host.facing(),initial); scan.reorient(); target=null; }
    public void load(CompoundTag tag) { acceleration=tag.getShort("SpeedyTime"); orient(true); }
    public void save(CompoundTag tag) { tag.putShort("SpeedyTime",(short)acceleration); }
    public void tick() {
        var level=host.level(); var pos=host.pos(); var base=host.base();
        if (acceleration<20) {
            float previous=acceleration;
            acceleration+=VisNetwork.drain(level,pos,TCAspects.get(LegacyAspect.ENTROPY),100)/5.0f;
            if (acceleration<20 && base!=null && base.transport().draw()) acceleration+=20;
            if (acceleration!=previous) host.changed();
        }
        rotation.tick(); var equipment=ArcaneBoreEquipment.inspect(host.focus(),host.pick());
        boolean powered=level.hasNeighborSignal(pos) || base!=null && level.hasNeighborSignal(base.pos());
        if (base!=null && powered && equipment.canDig(host.pick()) && rotation.settled() && --countdown<=0) {
            boolean dug=false;
            if (target!=null) {
                var previousTarget=target; target=null;
                if (level.hasChunkAt(previousTarget) && !level.isOutsideBuildHeight(previousTarget)) {
                    var state=level.getBlockState(previousTarget);
                    // Do not let a replaced target bypass the negative-hardness scan guard.
                    if (state.getDestroySpeed(level,previousTarget)>=0) {
                        if (!state.isAir()) {
                            var drops=ArcaneBoreOutput.collect(level,previousTarget,host.worker(),equipment);
                            host.blockBroken(previousTarget,Block.getId(state));
                            ArcaneBoreOutput.deliver(level,pos,base.pos(),base.output(),drops);
                        }
                        ArcaneBoreOutput.damagePick(host.pick(),level);
                        level.setBlock(previousTarget,Blocks.AIR.defaultBlockState(),3);
                        illuminateTunnel(base); host.changed(); dug=true;
                    }
                }
            }
            var next=scan.find(level,pos,host.facing(),equipment.area(),equipment.speed(),acceleration);
            if (next!=null) { target=next.pos(); countdown=next.delay(); host.targetSelected(target); }
            if (dug && acceleration>0) { acceleration-=1; host.changed(); }
        }
        repair.tick(level,pos,host.pick(),host.worker(),host::changed);
    }
    private void illuminateTunnel(Base base) {
        var level=host.level(); var pos=host.pos(); var facing=host.facing();
        for (var side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            var lamp=base.pos().relative(side);
            if (!level.hasChunkAt(lamp) || !level.getBlockState(lamp).is(TCArcane.ARCANE_LAMP.get())) continue;
            int distance=level.random.nextInt(32)*2,phase=distance/2%4;
            var light=pos.relative(facing,1+distance);
            int offset=phase==0?3:phase==2?-3:0;
            light=facing.getStepX()!=0?light.offset(0,0,offset):light.offset(offset,0,0);
            if (phase==3 && facing.getStepY()==0) light=light.below(2);
            if (!level.isOutsideBuildHeight(light) && level.hasChunkAt(light) && level.isEmptyBlock(light)
                    && !level.getBlockState(light).is(TCArcane.LAMP_AIR.get()) && level.getMaxLocalRawBrightness(light)<15)
                level.setBlock(light,TCArcane.LAMP_AIR.get().defaultBlockState(),3);
            break;
        }
    }
}

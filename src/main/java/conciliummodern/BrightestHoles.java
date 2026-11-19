package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import tc4revival.content.PortableHoleBlockEntity;

/** Boss-owned tunnel fronts persist without pretending a FakePlayer is online. */
final class BrightestHoles {
    private static final String KEY="thaumicconcilium:hole_fronts";
    static void begin(BrightestOne boss,BlockPos pos){
        var jobs=boss.getPersistentData().getList(KEY,Tag.TAG_COMPOUND);if(jobs.size()>=32)return;
        var job=new CompoundTag();job.putLong("Pos",pos.asLong());job.putInt("Remaining",33);jobs.add(job);boss.getPersistentData().put(KEY,jobs);
    }
    static void tick(BrightestOne boss){
        if(!(boss.level() instanceof ServerLevel level))return;
        var jobs=boss.getPersistentData().getList(KEY,Tag.TAG_COMPOUND);
        if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,boss)){jobs.clear();return;}
        var actor=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        for(int i=jobs.size()-1;i>=0;i--){var job=jobs.getCompound(i);var pos=BlockPos.of(job.getLong("Pos"));int remaining=job.getInt("Remaining");
            if(remaining<=0 || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !PortableHoleBlockEntity.open(actor,pos,1,1,120)){jobs.remove(i);continue;}
            if(remaining>1)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                if(x==0 && z==0)continue;var side=pos.offset(x,0,z);
                if(level.hasChunkAt(side) && level.getWorldBorder().isWithinBounds(side))PortableHoleBlockEntity.open(actor,side,-1,1,120);
            }
            if(remaining==1)jobs.remove(i);else{job.putLong("Pos",pos.below().asLong());job.putInt("Remaining",remaining-1);}
        }
        boss.getPersistentData().put(KEY,jobs);
    }
    private BrightestHoles(){}
}

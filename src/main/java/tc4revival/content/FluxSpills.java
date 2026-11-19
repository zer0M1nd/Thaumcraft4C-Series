package tc4revival.content;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;

/** Deferred exceptional spills survive breaking the crucible and unloading its chunk. */
public  class FluxSpills extends SavedData {
    private final LinkedHashMap<BlockPos,Long> pending=new LinkedHashMap<>();
    private static FluxSpills get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FluxSpills::load,FluxSpills::new,"thaumcraft_flux_spills"); }
    public static void spill(ServerLevel level,BlockPos origin) {
        spill(level,origin,level.random);
    }
    static void spill(ServerLevel level,BlockPos origin,net.minecraft.util.RandomSource random) {
        if (random.nextInt(4)!=0) return;
        var pos=origin.above(); if (!loaded(level,pos)) return;
        var state=level.getBlockState(pos);
        if (state.isAir()) place(level,pos,random);
        else if ((state.is(TCArcane.FLUX_GAS.get()) || state.is(TCArcane.FLUX_GOO.get())) && state.getValue(FiniteFluidBlock.QUANTA)<8)
            level.setBlock(pos,state.setValue(FiniteFluidBlock.QUANTA,state.getValue(FiniteFluidBlock.QUANTA)+1),3);
        else {
            pos=origin.offset(random.nextInt(3)-1,random.nextInt(3)-1,random.nextInt(3)-1);
            if (loaded(level,pos) && level.isEmptyBlock(pos)) place(level,pos,random);
        }
    }
    private static boolean loaded(ServerLevel level,BlockPos pos) { return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos); }
    private static void place(ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        level.setBlock(pos,(random.nextBoolean() ? TCArcane.FLUX_GAS.get() : TCArcane.FLUX_GOO.get()).defaultBlockState(),3);
    }
    public static void empty(ServerLevel level,BlockPos origin,long attempts) {
        int now=(int)Math.min(256,Math.max(0,attempts));
        for (int i=0;i<now;i++) spill(level,origin);
        long remaining=attempts-now;
        if (remaining>0) { var data=get(level); data.pending.merge(origin.immutable(),remaining,Math::addExact); data.setDirty(); }
    }
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var data=get(level); if (data.pending.isEmpty()) return;
        int budget=256, positions=Math.min(64,data.pending.size());
        while (positions-->0 && budget>0) {
            var entry=data.pending.entrySet().iterator().next(); var pos=entry.getKey(); long remaining=entry.getValue(); data.pending.remove(pos);
            if (level.hasChunkAt(pos)) {
                int count=(int)Math.min(remaining,budget);
                for (int i=0;i<count;i++) spill(level,pos);
                remaining-=count; budget-=count;
            }
            if (remaining>0) data.pending.put(pos,remaining);
        }
        data.setDirty();
    }
    static FluxSpills load(CompoundTag tag) {
        var data=new FluxSpills();
        for (var value : tag.getList("Pending",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)value; long remaining=entry.getLong("Attempts");
            if (remaining>0) data.pending.put(BlockPos.of(entry.getLong("Position")),remaining);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list=new ListTag(); pending.forEach((pos,count)->{ var entry=new CompoundTag(); entry.putLong("Position",pos.asLong()); entry.putLong("Attempts",count); list.add(entry); });
        tag.put("Pending",list); return tag;
    }
}

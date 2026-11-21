package tc4revival.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

/** Persistent biome edits for taint, pure nodes and other old column-based biome effects. */
public  class WorldBiomeChanges {
    private WorldBiomeChanges() {}
    /** Keep node effects on their side of the terrain surface; cave effects stay local in Y. */
    public static boolean changeNodeBiome(ServerLevel level,BlockPos origin,BlockPos target,ResourceKey<Biome> biome) {
        if(!level.hasChunkAt(origin)||!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(target))return false;
        int sourceSurface=surfaceQuart(level,origin),targetSurface=surfaceQuart(level,target);
        int sourceY=QuartPos.fromBlock(origin.getY());boolean above=sourceY>=sourceSurface;
        var chunk=level.getChunkSource().getChunkNow(target.getX()>>4,target.getZ()>>4);if(chunk==null)return false;
        var replacement=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome);
        int qx=QuartPos.fromBlock(target.getX())&3,qz=QuartPos.fromBlock(target.getZ())&3;
        boolean changed=false;var sampler=level.getChunkSource().randomState().sampler();
        for(int si=0;si<chunk.getSections().length;si++) {
            var section=chunk.getSections()[si];int startY=QuartPos.fromSection(chunk.getSectionYFromSectionIndex(si));
            var before=new ArrayList<Holder<Biome>>(64);boolean edit=false;
            for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++) {
                var old=section.getNoiseBiome(x,y,z);before.add(old);
                if(x==qx&&z==qz&&nodeQuartAllowed(above,sourceY,targetSurface,startY+y)&&!old.is(biome))edit=true;
            }
            if(!edit)continue;changed=true;
            section.fillBiomesFromNoise((x,y,z,noise)->(x&3)==qx&&(z&3)==qz&&nodeQuartAllowed(above,sourceY,targetSurface,y)
                    ?replacement:before.get((y&3)*16+(z&3)*4+(x&3)),sampler,
                    QuartPos.fromBlock(chunk.getPos().getMinBlockX()),startY,QuartPos.fromBlock(chunk.getPos().getMinBlockZ()));
        }
        if(changed){chunk.setUnsaved(true);level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));}return changed;
    }
    static boolean nodeQuartAllowed(boolean above,int sourceY,int surfaceY,int y) {
        return above?y>=surfaceY:y<surfaceY&&Math.abs(y-sourceY)<=2;
    }
    private static int surfaceQuart(ServerLevel level,BlockPos pos) {
        int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())-1;
        while(y>level.getMinBuildHeight()) {
            var state=level.getBlockState(new BlockPos(pos.getX(),y,pos.getZ()));
            if(!state.is(net.minecraft.tags.BlockTags.LOGS)&&!state.is(net.minecraft.tags.BlockTags.LEAVES)
                    &&(state.blocksMotion()||!state.getFluidState().isEmpty()))break;
            y--;
        }
        return QuartPos.fromBlock(y);
    }

    /**
     * Modern biome storage has 4x4 horizontal cells. Change the entire vertical cell column,
     * keeping every other cell and using the vanilla biome packet to invalidate client colours.
     * This operation deliberately does not load or generate chunks.
     */
    public static int changeColumns(ServerLevel level,Collection<BlockPos> columns,ResourceKey<Biome> target) {
        Holder<Biome> replacement=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(target);
        var edits=new LinkedHashMap<Long,ColumnEdits>();
        for (BlockPos pos:columns) {
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if (chunk==null) continue;
            var entry=edits.computeIfAbsent(chunk.getPos().toLong(),key->new ColumnEdits(chunk));
            entry.mask|=1<<((QuartPos.fromBlock(pos.getZ())&3)*4+(QuartPos.fromBlock(pos.getX())&3));
        }
        List<ChunkAccess> changed=new ArrayList<>();
        int count=0;
        var sampler=level.getChunkSource().randomState().sampler();
        for (var edit:edits.values()) {
            int changedMask=0;
            var chunk=edit.chunk;
            int startX=QuartPos.fromBlock(chunk.getPos().getMinBlockX());
            int startZ=QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
            for (int sectionIndex=0;sectionIndex<chunk.getSections().length;sectionIndex++) {
                var section=chunk.getSections()[sectionIndex];
                int sectionChanges=0;
                // Snapshot before rewriting: the resolver must not observe its own writes.
                List<Holder<Biome>> before=new ArrayList<>(64);
                for (int y=0;y<4;y++) for (int z=0;z<4;z++) for (int x=0;x<4;x++) {
                    var old=section.getNoiseBiome(x,y,z);
                    before.add(old);
                    int bit=1<<(z*4+x);
                    if ((edit.mask&bit)!=0 && !old.is(target)) sectionChanges|=bit;
                }
                if (sectionChanges==0) continue;
                changedMask|=sectionChanges;
                int startY=QuartPos.fromSection(chunk.getSectionYFromSectionIndex(sectionIndex));
                section.fillBiomesFromNoise((x,y,z,noise)->(edit.mask&(1<<((z&3)*4+(x&3))))!=0
                                ? replacement : before.get((y&3)*16+(z&3)*4+(x&3)),
                        sampler,startX,startY,startZ);
            }
            if (changedMask==0) continue;
            chunk.setUnsaved(true); changed.add(chunk); count+=Integer.bitCount(changedMask);
        }
        if (!changed.isEmpty()) level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
        return count;
    }
    public static boolean changeColumn(ServerLevel level,BlockPos pos,ResourceKey<Biome> target) {
        return changeColumns(level,List.of(pos),target)>0;
    }
    private static  class ColumnEdits {
        final LevelChunk chunk;
        int mask;
        ColumnEdits(LevelChunk chunk) { this.chunk=chunk; }
    }
}

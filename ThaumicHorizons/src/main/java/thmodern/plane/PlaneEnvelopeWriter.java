package thmodern.plane;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
/** Biomes are filled per chunk; shells are written with a block-count budget. */
public final class PlaneEnvelopeWriter {
 private final PocketPlanes.Plane plane;private final List<List<PlaneShell.Point>> rings=new ArrayList<>();private final List<BlockState> materials=new ArrayList<>();private final List<PlaneShell.Point> boundary;private final int minX,maxX,minZ,maxZ;
 public PlaneEnvelopeWriter(PocketPlanes.Plane plane){this.plane=plane;int radius=plane.parameters().radius(),index=0;for(var shell:plane.parameters().shells()){
  rings.add(PlaneShell.points(radius-++index));String id=switch(shell){case "eldritch"->"thaumcraft:eldritch_stone";default->"minecraft:"+shell;};var key=ResourceLocation.parse(id);if(!ForgeRegistries.BLOCKS.containsKey(key))throw new IllegalStateException("Missing shell material "+id);materials.add(ForgeRegistries.BLOCKS.getValue(key).defaultBlockState());
 }boundary=PlaneShell.points(radius);minX=Math.floorDiv(-radius,16);maxX=Math.floorDiv(radius,16);minZ=Math.floorDiv(plane.center().getZ()-radius,16);maxZ=Math.floorDiv(plane.center().getZ()+radius,16);}
 private void check(ServerLevel level){if(!level.dimension().location().equals(PocketPlanes.DIMENSION))throw new IllegalArgumentException("Envelope outside pocket dimension");}
 public int biomeCount(){return (maxX-minX+1)*(maxZ-minZ+1);}public long ringCount(){return rings.stream().mapToLong(List::size).sum();}public int boundaryCount(){return boundary.size();}
 public void biome(ServerLevel level,int cursor){check(level);if(cursor<0||cursor>=biomeCount())throw new IllegalArgumentException("Invalid biome cursor");int zCount=maxZ-minZ+1;var chunk=level.getChunk(minX+cursor/zCount,minZ+cursor%zCount);var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse(plane.parameters().biome())));
  chunk.fillBiomesFromNoise((x,y,z,sampler)->biome,Climate.empty());chunk.setUnsaved(true);level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
 }
 public long rings(ServerLevel level,long cursor,int budget){check(level);if(cursor<0||cursor>ringCount()||budget<1)throw new IllegalArgumentException("Invalid shell budget/cursor");long end=Math.min(ringCount(),cursor+budget),base=0;for(int i=0;i<rings.size();i++){var points=rings.get(i);for(long j=Math.max(0,cursor-base);j<points.size()&&base+j<end;j++)put(level,points.get((int)j),materials.get(i));base+=points.size();}return end;}
 public long boundary(ServerLevel level,long cursor,int budget){check(level);if(cursor<0||cursor>boundaryCount()||budget<1)throw new IllegalArgumentException("Invalid boundary budget/cursor");long end=Math.min(boundaryCount(),cursor+budget);for(long i=cursor;i<end;i++)put(level,boundary.get((int)i),PlanarBlocks.VOID.get().defaultBlockState());return end;}
 private void put(ServerLevel level,PlaneShell.Point point,BlockState block){var at=plane.center().offset(point.x(),point.y(),point.z());if(level.isOutsideBuildHeight(at))throw new IllegalStateException("Plane envelope exceeds world height");level.setBlock(at,block,Block.UPDATE_CLIENTS);}
}

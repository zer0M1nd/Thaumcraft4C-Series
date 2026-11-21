package thmodern.plane;

import java.util.*;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded deterministic layer writes. The persisted cursor advances only after a complete column. */
public final class PlaneStrataWriter {
 private final PocketPlanes.Plane plane;
 private final PlaneSurfaceWriter surfaces;
 private final List<PlaneLayers.Layer> layers;
 private final List<BlockState> materials;
 private final List<PerlinNoise> noises;
 private final int radius,width;
 private final long columns;
 public PlaneStrataWriter(PocketPlanes.Plane plane){
  this.plane=plane;surfaces=new PlaneSurfaceWriter(plane);radius=plane.parameters().radius();width=radius*2+1;columns=(long)width*width;layers=PlaneLayers.of(plane.parameters());
  var blocks=new ArrayList<BlockState>();var noise=new ArrayList<PerlinNoise>();
  for(int index=0;index<layers.size();index++){
   var id=ResourceLocation.parse(layers.get(index).block());
   if(!ForgeRegistries.BLOCKS.containsKey(id))throw new IllegalStateException("Missing planar stratum "+id);
   blocks.add(ForgeRegistries.BLOCKS.getValue(id).defaultBlockState());
   noise.add(PerlinNoise.create(RandomSource.create(plane.seed()^((long)index*0x9E3779B97F4A7C15L)),IntStream.rangeClosed(-9,0)));
  }
  materials=List.copyOf(blocks);noises=List.copyOf(noise);
 }
 public long size(){return columns*layers.size();}
 public record Surface(BlockPos pos,PlaneLayers.Layer layer,int index){}
 public Optional<Surface> surface(long cursor){
  if(cursor<0||cursor>=size())return Optional.empty();int index=(int)(cursor/columns);long cell=cursor%columns;
  int x=(int)(cell/width)-radius,z=(int)(cell%width)-radius;var layer=layers.get(index);
  // Modern seeded Perlin replaces 1.7's octave implementation; exact terrain noise is not a save compatibility contract.
  double scale=layer.noise()/25d;
  int offset=layer.noise()==0?0:(int)(noises.get(index).getValue(x*scale,layer.height()*scale/2,z*scale)*64);
  int bottom=PlaneLayers.bottom(radius,x,z),top=PlaneLayers.top(radius,x,z,layer.height()+offset);
  return top<=bottom?Optional.empty():Optional.of(new Surface(new BlockPos(x,top,plane.center().getZ()+z),layer,index));
 }
 /** Returns the next cursor; caller checkpoints it in PocketPlanes before the next server tick. */
 public long write(ServerLevel level,long cursor,int columnBudget){
  if(cursor<0||cursor>size()||columnBudget<1)throw new IllegalArgumentException("Invalid stratum generation budget/progress");
  if(!level.dimension().location().equals(PocketPlanes.DIMENSION))throw new IllegalArgumentException("Refusing planar generation outside pocket dimension");
  long end=Math.min(size(),cursor+columnBudget);
  for(;cursor<end;cursor++){
   var surface=surface(cursor);if(surface.isEmpty())continue;var top=surface.get();int x=top.pos().getX(),z=top.pos().getZ();
   int bottom=PlaneLayers.bottom(radius,x,z-plane.center().getZ());var mutable=new BlockPos.MutableBlockPos();
   for(int y=bottom;y<=top.pos().getY();y++)level.setBlock(mutable.set(x,y,z),materials.get(top.index()),Block.UPDATE_CLIENTS);
   surfaces.decorate(level,top,cursor);
  }
  return cursor;
 }
}

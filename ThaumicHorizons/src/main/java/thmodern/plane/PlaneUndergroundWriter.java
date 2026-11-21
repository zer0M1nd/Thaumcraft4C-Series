package thmodern.plane;
import java.util.*;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraftforge.registries.ForgeRegistries;

/** Each cave/deposit is independently seeded so a checkpoint never depends on a mutable world's RNG. */
public final class PlaneUndergroundWriter {
 private final PocketPlanes.Plane plane;private final PlaneParameters parameters;private final int radius;
 private final List<String> pockets;
 public PlaneUndergroundWriter(PocketPlanes.Plane plane){this.plane=plane;parameters=plane.parameters();radius=parameters.radius();var materials=new ArrayList<String>();String previous="minecraft:air";
  for(int i=0;i<radius*radius*radius/200;i++){var random=random(i,0x706f636b6574L);var aspect=PlaneDeposits.select(parameters,PlaneDeposits.POCKETS,random.nextDouble());if(aspect.isPresent())previous=PlaneDeposits.pocket(aspect.get(),random);materials.add(previous);}pockets=List.copyOf(materials);
 }
 public int caveCount(){return PlaneLayers.caveCount(parameters);}public int pocketCount(){return pockets.size();}
 private Random random(long index,long salt){return new Random(plane.seed()^salt^(index*0x9E3779B97F4A7C15L));}
 private boolean inside(BlockPos pos){long x=pos.getX(),y=pos.getY()-128,z=pos.getZ()-plane.center().getZ();return pos.getY()>0&&pos.getY()<256&&x*x+y*y+z*z<radius*radius;}
 private static BlockState material(String key){var id=ResourceLocation.parse(key);if(!ForgeRegistries.BLOCKS.containsKey(id))throw new IllegalStateException("Missing planar deposit "+id);return ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();}
 private void check(ServerLevel level,int index,int count){if(!level.dimension().location().equals(PocketPlanes.DIMENSION)||index<0||index>=count)throw new IllegalArgumentException("Invalid underground world or cursor");}
 public void writePocket(ServerLevel level,int index){
  check(level,index,pocketCount());var random=random(index,0x706f636b657447L);int xs=1+random.nextInt(3),zs=1+random.nextInt(3),ox=random.nextInt(radius*2)-radius-xs,oy=128-random.nextInt(radius),oz=plane.center().getZ()+random.nextInt(radius*2)-radius-zs;
  var noise=PerlinNoise.create(RandomSource.create(random.nextLong()),IntStream.rangeClosed(-9,0));double scale=(int)parameters.noise()/50d;var state=material(pockets.get(index));
  for(int x=0;x<xs;x++)for(int z=0;z<zs;z++){int offset=(int)(noise.getValue((ox+x)*scale,oy*scale*2,(oz+z)*scale)*32);
   for(int y=offset-1;y<offset+1;y++){var pos=new BlockPos(ox+x,oy+y,oz+z);if(!inside(pos))continue;var before=level.getBlockState(pos);if(!before.isAir()&&!before.getFluidState().is(FluidTags.WATER))level.setBlock(pos,state,Block.UPDATE_CLIENTS);}
  }
 }
 public void writeCave(ServerLevel level,int index){
  check(level,index,caveCount());var random=random(index,0x63617665L);int xs=10+random.nextInt(10),zs=10+random.nextInt(10),ox=random.nextInt((int)(radius*1.5))-(int)(radius*.75)-xs,oy=128-random.nextInt(radius/2),oz=plane.center().getZ()+random.nextInt((int)(radius*1.5))-(int)(radius*.75)-zs;
  var noise=PerlinNoise.create(RandomSource.create(random.nextLong()),IntStream.rangeClosed(-9,0));var samples=new double[xs*zs*16];double sum=0,scale=(int)parameters.noise()/10d;
  for(int x=0;x<xs;x++)for(int z=0;z<zs;z++)for(int y=0;y<16;y++){double sample=noise.getValue((ox+x)*scale,(oy+y)*scale*.4,(oz+z)*scale);samples[(x*zs+z)*16+y]=sample;sum+=sample;}double average=sum/samples.length;
  for(int x=0;x<xs;x++)for(int z=0;z<zs;z++)for(int y=0;y<16;y++){
   if(samples[(x*zs+z)*16+y]<=average)continue;var pos=new BlockPos(ox+x,oy+y,oz+z);if(!inside(pos)||level.getBlockState(pos).getFluidState().is(FluidTags.WATER))continue;
   if(!level.isEmptyBlock(pos.below())&&random.nextInt(50)==49){var aspect=PlaneDeposits.select(parameters,PlaneDeposits.CAVES,random.nextDouble());if(aspect.isPresent()&&!level.isEmptyBlock(pos))level.setBlock(pos,material(PlaneDeposits.cave(aspect.get(),random)),Block.UPDATE_CLIENTS);}
   else level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
  }
 }
}

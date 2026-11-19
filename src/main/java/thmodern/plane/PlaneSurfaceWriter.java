package thmodern.plane;
import java.util.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import thmodern.creature.PlanarSlimes;

/** Runs immediately after each layer column, preserving the original overwrite order. */
public final class PlaneSurfaceWriter {
 private final PocketPlanes.Plane plane;private final PlaneParameters p;
 public PlaneSurfaceWriter(PocketPlanes.Plane plane){this.plane=plane;p=plane.parameters();}
 private BlockState block(String id){var key=ResourceLocation.parse(id);if(!ForgeRegistries.BLOCKS.containsKey(key))throw new IllegalStateException("Missing surface material "+id);return ForgeRegistries.BLOCKS.getValue(key).defaultBlockState();}
 private boolean inside(BlockPos at){long x=at.getX(),y=at.getY()-128,z=at.getZ()-plane.center().getZ();return at.getY()>0&&at.getY()<256&&x*x+y*y+z*z<p.radius()*p.radius();}
 private void put(ServerLevel level,BlockPos at,BlockState state){if(inside(at))level.setBlock(at,state,Block.UPDATE_CLIENTS);}
 private void plant(ServerLevel level,BlockPos at,BlockState state){if(level.isEmptyBlock(at)&&state.canSurvive(level,at))put(level,at,state);}
 private void spawn(ServerLevel level,BlockPos at,EntityType<?> type,long cursor,String key){
  if(!inside(at))return;var ledger=PlanePopulation.get(level.getServer());UUID slot=UUID.nameUUIDFromBytes((plane.allocation()+":"+cursor+":"+key).getBytes(StandardCharsets.UTF_8));
  if(ledger.contains(plane.id(),slot)||ledger.count(plane.id())>=100)return;
  if(level.getEntity(slot)!=null){ledger.record(plane.id(),slot);return;}
  var entity=type.create(level);if(!(entity instanceof Mob mob))return;mob.setUUID(slot);mob.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);mob.setPersistenceRequired();
  if(!level.noCollision(mob))return;if(level.addFreshEntity(mob))ledger.record(plane.id(),slot);
 }
 public void decorate(ServerLevel level,PlaneStrataWriter.Surface surface,long cursor){
  if(!level.dimension().location().equals(PocketPlanes.DIMENSION))throw new IllegalArgumentException("Surface outside pocket dimension");
  var at=surface.pos();var above=at.above();var layer=surface.layer();int life=layer.life();var random=new Random(plane.seed()^cursor*0x9E3779B97F4A7C15L^0x73757266616365L);
  boolean cold=layer.environment()&&level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).get(ResourceLocation.parse(p.biome())).getBaseTemperature()<.2;
  switch(layer.block()){
   case "minecraft:water"->{if(cold)put(level,at,Blocks.ICE.defaultBlockState());else{if((life>40||p.amount("bestia")>0)&&random.nextInt(100)>98)spawn(level,at,EntityType.SQUID,cursor,"squid");if(life>0&&random.nextInt(100)>98)plant(level,above,Blocks.LILY_PAD.defaultBlockState());}}
   case "minecraft:dirt"->{if(life>0&&level.isEmptyBlock(above)){put(level,at,Blocks.GRASS_BLOCK.defaultBlockState());if(life>=10){
    if((life>=20||p.amount("messis")>0||p.amount("meto")>0)&&random.nextInt(100)>97){int choice=random.nextInt(10);String item=List.of("carrots","potatoes","wheat","melon","pumpkin","sugar_cane","red_mushroom","brown_mushroom","thaumcraft:shimmerleaf","thaumcraft:vishroom").get(choice);if(choice<3)put(level,at,Blocks.FARMLAND.defaultBlockState());if(choice==6||choice==7)put(level,at,Blocks.MYCELIUM.defaultBlockState());plant(level,above,block(item.contains(":")?item:"minecraft:"+item));}
    else if((life>=15||p.amount("arbor")>0)&&random.nextInt(100)>97){int choice=random.nextInt(10);if(choice<6)plant(level,above,block("minecraft:"+List.of("oak","spruce","birch","jungle","acacia","dark_oak").get(choice)+"_sapling"));else if(choice==6&&(p.biome().equals("thaumcraft:magical_forest")||p.amount("praecantatio")>10||p.amount("auram")>5)&&random.nextInt(4)==0)plant(level,above,block("thaumcraft:silverwood_sapling"));else if(choice==7&&random.nextInt(3)==0)plant(level,above,block("thaumcraft:greatwood_sapling"));}
    else if((life>=10||p.amount("herba")>0)&&random.nextInt(100)>94){int choice=random.nextInt(18);plant(level,above,block("minecraft:"+(choice<16?List.of("poppy","blue_orchid","allium","azure_bluet","red_tulip","orange_tulip","white_tulip","pink_tulip","oxeye_daisy").get(Math.min(8,choice)):choice==16?"dead_bush":"grass")));}
   }}}
   case "minecraft:sand"->{if(life>10){if(life>=20&&random.nextInt(100)>98)plant(level,above,block("thaumcraft:cinderpearl"));else if(random.nextInt(100)>97)plant(level,above,Blocks.CACTUS.defaultBlockState());}}
   case "thaumcraft:flesh_block"->{if(random.nextInt(300)>298){boolean rotate=random.nextBoolean();for(int x=0;x<(rotate?3:2);x++)for(int z=0;z<(rotate?2:3);z++)for(int y=0;y<2;y++)put(level,at.offset(x,y,z),PlanarBlocks.BRAIN.get().defaultBlockState());}}
  }
  if(cold&&level.getBlockState(at).isSolidRender(level,at))plant(level,above,Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,2));
  if((life>=50||p.amount("bestia")>0)&&random.nextInt(100)>98)spawn(level,above,List.of(EntityType.COW,EntityType.PIG,EntityType.CHICKEN,EntityType.SHEEP,EntityType.HORSE,EntityType.OCELOT,EntityType.WOLF).get(random.nextInt(7)),cursor,"animal");
  if((p.amount("venenum")>0||p.amount("permutatio")>0||p.amount("metallum")>0||p.amount("machina")>0)&&random.nextInt(100)>98)spawn(level,above,PlanarSlimes.TYPES.get("mercurial").get(),cursor,"mercurial");
  if((p.amount("limus")>0||p.amount("corpus")>0||p.amount("fames")>0)&&random.nextInt(100)>98)spawn(level,above,PlanarSlimes.TYPES.get("meat").get(),cursor,"meat");
  if(p.amount("limus")>0&&random.nextInt(100)>98)spawn(level,above,EntityType.SLIME,cursor,"slime");
  if(p.amount("alienis")>0&&random.nextInt(200)>198){put(level,at,block("thaumcraft:obsidian_tile"));for(int y=1;y<4;y++)put(level,at.above(y),block("thaumcraft:obsidian_totem"));}
  if(p.amount("fabrico")>0&&random.nextInt(200)>198)put(level,above,block("thaumcraft:arcane_pedestal"));
  if(p.amount("vitium")>0){if(random.nextInt(100)>98)put(level,above,tc4revival.content.TaintFibreBlock.faces(block("thaumcraft:taint_fibres"),level,above));if(random.nextInt(200)>198)spawn(level,above,tc4revival.content.TCEntities.TAINTACLE.get(),cursor,"taintacle");if(random.nextInt(200)>198)spawn(level,above,tc4revival.content.TCEntities.TAINT_HIVE.get(),cursor,"hive");}
 }
}

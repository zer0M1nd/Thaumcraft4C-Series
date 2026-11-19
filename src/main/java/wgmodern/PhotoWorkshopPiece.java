package wgmodern;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.util.RandomSource;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.block.state.properties.*;import net.minecraft.world.level.chunk.ChunkGenerator;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;import net.minecraft.world.entity.*;import net.minecraft.world.entity.decoration.*;import net.minecraft.resources.ResourceLocation;
/** The original 7x7 two-storey photo workshop, including its three random photograph frames. */
public class PhotoWorkshopPiece extends StructurePiece {
 private BlockPos origin;private final Direction facing;private int frames;private boolean painting,chest;
 public PhotoWorkshopPiece(BlockPos origin,Direction facing){super(WGWorldgen.PIECE.get(),0,bounds(origin,facing));this.origin=origin;this.facing=facing;}
 public PhotoWorkshopPiece(StructurePieceSerializationContext c,CompoundTag tag){super(WGWorldgen.PIECE.get(),tag);origin=NbtUtils.readBlockPos(tag.getCompound("Origin"));facing=Direction.from2DDataValue(tag.getInt("Facing"));frames=tag.getInt("Frames");painting=tag.getBoolean("Painting");chest=tag.getBoolean("Chest");}
 @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){tag.put("Origin",NbtUtils.writeBlockPos(origin));tag.putInt("Facing",facing.get2DDataValue());tag.putInt("Frames",frames);tag.putBoolean("Painting",painting);tag.putBoolean("Chest",chest);}
 @Override public void move(int x,int y,int z){super.move(x,y,z);origin=origin.offset(x,y,z);}
 static BlockPos rotate(int x,int z,Direction face){return switch(face){case EAST->new BlockPos(-z,0,x);case SOUTH->new BlockPos(-x,0,-z);case WEST->new BlockPos(z,0,-x);default->new BlockPos(x,0,z);};}
 public BlockPos pos(int x,int y,int z){return origin.offset(rotate(x,z,facing)).above(y);}
 static BoundingBox bounds(BlockPos origin,Direction face){var end=origin.offset(rotate(6,6,face));return new BoundingBox(Math.min(origin.getX(),end.getX()),origin.getY(),Math.min(origin.getZ(),end.getZ()),Math.max(origin.getX(),end.getX()),origin.getY()+9,Math.max(origin.getZ(),end.getZ()));}
 private Rotation rotation(){return switch(facing){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};}
 private void put(WorldGenLevel level,BoundingBox clip,int x,int y,int z,BlockState state){if(chest&&x==1&&y==1&&z==5)return;var p=pos(x,y,z);if(clip.isInside(p))level.setBlock(p,state.rotate(rotation()),2);}
 private void box(WorldGenLevel l,BoundingBox clip,int x,int y,int z,int X,int Y,int Z,BlockState s){for(int a=x;a<=X;a++)for(int b=y;b<=Y;b++)for(int c=z;c<=Z;c++)put(l,clip,a,b,c,s);}
 @Override public synchronized void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
  box(l,clip,0,0,0,6,7,6,Blocks.BIRCH_PLANKS.defaultBlockState());box(l,clip,1,1,1,5,7,5,Blocks.AIR.defaultBlockState());for(int x:new int[]{0,6})for(int z:new int[]{0,6})box(l,clip,x,1,z,x,8,z,Blocks.OAK_LOG.defaultBlockState());
  for(int i=1;i<=5;i++){put(l,clip,i,4,0,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));put(l,clip,i,4,6,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));put(l,clip,0,4,i,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));put(l,clip,6,4,i,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));}
  var slab=Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP);box(l,clip,1,4,1,5,4,5,slab);box(l,clip,2,4,5,4,4,5,Blocks.AIR.defaultBlockState());put(l,clip,3,4,4,Blocks.AIR.defaultBlockState());box(l,clip,1,0,1,5,0,5,Blocks.ORANGE_WOOL.defaultBlockState());put(l,clip,3,0,0,Blocks.ORANGE_WOOL.defaultBlockState());var door=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH);put(l,clip,3,1,0,door);put(l,clip,3,2,0,door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
  for(int[] p:new int[][]{{1,2,0},{5,2,0},{0,2,1},{0,2,3},{0,2,5},{6,2,1},{6,2,3},{6,2,5},{2,6,0},{3,6,0},{4,6,0},{0,6,2},{0,6,4},{2,6,6},{4,6,6}})put(l,clip,p[0],p[1],p[2],Blocks.GLASS_PANE.defaultBlockState());
  put(l,clip,1,1,3,Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH).setValue(StairBlock.HALF,Half.TOP));put(l,clip,2,1,3,slab);put(l,clip,3,1,3,slab);put(l,clip,3,4,3,Blocks.GLOWSTONE.defaultBlockState());for(int i=0;i<4;i++)put(l,clip,4-i,1+i,5,Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST));
  for(int i=1;i<=5;i++){put(l,clip,0,8,i,Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));put(l,clip,6,8,i,Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST));put(l,clip,i,8,0,Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH));put(l,clip,i,8,6,Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));}box(l,clip,1,9,1,5,9,5,Blocks.BIRCH_SLAB.defaultBlockState());
  var chestPos=pos(1,1,5);if(clip.isInside(chestPos)&&!chest){put(l,clip,1,1,5,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.EAST));if(l.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be){be.setLootTable(new ResourceLocation("witchinggadgets","chests/photo_workshop"),random.nextLong());chest=true;}}
  // Worldgen must not query ServerLevel chunks through comparator updates or Painting.create/survives.
  // The structure supplies a known wall and a one-block painting; use the WorldGenLevel for insertion.
  // Repeated neighbouring-chunk callbacks must neither erase nor duplicate the displays.
  for(int i=0;i<3;i++){var p=pos(5,6,i+2);if((frames&(1<<i))==0&&clip.isInside(p)){var frame=new ItemFrame(l.getLevel(),p,rotation().rotate(Direction.WEST));frame.setItem(randomPhoto(l.getLevel(),random), false);if(l.addFreshEntity(frame))frames|=1<<i;}}
  var pp=pos(1,6,3);
  if(!painting&&clip.isInside(pp)){
   var value=new Painting(EntityType.PAINTING,l.getLevel());
   var ids=new String[]{"kebab","aztec","alban","aztec2","bomb","plant","wasteland"};
   var data=new CompoundTag();
   data.putString("variant","minecraft:"+ids[random.nextInt(ids.length)]);
   data.putByte("facing",(byte)rotation().rotate(Direction.EAST).get2DDataValue());
   data.putInt("TileX",pp.getX());data.putInt("TileY",pp.getY());data.putInt("TileZ",pp.getZ());
   value.setPos(pp.getX(),pp.getY(),pp.getZ());
   value.readAdditionalSaveData(data);
   painting=l.addFreshEntity(value);
  }
  for(int x=0;x<=6;x++)for(int z=0;z<=6;z++){for(int y:new int[]{2,6}){var glass=pos(x,y,z);if(clip.isInside(glass)&&l.getBlockState(glass).is(Blocks.GLASS_PANE))l.setBlock(glass,Block.updateFromNeighbourShapes(l.getBlockState(glass),l,glass),2);}var p=pos(x,10,z);if(p.getX()<clip.minX()||p.getX()>clip.maxX()||p.getZ()<clip.minZ()||p.getZ()>clip.maxZ())continue;while(p.getY()<l.getMaxBuildHeight()&&!l.isEmptyBlock(p)){l.setBlock(p,Blocks.AIR.defaultBlockState(),2);p=p.above();}p=pos(x,-1,z);while(p.getY()>l.getMinBuildHeight()&&(l.isEmptyBlock(p)||!l.getFluidState(p).isEmpty())){l.setBlock(p,Blocks.COBBLESTONE.defaultBlockState(),2);p=p.below();}}
 }
 public static String subject(int roll){if(roll<0||roll>=33)throw new IllegalArgumentException("Photo roll must be 0..32");return roll<4?"minecraft:zombie":roll<8?"minecraft:skeleton":roll<12?"minecraft:creeper":roll<16?"minecraft:spider":roll<19?"minecraft:cave_spider":roll<21?"minecraft:enderman":roll==21?"minecraft:wither_skeleton":roll<25?"minecraft:zombified_piglin":roll<27?"minecraft:slime":roll<29?"minecraft:blaze":roll<31?"thaumcraft:brainy_zombie":"thaumcraft:fire_bat";}
 public static net.minecraft.world.item.ItemStack randomPhoto(net.minecraft.server.level.ServerLevel level,RandomSource random){var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(subject(random.nextInt(33))));var entity=type==null?null:type.create(level);if(entity==null)return new net.minecraft.world.item.ItemStack(Content.PHOTO.get());return PhotoData.create(true,PhotoData.worldgenEntity(entity));}
}

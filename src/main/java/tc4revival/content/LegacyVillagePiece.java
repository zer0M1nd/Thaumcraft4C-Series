package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Procedural legacy rooms, with a separate full write bound for modern chunk scheduling. */
public  class LegacyVillagePiece extends StructurePiece {
    private BlockPos origin;
    private final Direction face;
    private final LegacyVillagerTrades.Role role;
    private final boolean lowRoof;
    private final int table;
    private boolean residentSpawned,chestFilled,desert;
    public static LegacyVillagePiece atEntrance(LegacyVillagerTrades.Role role,BlockPos entrance,Direction face,RandomSource random) {
        var delta=rotate(role==LegacyVillagerTrades.Role.WIZARD?3:1,role==LegacyVillagerTrades.Role.WIZARD?1:0,face);
        return new LegacyVillagePiece(role,entrance.offset(-delta.getX(),0,-delta.getZ()),face,random.nextBoolean(),random.nextInt(3));
    }
    private LegacyVillagePiece(LegacyVillagerTrades.Role role,BlockPos origin,Direction face,boolean lowRoof,int table) {
        super(LegacyVillageStructures.PIECE.get(),0,bounds(origin,face,0,role==LegacyVillagerTrades.Role.WIZARD?0:-1,role==LegacyVillagerTrades.Role.WIZARD?7:3,role==LegacyVillagerTrades.Role.WIZARD?11:4,role==LegacyVillagerTrades.Role.WIZARD?11:5));
        this.role=role;this.origin=origin;this.face=face;this.lowRoof=lowRoof;this.table=table;
    }
    public LegacyVillagePiece(StructurePieceSerializationContext context,CompoundTag tag) {
        super(LegacyVillageStructures.PIECE.get(),tag);origin=NbtUtils.readBlockPos(tag.getCompound("Origin"));
        role=LegacyVillagerTrades.Role.valueOf(tag.getString("Role")); face=Direction.from2DDataValue(tag.getInt("Face"));
        lowRoof=tag.getBoolean("LowRoof");table=Math.max(0,Math.min(2,tag.getInt("Table")));residentSpawned=tag.getBoolean("Resident");chestFilled=tag.getBoolean("Chest");desert=tag.getBoolean("Desert");
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.put("Origin",NbtUtils.writeBlockPos(origin));tag.putString("Role",role.name());tag.putInt("Face",face.get2DDataValue());
        tag.putBoolean("LowRoof",lowRoof);tag.putInt("Table",table);tag.putBoolean("Resident",residentSpawned);tag.putBoolean("Chest",chestFilled);tag.putBoolean("Desert",desert);
    }
    @Override public void move(int x,int y,int z) { super.move(x,y,z);origin=origin.offset(x,y,z); }
    private static BlockPos rotate(int x,int z,Direction face) {
        return switch(face) {case NORTH->new BlockPos(x,0,z);case EAST->new BlockPos(-z,0,x);case SOUTH->new BlockPos(-x,0,-z);case WEST->new BlockPos(z,0,-x);default->throw new IllegalArgumentException("Vertical village door");};
    }
    public BlockPos position(int x,int y,int z) { return origin.offset(rotate(x,z,face)).above(y); }
    private static BoundingBox bounds(BlockPos origin,Direction face,int minX,int minZ,int maxX,int maxZ,int height) {
        var a=origin.offset(rotate(minX,minZ,face));var b=origin.offset(rotate(maxX,maxZ,face));
        return new BoundingBox(Math.min(a.getX(),b.getX()),origin.getY(),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX()),origin.getY()+height,Math.max(a.getZ(),b.getZ()));
    }
    public BoundingBox collisionBounds() { return bounds(origin,face,0,0,role==LegacyVillagerTrades.Role.WIZARD?4:3,4,role==LegacyVillagerTrades.Role.WIZARD?11:5); }
    public LegacyVillagerTrades.Role role() { return role; }
    public BlockPos workstation() { return role==LegacyVillagerTrades.Role.WIZARD?position(2,1,2):position(2,1,3); }
    public void setDesert(boolean value) { desert=value; }
    private BlockState material(BlockState state) {
        if(!desert)return state;
        if(state.is(Blocks.OAK_LOG)||state.is(Blocks.COBBLESTONE)||state.is(Blocks.GRAVEL))return Blocks.SANDSTONE.defaultBlockState();
        if(state.is(Blocks.OAK_PLANKS))return Blocks.CUT_SANDSTONE.defaultBlockState();
        if(state.is(Blocks.COBBLESTONE_STAIRS)||state.is(Blocks.OAK_STAIRS))return Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,state.getValue(StairBlock.FACING)).setValue(StairBlock.HALF,state.getValue(StairBlock.HALF));
        return state;
    }
    private Rotation rotation() { return switch(face) {case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;}; }
    private void put(WorldGenLevel level,BoundingBox clip,int x,int y,int z,BlockState state) {
        putWorld(level,clip,x,y,z,state.rotate(rotation()));
    }
    private void putWorld(WorldGenLevel level,BoundingBox clip,int x,int y,int z,BlockState state) {
        var p=position(x,y,z);
        if (clip.isInside(p) && !level.isOutsideBuildHeight(p)) {
            state=material(state);
            if (state.getBlock() instanceof IronBarsBlock || state.getBlock() instanceof WallBlock) state=Block.updateFromNeighbourShapes(state,level,p);
            level.setBlock(p,state,2);
        }
    }
    private void box(WorldGenLevel level,BoundingBox clip,int x1,int y1,int z1,int x2,int y2,int z2,Block block) {
        for (int y=y1;y<=y2;y++)for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)put(level,clip,x,y,z,block.defaultBlockState());
    }
    @Override public synchronized void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        boolean tower=role==LegacyVillagerTrades.Role.WIZARD;
        if(tower) tower(level,clip,random); else bank(level,clip);
        int width=tower?5:4,depth=tower?12:5,height=tower?12:6;
        for(int x=0;x<width;x++)for(int z=0;z<depth;z++) {
            var p=position(x,height,z);
            if (p.getX()<clip.minX()||p.getX()>clip.maxX()||p.getZ()<clip.minZ()||p.getZ()>clip.maxZ())continue;
            while(p.getY()<level.getMaxBuildHeight()&&!level.isEmptyBlock(p)) {level.setBlock(p,Blocks.AIR.defaultBlockState(),2);p=p.above();}
            p=position(x,-1,z);
            while(p.getY()>level.getMinBuildHeight()&&(level.isEmptyBlock(p)||!level.getFluidState(p).isEmpty())) {level.setBlock(p,material(Blocks.COBBLESTONE.defaultBlockState()),2);p=p.below();}
        }
        put(level,clip,2,1,tower?2:3,(tower?TCArcane.BLOCK.get():TCArcane.WAND_PEDESTAL.get()).defaultBlockState());
        var spawn=position(tower?3:1,1,2);
        if(!residentSpawned && clip.isInside(spawn)) {
            var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(level.getLevel());
            villager.moveTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0);villager.setPersistenceRequired();
            villager.finalizeSpawn(level,level.getCurrentDifficultyAt(spawn),net.minecraft.world.entity.MobSpawnType.STRUCTURE,null,null);
            villager.getPersistentData().put("TC4VillageWorkstation",NbtUtils.writeBlockPos(workstation()));
            residentSpawned=level.addFreshEntity(villager);
        }
    }
    private void door(WorldGenLevel level,BoundingBox clip,int x,int z) {
        var door=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH);
        put(level,clip,x,1,z,door);put(level,clip,x,2,z,door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
        var step=position(x,0,z-1);
        if(clip.isInside(step)&&level.isEmptyBlock(step)&&!level.isEmptyBlock(step.below()))
            put(level,clip,x,0,z-1,Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH));
    }
    private void tower(WorldGenLevel level,BoundingBox clip,RandomSource random) {
        box(level,clip,2,1,2,4,11,4,Blocks.AIR);
        for(int y:new int[]{0,5,10}) {
            box(level,clip,2,y,2,4,y,4,Blocks.OAK_PLANKS);
            for(int x:new int[]{1,5})for(int z:new int[]{1,5})put(level,clip,x,y,z,Blocks.COBBLESTONE.defaultBlockState());
        }
        for(int side:new int[]{1,5}) {box(level,clip,side,0,2,side,11,4,Blocks.COBBLESTONE);box(level,clip,2,0,side,4,11,side,Blocks.COBBLESTONE);}
        for(int y:new int[]{7,8})for(int z:new int[]{1,5})put(level,clip,3,y,z,Blocks.GLASS_PANE.defaultBlockState());
        for(int y:new int[]{2,3})put(level,clip,3,y,5,Blocks.GLASS_PANE.defaultBlockState());
        for(int y=1;y<=9;y++)put(level,clip,4,y,3,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.WEST));
        int ladderMetadata=rotation().rotate(Direction.WEST).get3DDataValue();
        var hatchFace=switch(ladderMetadata&3){case 0->Direction.SOUTH;case 1->Direction.NORTH;case 2->Direction.EAST;default->Direction.WEST;};
        putWorld(level,clip,4,10,3,Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING,hatchFace).setValue(TrapDoorBlock.OPEN,(ladderMetadata&4)!=0));
        put(level,clip,3,5,3,Blocks.GLOWSTONE.defaultBlockState());
        var chest=position(2,6,2);
        if(clip.isInside(chest)) {
            put(level,clip,2,6,2,Blocks.CHEST.defaultBlockState());
            if(!chestFilled&&level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity inventory) {
                inventory.setLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:chests/wizard_tower"),random.nextLong());chestFilled=true;
            }
        }
        door(level,clip,3,1);
    }
    private void bank(WorldGenLevel level,BoundingBox clip) {
        box(level,clip,1,1,1,3,5,4,Blocks.AIR);box(level,clip,0,0,0,3,0,4,Blocks.COBBLESTONE);box(level,clip,1,0,1,2,0,3,Blocks.DIRT);
        box(level,clip,1,lowRoof?4:5,1,2,lowRoof?4:5,3,Blocks.OAK_LOG);
        for(int z:new int[]{0,4}) {box(level,clip,1,1,z,2,3,z,Blocks.OAK_PLANKS);box(level,clip,1,4,z,2,4,z,Blocks.OAK_LOG);}
        for(int x:new int[]{0,3}) {
            box(level,clip,x,1,1,x,3,3,Blocks.OAK_PLANKS);box(level,clip,x,4,1,x,4,3,Blocks.OAK_LOG);
            for(int z:new int[]{0,4})box(level,clip,x,1,z,x,3,z,Blocks.OAK_LOG);
            put(level,clip,x,2,2,Blocks.IRON_BARS.defaultBlockState());
        }
        if(table==1) {put(level,clip,table,1,3,Blocks.COBBLESTONE_WALL.defaultBlockState());put(level,clip,table,2,3,Blocks.STONE_PRESSURE_PLATE.defaultBlockState());}
        door(level,clip,1,0);
    }
}

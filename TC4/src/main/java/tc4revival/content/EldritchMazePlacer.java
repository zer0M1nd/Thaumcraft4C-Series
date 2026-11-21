package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/** Actual room blocks, local decoration queues and key-room encounters. */
public  class EldritchMazePlacer implements EldritchRoomGeometry.Environment {
    private final ServerLevel level;
    private final int originX,originZ;
    private final EldritchMazeLayout.Cell cell;
    private final List<BlockPos> urns=new ArrayList<>(),decorations=new ArrayList<>(),crabs=new ArrayList<>();
    private final Set<BlockPos> changed=new LinkedHashSet<>();
    private EldritchMazePlacer(ServerLevel level,int cx,int cz,EldritchMazeLayout.Cell cell) {
        this.level=level; originX=cx*16; originZ=cz*16; this.cell=cell;
    }
    public static void generate(ServerLevel level,int cx,int cz,EldritchMazeLayout.Cell cell) {
        if (!level.getServer().isSameThread() || !EldritchMazeSavedData.isMazeDimension(level.dimension())) throw new IllegalStateException("Wrong maze placement context");
        var sink=new EldritchMazePlacer(level,cx,cz,cell);
        var random=roomRandom(level.getSeed(),cx,cz);
        switch (cell.feature()) {
            case 1->EldritchRoomGeometry.entrance(sink,cell);
            case 2,3,4,5->EldritchRoomGeometry.bossQuarter(sink,cell);
            case 6->EldritchRoomGeometry.keyRoom(sink,cell);
            case 7->EldritchRoomGeometry.nest(sink,cell,random);
            case 8->EldritchRoomGeometry.library(sink,cell);
            default->{ random.nextInt(1); EldritchRoomGeometry.passage(sink,cell,random); }
        }
        sink.decorate(); sink.finishShapes();
        if (cell.feature()==6) sink.keyEncounter();
    }
    static Random roomRandom(long worldSeed,int cx,int cz) {
        // MazeHandler was called by Forge's world generator, not by the empty chunk provider.
        // The legacy GameRegistry implementation shifts each multiplier by three bits.
        var random=new Random(worldSeed); long x=random.nextLong()>>3,z=random.nextLong()>>3;
        random.setSeed((x*cx+z*cz)^worldSeed); return random;
    }
    private BlockPos position(int x,int y,int z) { return new BlockPos(originX+x,50+y,originZ+z); }
    private void write(BlockPos pos,BlockState state) {
        if (!level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) throw new IllegalArgumentException("Room outside world bounds");
        if (!level.getBlockState(pos).equals(state) && !level.setBlock(pos,state,18)) throw new IllegalStateException("Failed maze block write at "+pos);
        changed.add(pos.immutable());
    }
    @Override public void place(int x,int y,int z,EldritchRoomGeometry.Material material,Direction facing) {
        BlockPos pos=position(x,y,z); BlockState current=level.getBlockState(pos);
        if (material==EldritchRoomGeometry.Material.BEDROCK_IF_AIR && !current.isAir()) return;
        // This roll preceded the void-wall check in the old STONE case.
        if (material==EldritchRoomGeometry.Material.STONE && cell.feature()==7 && level.random.nextInt(3)==0)
            material=EldritchRoomGeometry.Material.CRUST;
        if ((material==EldritchRoomGeometry.Material.STONE || material==EldritchRoomGeometry.Material.CRUST
                || material==EldritchRoomGeometry.Material.ROCK || material==EldritchRoomGeometry.Material.NONSPAWNING_STONE
                || material==EldritchRoomGeometry.Material.TRAPPED_STONE) && current.is(TCEldritch.VOID.get())) return;
        if (material==EldritchRoomGeometry.Material.AIR || material==EldritchRoomGeometry.Material.DOOR_FRAME || material==EldritchRoomGeometry.Material.DOOR_LOCK) {
            urns.remove(pos); decorations.remove(pos); crabs.remove(pos);
        }
        BlockState state=switch(material) {
            case BEDROCK_IF_AIR->Blocks.BEDROCK.defaultBlockState();
            case STONE->stone(pos);
            case CRUST->crust(pos);
            case VOID->TCEldritch.VOID.get().defaultBlockState();
            case AIR->Blocks.AIR.defaultBlockState();
            case ROCK->TCEldritch.ROCK.get().defaultBlockState();
            case NONSPAWNING_STONE->TCEldritch.NONSPAWNING_STONE.get().defaultBlockState();
            case TRAPPED_STONE->TCEldritch.TRAP.get().defaultBlockState();
            case STAIR,URN_STAIR,INVERTED_STAIR->TCEldritch.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING,facing)
                    .setValue(StairBlock.HALF,material==EldritchRoomGeometry.Material.INVERTED_STAIR ? Half.TOP : Half.BOTTOM);
            case SLAB,UPPER_SLAB->TCEldritch.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,
                    material==EldritchRoomGeometry.Material.UPPER_SLAB ? SlabType.TOP : SlabType.BOTTOM);
            case ALTAR->TCEldritch.CAP.get().defaultBlockState();
            case PORTAL->TCEldritch.PORTAL.get().defaultBlockState();
            case OBELISK_BASE->TCEldritch.OBELISK.get().defaultBlockState();
            case OBELISK_SEGMENT->TCEldritch.OBELISK_SEGMENT.get().defaultBlockState();
            case PEDESTAL_STONE->TCEldritch.PEDESTAL_STONE.get().defaultBlockState();
            case GLYPHED_STONE->TCEldritch.GLYPHED_STONE.get().defaultBlockState();
            case GLOWING_STONE->TCEldritch.GLOWING_STONE.get().defaultBlockState();
            case CRYSTAL->TCEldritch.CRYSTAL.get().defaultBlockState().setValue(EldritchCrystalBlock.FACING,facing);
            case DOOR_FRAME->TCEldritch.DOOR_FRAME.get().defaultBlockState();
            case DOOR_LOCK->TCEldritch.LOCK.get().defaultBlockState().setValue(EldritchLockBlock.FACING,facing);
            case DOOR_BARRIER->TCEldritch.DOOR_BARRIER.get().defaultBlockState();
            case WEB->Blocks.COBWEB.defaultBlockState();
            case MIND_SPIDER_SPAWNER->Blocks.SPAWNER.defaultBlockState();
            case TAINT_FIBRES,TAINT_GRASS->TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState()
                    .setValue(TaintFibreBlock.VARIANT,material==EldritchRoomGeometry.Material.TAINT_GRASS ? 1 : 0),level,pos);
        };
        if (material==EldritchRoomGeometry.Material.URN_STAIR && level.random.nextFloat()<.005) urns.add(pos);
        write(pos,state);
        if (material==EldritchRoomGeometry.Material.MIND_SPIDER_SPAWNER && level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)
            spawner.setEntityId(TCEntities.MIND_SPIDER.get(),level.random);
    }
    private BlockState stone(BlockPos pos) {
        if (level.random.nextInt(25)==0) {
            boolean crab=cell.feature()==7 || level.random.nextInt(50)==0;
            if (crab && (cell.feature()==0 || cell.feature()==7)) crabs.add(pos); else decorations.add(pos);
        }
        return TCEldritch.STONE.get().defaultBlockState();
    }
    private BlockState crust(BlockPos pos) {
        if (level.random.nextInt(25)==0) return TCEldritch.GLOWING_STONE.get().defaultBlockState();
        if (level.random.nextInt(25)==0) {
            boolean crab=cell.feature()==7 || cell.feature()==12 && level.random.nextBoolean() || level.random.nextInt(25)==0;
            if (crab && (cell.feature()==0 || cell.feature()==7 || cell.feature()==12)) crabs.add(pos);
        }
        return TCEldritch.CRUST.get().defaultBlockState();
    }
    @Override public boolean isAir(int x,int y,int z) { return level.isEmptyBlock(position(x,y,z)); }
    @Override public boolean canCrustReplace(int x,int y,int z) {
        BlockState state=level.getBlockState(position(x,y,z));
        return state.isAir() || state.is(TCEldritch.STONE.get()) || state.is(TCEldritch.ROCK.get()) || state.is(TCEldritch.NONSPAWNING_STONE.get())
                || state.is(TCEldritch.CRUST.get()) || state.is(TCEldritch.PEDESTAL_STONE.get()) || state.is(TCEldritch.STAIRS.get());
    }
    @Override public boolean adjacentToSolid(int x,int y,int z) {
        BlockPos pos=position(x,y,z);
        for (Direction side:Direction.values()) { BlockPos neighbor=pos.relative(side); if (level.getBlockState(neighbor).isSolidRender(level,neighbor)) return true; }
        return false;
    }
    @Override public void taintBiome(int x,int z) { WorldBiomeChanges.changeColumn(level,position(x,0,z),TCBiomes.TAINTED_LAND); }
    @Override public void loot(int x,int y,int z,boolean crate,int rarity) { write(position(x,y,z),TCEldritch.loot(crate,rarity).defaultBlockState()); }
    private boolean adjacentDevice(BlockPos pos) {
        for (Direction side:Direction.values()) if (TCEldritch.isDevice(level.getBlockState(pos.relative(side)).getBlock())) return true;
        return false;
    }
    private int exposed(BlockPos pos) {
        int count=0; for (Direction side:Direction.values()) if (level.isEmptyBlock(pos.relative(side))) count++; return count;
    }
    private boolean bedrockShowing(BlockPos pos) {
        for (Direction side:Direction.values()) {
            BlockPos front=pos.relative(side),back=pos.relative(side.getOpposite()); BlockState behind=level.getBlockState(back);
            if (!level.getBlockState(front).isSolidRender(level,front) && (behind.is(Blocks.BEDROCK) || behind.is(TCEldritch.VOID.get()))) return true;
        }
        return false;
    }
    private void decorate() {
        for (BlockPos pos:urns) if (level.isEmptyBlock(pos.above())) {
            write(pos,TCEldritch.PEDESTAL_STONE.get().defaultBlockState()); float roll=level.random.nextFloat();
            write(pos.above(),TCEldritch.loot(false,roll<.025f ? 2 : roll<.1f ? 1 : 0).defaultBlockState());
        }
        for (BlockPos pos:decorations) {
            int count=exposed(pos); if (count==0 || count!=1 && bedrockShowing(pos) || adjacentDevice(pos)) continue;
            Block selected=level.random.nextInt(3)!=0 ? TCEldritch.GLOWING_STONE.get()
                    : level.random.nextInt(8)!=0 ? TCEldritch.GLYPHED_STONE.get() : TCEldritch.TRAP.get();
            write(pos,selected.defaultBlockState());
            if (selected==TCEldritch.GLOWING_STONE.get() && level.random.nextInt(12)==0)
                for (Direction side:Direction.values()) if (level.isEmptyBlock(pos.relative(side))) {
                    write(pos.relative(side),TCEldritch.CRYSTAL.get().defaultBlockState().setValue(EldritchCrystalBlock.FACING,side)); break;
                }
        }
        for (BlockPos pos:crabs) {
            if (exposed(pos)!=1 || adjacentDevice(pos)) continue;
            write(pos,TCEldritch.CRAB_SPAWNER.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof EldritchCrabSpawnerBlockEntity spawner)
                for (Direction side:Direction.values()) if (level.isEmptyBlock(pos.relative(side))) { spawner.setFacing(side); break; }
        }
    }
    private void finishShapes() {
        // A neighboring room's exposed void faces and stair corners also depend on this room.
        var affected=new LinkedHashSet<>(changed);
        for (BlockPos pos:changed) for (Direction side:Direction.values()) {
            BlockPos neighbor=pos.relative(side);
            if (!level.isOutsideBuildHeight(neighbor) && level.hasChunkAt(neighbor)) affected.add(neighbor);
        }
        for (BlockPos pos:affected) {
            BlockState state=level.getBlockState(pos),resolved=Block.updateFromNeighbourShapes(state,level,pos);
            if (!resolved.equals(state) && !level.setBlock(pos,resolved,18))
                throw new IllegalStateException("Failed maze shape update at "+pos);
        }
    }
    private void keyEncounter() {
        var encounter=new ArrayList<net.minecraft.world.entity.Entity>();
        var key=TCEntities.PERMANENT_ITEM.get().create(level); if (key==null) throw new IllegalStateException("Missing permanent item entity");
        key.setPos(originX+8.5,53.5,originZ+8.5); key.setItem(TCEldritch.RUNED_TABLET.get().getDefaultInstance()); key.setDeltaMovement(Vec3.ZERO);
        encounter.add(key);
        int count=2+(level.getDifficulty()==Difficulty.HARD ? 2 : level.getDifficulty()==Difficulty.NORMAL ? 1 : 0);
        for (int i=0;i<count;i++) {
            var guardian=TCEntities.ELDRITCH_GUARDIAN.get().create(level); if (guardian==null) throw new IllegalStateException("Missing guardian entity");
            guardian.setPos(originX+8.5+(1+level.random.nextInt(3))*(level.random.nextInt(3)-1),52,
                    originZ+8.5+(1+level.random.nextInt(3))*(level.random.nextInt(3)-1));
            guardian.finalizeSpawn(level,level.getCurrentDifficultyAt(guardian.blockPosition()),MobSpawnType.STRUCTURE,null,null);
            guardian.restrictTo(position(8,2,8),16); if (i==0 && count>=4) ChampionMobs.make(guardian,true);
            encounter.add(guardian);
        }
        try {
            for (var entity:encounter)
                if (!level.addFreshEntity(entity)) throw new IllegalStateException("Failed to place key-room encounter");
        } catch (RuntimeException failure) {
            // Failed room retries must not retain keys or a partially accepted set of guardians.
            for (var entity:encounter) entity.discard();
            throw failure;
        }
    }
}

package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.Aspect;

public  class ArcaneLampBlockEntity extends BlockEntity implements EssentiaTransport,ArcaneLampFuel.Host {
    private final ArcaneLampBlock.Kind kind;
    private final ArcaneLampFuel fuel;
    private final List<BlockPos> columns=new ArrayList<>();
    private int animalCounter;
    private BlockPos previous=BlockPos.ZERO;
    private BlockState previousState=Blocks.AIR.defaultBlockState();
    public ArcaneLampBlockEntity(BlockPos pos,BlockState state) {
        super(TCArcane.ARCANE_LAMP_ENTITY.get(),pos,state); kind=((ArcaneLampBlock)state.getBlock()).kind(); fuel=new ArcaneLampFuel(this,kind==ArcaneLampBlock.Kind.FERTILITY);
    }
    public ArcaneLampBlock.Kind kind() { return kind; }
    public ArcaneLampFuel fuel() { return fuel; }
    @Override public Direction facing() { return getBlockState().getValue(ArcaneLampBlock.FACING); }
    @Override public EssentiaTransport neighbor(Direction face) {
        if (level==null || !level.hasChunkAt(worldPosition.relative(face))) return null;
        return level.getBlockEntity(worldPosition.relative(face)) instanceof EssentiaTransport source?source:null;
    }
    @Override public void changed() {
        setChanged(); if (level==null || level.isClientSide || kind==ArcaneLampBlock.Kind.LIGHT) return;
        var state=getBlockState(); boolean active=fuel.charges()>0;
        if (state.getValue(ArcaneLampBlock.ACTIVE)!=active) level.setBlock(worldPosition,state.setValue(ArcaneLampBlock.ACTIVE,active),2);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,ArcaneLampBlockEntity lamp) {
        if (!(level instanceof ServerLevel server)) return;
        if (lamp.kind==ArcaneLampBlock.Kind.LIGHT) { lamp.spreadLight(server); return; }
        lamp.fuel.tick();
        if (lamp.kind==ArcaneLampBlock.Kind.GROWTH && lamp.fuel.charges()>0) lamp.growPlant(server);
        if (lamp.kind==ArcaneLampBlock.Kind.FERTILITY && lamp.fuel.charges()>1 && lamp.animalCounter++%300==0) lamp.breedAnimals(server);
    }
    private void spreadLight(ServerLevel level) {
        var random=level.random;
        int x=worldPosition.getX()+random.nextInt(16)-random.nextInt(16),y=worldPosition.getY()+random.nextInt(16)-random.nextInt(16),z=worldPosition.getZ()+random.nextInt(16)-random.nextInt(16);
        if (!level.hasChunk(x>>4,z>>4)) return;
        y=Math.max(5,Math.min(y,level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z)+4));
        var at=new BlockPos(x,y,z);
        if (level.isOutsideBuildHeight(at) || !level.hasChunkAt(at)) return;
        if (level.isEmptyBlock(at) && !level.getBlockState(at).is(TCArcane.LAMP_AIR.get()) && level.getMaxLocalRawBrightness(at)<9)
            level.setBlock(at,TCArcane.LAMP_AIR.get().defaultBlockState(),3);
    }
    public void removeLights() {
        if (level==null) return;
        // spreadLight can project any sample down to terrain height + 4, with
        // a floor of 5. Include that complete footprint, also after save/load.
        int bottom=Math.min(worldPosition.getY()-15,5),top=Math.max(worldPosition.getY()+15,5);
        for (var at:BlockPos.betweenClosed(new BlockPos(worldPosition.getX()-15,bottom,worldPosition.getZ()-15),
                new BlockPos(worldPosition.getX()+15,top,worldPosition.getZ()+15)))
            if (!level.isOutsideBuildHeight(at) && level.hasChunkAt(at) && level.getBlockState(at).is(TCArcane.LAMP_AIR.get())) level.setBlock(at,Blocks.AIR.defaultBlockState(),3);
    }
    private void growPlant(ServerLevel level) {
        var current=level.getBlockState(previous);
        if (current!=previousState) {
            if (level.getNearestPlayer(previous.getX(),previous.getY(),previous.getZ(),32,false)!=null) tc4revival.network.FocusEffectPacket.blockSparkle(level,previous,0x40ff40);
            previousState=current;
        }
        if (columns.isEmpty()) {
            for (int x=-6;x<=6;x++) for (int z=-6;z<=6;z++) columns.add(worldPosition.offset(x,6,z));
            for (int i=columns.size();i>1;i--) Collections.swap(columns,i-1,level.random.nextInt(i));
        }
        // Read the removed entry itself: the old get(0) after remove(0) crashes on the final column.
        var column=columns.remove(0);
        for (int y=Math.min(column.getY(),level.getMaxBuildHeight()-1);y>=Math.max(worldPosition.getY()-6,level.getMinBuildHeight());y--) {
            var target=new BlockPos(column.getX(),y,column.getZ());
            if (!level.hasChunkAt(target) || worldPosition.distSqr(target)>=36 || !LampPlantGrowth.candidate(level,target)) continue;
            fuel.consume(1); previous=target; previousState=level.getBlockState(target); LampPlantGrowth.schedule(level,target,previousState); return;
        }
    }
    private void breedAnimals(ServerLevel level) {
        var animals=level.getEntitiesOfClass(Animal.class,new AABB(worldPosition).inflate(7));
        for (var animal:animals) {
            if (animal.getAge()!=0 || animal.isInLove()) continue;
            var same=animals.stream().filter(other->other.getClass()==animal.getClass()).toList();
            if (same.size()>7) continue;
            Animal first=null;
            for (var partner:same) if (partner.getAge()==0 && !partner.isInLove()) {
                if (first==null) first=partner;
                else { fuel.consume(2); first.setInLove(null); partner.setInLove(null); return; }
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("orientation",facing().get3DDataValue()); if (kind!=ArcaneLampBlock.Kind.LIGHT) fuel.save(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); if (kind!=ArcaneLampBlock.Kind.LIGHT) fuel.load(tag); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public boolean connectable(Direction face) { return kind!=ArcaneLampBlock.Kind.LIGHT && fuel.connectable(face); }
    @Override public boolean canInput(Direction face) { return connectable(face); }
    @Override public boolean canOutput(Direction face) { return false; }
    @Override public Aspect suctionType(Direction face) { return kind==ArcaneLampBlock.Kind.LIGHT?null:fuel.suctionType(face); }
    @Override public int suction(Direction face) { return kind==ArcaneLampBlock.Kind.LIGHT?0:fuel.suction(face); }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) { return null; }
    @Override public int essentiaAmount(Direction face) { return 0; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) { return 0; }
}

package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

public  class AdvancedAlchemyBlockEntity extends BlockEntity implements AdvancedAlchemyProcess.Host,EssentiaTransport,RevealedAspectContainer {
    private final AdvancedAlchemyProcess process=new AdvancedAlchemyProcess();
    private BlockPos corePosition;
    private Direction outlet;
    private boolean located,destroy;
    public AdvancedAlchemyBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ADVANCED_ALCHEMY_ENTITY.get(),pos,state); }
    public boolean core() { return getBlockState().getValue(AdvancedAlchemyBlock.PART)==0; }
    public AdvancedAlchemyProcess process() { return process; }
    public void requestDismantle() { destroy=true; }
    public AdvancedAlchemyBlockEntity controller() {
        if (core()) return this;
        if (!located && level!=null) {
            located=true;
            for (var side:Direction.values()) {
                var pos=worldPosition.relative(side);
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof AdvancedAlchemyBlockEntity entity && entity.core()) {
                    corePosition=pos; outlet=side.getOpposite(); break;
                }
            }
        }
        return level!=null && corePosition!=null && level.hasChunkAt(corePosition)
                && level.getBlockEntity(corePosition) instanceof AdvancedAlchemyBlockEntity entity && entity.core() && !entity.isRemoved() ? entity : null;
    }
    public void tick() {
        if (level==null) return;
        if (!core()) { controller(); return; }
        if (level.isClientSide) return;
        if (destroy) { AdvancedAlchemyStructure.dismantle(level,worldPosition,true); return; }
        process.tick(this);
    }
    @Override public int drain(Aspect aspect,int requested) { return VisNetwork.drain(level,worldPosition,aspect,requested); }
    @Override public void changed(boolean sync) {
        setChanged();
        if (level!=null && !level.isClientSide && sync) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
            for (var side:Direction.Plane.HORIZONTAL) level.updateNeighbourForOutputSignal(worldPosition.relative(side),getBlockState().getBlock());
        }
    }
    @Override public AspectAmounts revealedAspects() { var core=controller(); return core==null ? new AspectAmounts() : core.process.contents(); }
    @Override public boolean connectable(Direction face) { return !core() && controller()!=null && face==outlet; }
    @Override public boolean canInput(Direction face) { return false; }
    @Override public boolean canOutput(Direction face) { return connectable(face); }
    @Override public Aspect suctionType(Direction face) { return null; }
    @Override public int suction(Direction face) { return 0; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) { var core=controller(); return core==null || core() ? null : core.process.firstAspect(); }
    @Override public int essentiaAmount(Direction face) { var core=controller(); var aspect=essentiaType(face); return core==null || aspect==null ? 0 : core.process.amount(aspect); }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { var core=controller(); return core!=null && canOutput(face) && core.process.take(aspect,amount,core) ? amount : 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (core()) process.save(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); process.load(tag); located=false; corePosition=null; outlet=null; destroy=false; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(connection,packet); if (level!=null) level.getChunkSource().getLightEngine().checkBlock(worldPosition);
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition.offset(-1,0,-1),worldPosition.offset(2,2,2)); }
}

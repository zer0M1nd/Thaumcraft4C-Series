package tc4revival.content;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;

/** A paired remote source, with no local inventory and no positive availability query. */
public  class EssentiaMirrorBlockEntity extends BlockEntity implements EssentiaSource {
    private static final ThreadLocal<HashSet<GlobalPos>> DRAINING=ThreadLocal.withInitial(HashSet::new);
    private GlobalPos destination;
    private boolean linked;
    private Direction linkedFacing;
    private int count,interval=40;
    public EssentiaMirrorBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ESSENTIA_MIRROR_ENTITY.get(),pos,state); }
    @Override public boolean mirror() { return true; }
    @Override public GlobalPos mirrorDestination() { return destination; }
    public boolean linked() { return linked; }
    public Direction facing() { return getBlockState().getValue(EssentiaMirrorBlock.FACING); }
    private GlobalPos location() { return GlobalPos.of(level.dimension(),worldPosition); }
    public void remember(GlobalPos target) { destination=target; linked=false; linkedFacing=null; changed(); }
    private EssentiaMirrorBlockEntity target() {
        if (!(level instanceof ServerLevel server) || destination==null) return null;
        var world=server.getServer().getLevel(destination.dimension());
        if (world==null || !world.hasChunkAt(destination.pos())) return null;
        return world.getBlockEntity(destination.pos()) instanceof EssentiaMirrorBlockEntity mirror && mirror!=this ? mirror : null;
    }
    public boolean valid(boolean invalidate) {
        if (!linked) return false;
        var other=target();
        boolean valid=other!=null && other.linked && location().equals(other.destination);
        if (!valid && invalidate) { linked=false; changed(); }
        return valid;
    }
    public boolean restoreLink() {
        var other=target(); if (other==null || other.valid(true)) return false;
        other.destination=location(); other.linked=true; other.changed();
        linked=true; linkedFacing=other.facing(); changed(); return true;
    }
    public void invalidateLink() {
        var other=target();
        if (other!=null) { other.linked=false; other.changed(); }
    }
    public void tick() {
        if (level==null || level.isClientSide || count++%interval!=0) return;
        if (valid(false)) interval=40;
        else { if (interval<600) interval+=20; restoreLink(); }
    }
    @Override public boolean contains(Aspect aspect,int quantity) { return false; }
    @Override public java.util.List<? extends EssentiaSource> availabilitySources() {
        var other=target();return !valid(false)||other==null?java.util.List.of():
                EssentiaSources.availableSources(other,linkedFacing==null?other.facing():linkedFacing,8,true);
    }
    @Override public boolean takeFromContainer(Aspect aspect,int quantity) {
        if (aspect==null || quantity>1 || quantity<0 || !valid(true)) return false;
        var other=target(); if (other==null) return false;
        if (linkedFacing==null) linkedFacing=other.facing();
        var active=DRAINING.get(); var here=location();
        if (!active.add(here)) return false;
        try {
            var source=EssentiaSources.drain(other,aspect,linkedFacing,8,true);
            source.ifPresent(pos->tc4revival.network.EssentiaSourcePacket.send((ServerLevel)other.level,other.worldPosition,pos,aspect.color()));
            return source.isPresent();
        }
        finally { active.remove(here); if (active.isEmpty()) DRAINING.remove(); }
    }
    private void changed() {
        setChanged();
        if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putBoolean("linked",linked);
        if (destination!=null) {
            tag.putString("linkDimension",destination.dimension().location().toString());
            tag.putInt("linkX",destination.pos().getX()); tag.putInt("linkY",destination.pos().getY()); tag.putInt("linkZ",destination.pos().getZ());
        }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); linked=tag.getBoolean("linked"); destination=null; linkedFacing=null;
        var dimension=ResourceLocation.tryParse(tag.getString("linkDimension"));
        if (dimension!=null && tag.contains("linkX",3) && tag.contains("linkY",3) && tag.contains("linkZ",3))
            destination=GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),new BlockPos(tag.getInt("linkX"),tag.getInt("linkY"),tag.getInt("linkZ")));
        else linked=false;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

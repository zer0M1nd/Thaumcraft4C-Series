package thmodern.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeApi;
import thmodern.Horizons;

public final class RecombinatorEntity extends BlockEntity {
    private int warmup;
    private boolean active;
    private int previousWarmup;
    public RecombinatorEntity(BlockPos pos, BlockState state) { super(Horizons.RECOMBINATOR_ENTITY.get(), pos, state); }
    public int warmup() { return warmup; }
    public float animationCount(float partial){return net.minecraft.util.Mth.lerp(partial,previousWarmup,warmup);}
    public void tick() {
        previousWarmup=warmup;
        if(level!=null&&level.isClientSide){warmup=active?(warmup==Integer.MAX_VALUE?51:warmup+1):Math.max(0,Math.min(50,warmup)-1);return;}
        if (!(level instanceof ServerLevel server)) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        int before = warmup;
        warmup = powered ? (warmup==Integer.MAX_VALUE?51:warmup+1) : Math.max(0, Math.min(50, warmup) - 1);
        if (warmup != before) setChanged();
        if(active!=powered){active=powered;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);setChanged();}
        if (!powered || warmup <= 50 || !level.hasChunkAt(worldPosition.below())
                || !(level.getBlockEntity(worldPosition.below()) instanceof AuraNodeBlockEntity receiver)) return;
        var random = level.random;
        var sourcePos = worldPosition.offset(random.nextInt(5)-random.nextInt(5),
                random.nextInt(5)-random.nextInt(5)-1, random.nextInt(5)-random.nextInt(5));
        if (sourcePos.equals(receiver.getBlockPos()) || !level.hasChunkAt(sourcePos)
                || !(level.getBlockEntity(sourcePos) instanceof AuraNodeBlockEntity donor)
                || !(donor.getBlockState().getBlock() instanceof AuraNodeBlock)) return;
        // Native stabilizers expose their strength; no private node lock access is necessary.
        if (level.hasChunkAt(sourcePos.below()) && level.getBlockEntity(sourcePos.below()) instanceof NodeStabilizerBlockEntity
                && !level.hasNeighborSignal(sourcePos.below())) return;
        NodeApi.check(receiver); NodeApi.check(donor);
        var plans = Recombination.candidates(TCAspects.catalog(), receiver.permanentBase(), receiver.aspects(),
                donor.permanentBase(), donor.aspects());
        if (plans.isEmpty()) return;
        var plan = plans.get(random.nextInt(plans.size()));
        var a = thaumcraft.api.aspects.Aspect.wrap(plan.receiver());
        var b = thaumcraft.api.aspects.Aspect.wrap(plan.donor());
        NodeApi.extract(receiver, a, 1, false);
        NodeApi.extract(donor, b, 1, false);
        var capacity = receiver.permanentBase().remove(plan.receiver(), 1).add(plan.result(), 1);
        NodeApi.edit(receiver, AspectList.from(capacity), receiver.permanentType(), receiver.permanentModifier());
        NodeApi.insert(receiver, thaumcraft.api.aspects.Aspect.wrap(plan.result()), 1, false);
        if (random.nextInt(3) == 0) NodeApi.edit(donor,
                AspectList.from(donor.permanentBase().remove(plan.donor(), 1)), donor.permanentType(), donor.permanentModifier());
        int rgb = plan.result().color();
        thaumcraft.api.fx.LegacyEffects.beam(server,sourcePos.getCenter(),worldPosition.getCenter(),0xff00ff,.5f,8);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Warmup", warmup); tag.putBoolean("Active",active); }
    @Override public void load(CompoundTag tag) { super.load(tag); warmup = Math.max(0,tag.getInt("Warmup"));previousWarmup=warmup;active=tag.getBoolean("Active"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

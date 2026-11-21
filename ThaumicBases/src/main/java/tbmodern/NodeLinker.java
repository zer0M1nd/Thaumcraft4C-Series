package tbmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.nodes.NodeApi;
import thaumcraft.api.wands.IWandable;

/** First wand click chooses the receiver; the second selects its donor. Never force-loads chunks. */
public final class NodeLinker extends BlockEntity implements IWandable {
    private static final String SELECTION = "thaumicbases:node_link_selection";
    private static final TagKey<Block> STABILIZERS = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/ichorium"));
    private BlockPos donor;
    private int instability;
    public NodeLinker(BlockPos pos, BlockState state) { super(Content.LINKER.get(), pos, state); }
    public BlockPos donor() { return donor; }
    public int instability() { return instability; }
    @Override public AABB getRenderBoundingBox() {
        AABB bounds = new AABB(worldPosition).inflate(1);
        return donor == null ? bounds : bounds.minmax(new AABB(donor).inflate(1));
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand, UseOnContext context) {
        if (level == null || context.getPlayer() == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            var tag = wand.getOrCreateTag();
            var selection = tag.getCompound(SELECTION);
            String dimension = level.dimension().location().toString();
            if (selection.getString("Dimension").equals(dimension) && selection.contains("Position")) {
                BlockPos first = BlockPos.of(selection.getLong("Position"));
                if (!first.equals(worldPosition) && level.hasChunkAt(first) && level.getBlockEntity(first) instanceof NodeLinker receiver) {
                    receiver.donor = worldPosition.immutable(); receiver.instability = 0; receiver.sync();
                    tag.remove(SELECTION);
                    context.getPlayer().displayClientMessage(Component.translatable("tb.txt.linkEstablished"), true);
                    return InteractionResult.SUCCESS;
                }
            }
            var next = new CompoundTag(); next.putString("Dimension", dimension); next.putLong("Position", worldPosition.asLong());
            tag.put(SELECTION, next);
            context.getPlayer().displayClientMessage(Component.translatable("tb.txt.linkStarted"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, NodeLinker linker) {
        if (linker.donor == null) { if (linker.instability != 0) { linker.instability = 0; linker.sync(); } return; }
        // An unloaded endpoint is paused, not considered destroyed.
        if (!level.hasChunkAt(linker.donor)) return;
        if (!(level.getBlockEntity(linker.donor) instanceof NodeLinker other) || other.donor != null || linker.donor.equals(pos)) {
            linker.donor = null; linker.instability = 0; linker.sync(); return;
        }
        if (!(level.getBlockEntity(pos.below()) instanceof AuraNodeBlockEntity receiver)
            || !(level.getBlockEntity(linker.donor.below()) instanceof AuraNodeBlockEntity transmitter)) return;
        if(level.getGameTime()%4==0)WorkEffects.link(level,pos,linker.donor);
        if (level.getGameTime() % 10 != 0) return;
        var donorSnapshot = NodeApi.snapshot(transmitter);
        boolean permanent = level.random.nextDouble() < .3 || donorSnapshot.charge().visSize() == 0;
        AspectList from = permanent ? NodeApi.getBase(transmitter) : donorSnapshot.charge();
        if (from.visSize() == 0) { level.removeBlock(transmitter.getBlockPos(), false); return; }
        Aspect[] aspects = java.util.Arrays.stream(from.getAspects()).filter(a -> from.getAmount(a)>0).toArray(Aspect[]::new);
        if(aspects.length==0)return;
        Aspect aspect = aspects[level.random.nextInt(aspects.length)];
        if (permanent) {
            boolean accepted = level.random.nextDouble() <= .7 && linker.stable(transmitter);
            // Instability may destroy the donor; never mutate a removed block entity.
            if (!transmitter.isRemoved() && level.getBlockEntity(transmitter.getBlockPos()) == transmitter) {
                var base = NodeApi.getBase(transmitter);
                if(base.getAmount(aspect)<1) { linker.sync(); return; }
                base.remove(aspect, 1);
                NodeApi.edit(transmitter, base, transmitter.permanentType(), transmitter.permanentModifier());
                if (accepted) {
                    var destination = NodeApi.getBase(receiver);
                    if (destination.getAmount(aspect) < 1000000) {
                        destination.add(aspect, 1);
                        NodeApi.edit(receiver, destination, receiver.permanentType(), receiver.permanentModifier());
                        NodeApi.insert(receiver, aspect, 1, false);
                    }
                }
            }
            linker.instability = Math.min(1000000, linker.instability + 1);
        } else {
            var destination = NodeApi.snapshot(receiver);
            boolean accepted = destination.charge().getAmount(aspect) > 0
                && destination.charge().getAmount(aspect) <= destination.capacity().getAmount(aspect) && linker.stable(transmitter);
            if (!transmitter.isRemoved() && level.getBlockEntity(transmitter.getBlockPos()) == transmitter) {
                int taken = NodeApi.extract(transmitter, aspect, 1, false);
                if (accepted && taken > 0) NodeApi.insert(receiver, aspect, taken, false);
            }
            if (level.random.nextDouble() <= .33) linker.instability = Math.min(1000000, linker.instability + 1);
        }
        linker.sync();
    }
    private boolean stable(AuraNodeBlockEntity transmitter) {
        if (instability <= 0 || level.random.nextInt(50) > instability) return true;
        int choice = level.random.nextInt(instability), reduction = 0;
        boolean protectedNode = level.getBlockState(worldPosition.above()).is(STABILIZERS);
        if (choice == 49) {
            if (!protectedNode) {
                BlockPos target = transmitter.getBlockPos(); level.removeBlock(target, false);
                level.explode(null, target.getX()+.5, target.getY()+.5, target.getZ()+.5, 2, Level.ExplosionInteraction.BLOCK);
                reduction = instability;
            }
        } else if (choice >= 45) {
            if (!protectedNode) {
                boolean baseDamage = level.random.nextBoolean();
                var values = baseDamage ? NodeApi.getBase(transmitter) : NodeApi.snapshot(transmitter).charge();
                if (values.size() > 0) {
                    Aspect aspect = values.getAspects()[level.random.nextInt(values.size())];
                    int amount = Math.min(4 + level.random.nextInt(16), values.getAmount(aspect));
                    if (baseDamage) { values.remove(aspect, amount); NodeApi.edit(transmitter, values, transmitter.permanentType(), transmitter.permanentModifier()); }
                    else NodeApi.extract(transmitter, aspect, amount, false);
                    reduction = baseDamage ? 25 : 6;
                }
            }
        } else {
            BlockPos endpoint = level.random.nextBoolean() ? donor : worldPosition;
            if (choice >= 31) {
                var wisp = TCEntities.WISP.get().create(level);
                if (wisp != null) {
                    var all = tc4revival.aspect.TCAspects.catalog().all();
                    var options = new java.util.ArrayList<>(all);
                    wisp.setAspectTag(options.get(level.random.nextInt(options.size())).tag());
                    wisp.moveTo(endpoint.getX()+.5,endpoint.getY()-.5,endpoint.getZ()+.5,0,0);level.addFreshEntity(wisp);
                }
                reduction = 9;
            } else if (choice >= 20) {
                var players = level.getEntitiesOfClass(Player.class,new AABB(endpoint).inflate(5));
                if (!players.isEmpty()) { players.get(level.random.nextInt(players.size())).hurt(level.damageSources().magic(),4+level.random.nextInt(4)); reduction = 5; }
            } else {
                BlockPos target = choice >= 10 ? endpoint.below().relative(Direction.from2DDataValue(level.random.nextInt(4))) : endpoint.above(2);
                if (level.hasChunkAt(target) && !level.isOutsideBuildHeight(target))
                    level.setBlock(target,(choice >= 10 ? TCArcane.FLUX_GOO.get() : TCArcane.FLUX_GAS.get()).defaultBlockState(),3);
                reduction = choice >= 10 ? 5 : 3;
            }
        }
        instability = Math.max(0, instability - reduction);
        return false;
    }
    private void sync() { setChanged(); if(level != null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); if(donor != null) tag.putLong("Donor",donor.asLong()); tag.putInt("Instability",instability);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); donor = tag.contains("Donor") ? BlockPos.of(tag.getLong("Donor")) : null;
        instability = Math.max(0,Math.min(1000000,tag.getInt("Instability")));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

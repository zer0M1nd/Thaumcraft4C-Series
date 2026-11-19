package tc4revival.content;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.TCAspects;

/** The top centre is deliberately unconstrained by the old 3 by 2 by 3 blueprint. */
public  class AdvancedAlchemyStructure {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    private AdvancedAlchemyStructure() {}
    static boolean mutating() { return DEPTH.get()>0; }
    private static <T> T quietly(java.util.function.Supplier<T> operation) {
        int depth=DEPTH.get(); DEPTH.set(depth+1);
        try { return operation.get(); } finally { if (depth==0) DEPTH.remove(); else DEPTH.set(depth); }
    }
    public static int part(int x,int y,int z) {
        if (x==0 && z==0) return y==0 ? 0 : -1;
        return y==0 ? (x==0 || z==0 ? 1 : 4) : (x==0 || z==0 ? 3 : 2);
    }
    public static BlockState restored(int part) {
        return switch(part) {
            case 0->TCArcane.ALCHEMY_FURNACE.get().defaultBlockState();
            case 2->TCArcane.ALEMBIC.get().defaultBlockState();
            case 3->TCArcane.ALCHEMICAL_CONSTRUCT.get().defaultBlockState();
            default->TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get().defaultBlockState();
        };
    }
    public static boolean matches(Level level,BlockPos origin) {
        for (int x=-1;x<=1;x++) for (int y=0;y<=1;y++) for (int z=-1;z<=1;z++) {
            int part=part(x,y,z); if (part<0) continue;
            var pos=origin.offset(x,y,z);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(restored(part).getBlock())) return false;
        }
        return true;
    }
    public static InteractionResult form(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var clicked=context.getClickedPos(); var clickedState=level.getBlockState(clicked);
        if (!clickedState.is(TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get()) && !clickedState.is(TCArcane.ALCHEMICAL_CONSTRUCT.get())) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)) return InteractionResult.PASS;
        if (!tc4revival.research.KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID())
                .map(knowledge->knowledge.researchMarkers().contains("ADVALCHEMYFURNACE")).orElse(false)) return InteractionResult.PASS;
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++) {
            var origin=clicked.offset(x,y,z);
            if (!level.hasChunkAt(origin) || !level.getBlockState(origin).is(TCArcane.ALCHEMY_FURNACE.get())) continue;
            // TC4 stops at the first nearby base furnace, including when its blueprint is invalid.
            if (!matches(level,origin)) return InteractionResult.PASS;
            var states=new LinkedHashMap<BlockPos,BlockState>(); var data=new LinkedHashMap<BlockPos,CompoundTag>();
            var drops=new java.util.ArrayList<ItemStack>();
            if (level.getBlockEntity(origin) instanceof net.minecraft.world.Container inventory)
                for (int slot=0;slot<inventory.getContainerSize();slot++) if (!inventory.getItem(slot).isEmpty()) drops.add(inventory.getItem(slot).copy());
            for (int dx=-1;dx<=1;dx++) for (int dy=0;dy<=1;dy++) for (int dz=-1;dz<=1;dz++) {
                if (part(dx,dy,dz)<0) continue;
                var pos=origin.offset(dx,dy,dz);
                if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
                states.put(pos,level.getBlockState(pos)); var entity=level.getBlockEntity(pos);
                if (entity!=null) data.put(pos,entity.saveWithFullMetadata());
            }
            var wand=(CastingWandItem)stack.getItem();
            var cost=java.util.Map.of(TCAspects.catalog().require("ignis"),50,TCAspects.catalog().require("aqua"),50,TCAspects.catalog().require("ordo"),50);
            if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:advanced_alchemical_construct",wand,stack,player,cost,true,true,false)) return InteractionResult.PASS;
            boolean committed=convert(level,origin,states,data,
                    ()->thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:advanced_alchemical_construct",wand,stack,player,cost,true,true,true),
                    (pos,state)->level.setBlock(pos,state,18));
            if (!committed) return InteractionResult.FAIL;
            // Delay the old base-furnace inventory drop until the whole conversion commits.
            for (var drop:drops) net.minecraft.world.Containers.dropItemStack(level,origin.getX(),origin.getY(),origin.getZ(),drop);
            for (var pos:states.keySet()) {
                level.updateNeighborsAt(pos,TCArcane.ADVANCED_ALCHEMY_FURNACE.get());
                tc4revival.network.FocusEffectPacket.blockSparkle(player.serverLevel(),pos,-9999);
            }
            level.playSound(null,origin,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    /** Shared by the real wand path and failure-injection scenarios; charge follows all writes. */
    static boolean convert(Level level,BlockPos origin,LinkedHashMap<BlockPos,BlockState> states,
            LinkedHashMap<BlockPos,CompoundTag> data,java.util.function.BooleanSupplier charge,
            java.util.function.BiPredicate<BlockPos,BlockState> writer) {
        return quietly(()->{
            boolean success=false;
            try {
                for (var pos:states.keySet()) {
                    var state=formedState(origin,pos);
                    if (!writer.test(pos,state) || !level.getBlockState(pos).equals(state)) return false;
                }
                // A later write can invalidate an earlier block through callbacks.
                for (var pos:states.keySet()) {
                    var expected=formedState(origin,pos);
                    if (!level.getBlockState(pos).equals(expected)) return false;
                    if (expected.getValue(AdvancedAlchemyBlock.PART)<=1
                            && !(level.getBlockEntity(pos) instanceof AdvancedAlchemyBlockEntity)) return false;
                }
                if (!charge.getAsBoolean()) return false;
                success=true; return true;
            } finally {
                if (!success) {
                    // Restore geometry before inventories, so no transient neighbour sees half-restored data.
                    for (var entry:states.entrySet()) {
                        if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) level.setBlock(entry.getKey(),entry.getValue(),18);
                        if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) throw new IllegalStateException("Cannot restore advanced alchemy furnace at "+entry.getKey());
                    }
                    for (var entry:data.entrySet()) {
                        var entity=level.getBlockEntity(entry.getKey());
                        if (entity==null) throw new IllegalStateException("Missing restored alchemy block entity at "+entry.getKey());
                        entity.load(entry.getValue().copy()); entity.setChanged();
                    }
                    for (var entry:states.entrySet()) level.sendBlockUpdated(entry.getKey(),entry.getValue(),entry.getValue(),2);
                }
            }
        });
    }
    private static BlockState formedState(BlockPos origin,BlockPos pos) {
        return TCArcane.ADVANCED_ALCHEMY_FURNACE.get().defaultBlockState().setValue(AdvancedAlchemyBlock.PART,
                part(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ()));
    }
    public static void dismantle(Level level,BlockPos origin,boolean includeCore) {
        var changed=new java.util.ArrayList<BlockPos>();
        quietly(()->{
            for (int x=-1;x<=1;x++) for (int y=0;y<=1;y++) for (int z=-1;z<=1;z++) {
                if (x==0 && y==0 && z==0) continue;
                var pos=origin.offset(x,y,z); if (!level.hasChunkAt(pos)) continue;
                var state=level.getBlockState(pos);
                if (state.is(TCArcane.ADVANCED_ALCHEMY_FURNACE.get())) { level.setBlock(pos,restored(state.getValue(AdvancedAlchemyBlock.PART)),18); changed.add(pos); }
            }
            if (includeCore && level.getBlockState(origin).is(TCArcane.ADVANCED_ALCHEMY_FURNACE.get())) {
                level.setBlock(origin,restored(0),18); changed.add(origin);
            }
            return true;
        });
        for (var pos:changed) level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
    }
}

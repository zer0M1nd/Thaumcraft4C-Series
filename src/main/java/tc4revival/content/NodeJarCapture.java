package tc4revival.content;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

public  class NodeJarCapture {
    private NodeJarCapture() {}
    public static InteractionResult form(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var clicked=context.getClickedPos(); var clickedState=level.getBlockState(clicked);
        if (!clickedState.is(Blocks.GLASS) && !clickedState.is(BlockTags.WOODEN_SLABS)) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        if (!KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).map(knowledge->knowledge.researchMarkers().contains("NODEJAR")).orElse(false)) return InteractionResult.PASS;
        for (int x=-2;x<=0;x++) for (int y=-3;y<=0;y++) for (int z=-2;z<=0;z++) {
            var origin=clicked.offset(x,y,z); if (!matches(level,origin)) continue;
            for (int row=0;row<4;row++) for (int column=0;column<3;column++) for (int depth=0;depth<3;depth++) {
                var pos=origin.offset(column,2-row,depth);
                if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
            }
            var center=origin.offset(1,0,1); var node=payload(level,center); if (node==null) continue;
            var cost=new LinkedHashMap<Aspect,Integer>();
            for (String aspect:new String[]{"ignis","terra","ordo","aer","perditio","aqua"}) cost.put(TCAspects.catalog().require(aspect),70);
            var wand=(CastingWandItem)stack.getItem();
            if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:node_jar",wand,stack,player,cost,true,true,false)) continue;
            var modifier=level.random.nextFloat()<.75f ? NodeJarData.degraded(node.modifier()) : node.modifier();
            if (!convert(level,origin,new NodeJarData(node.aspects(),node.type(),modifier,node.id(),node.capacity()),
                    ()->thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:node_jar",wand,stack,player,cost,true,true,true),
                    (pos,state)->level.setBlock(pos,state,18))) return InteractionResult.FAIL;
            level.blockEvent(center,level.getBlockState(center).getBlock(),9,0);
            level.playSound(null,origin,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    /** No neighbour callbacks or payment until the entire shell and node have been converted. */
    static boolean convert(Level level,BlockPos origin,NodeJarData result,java.util.function.BooleanSupplier charge,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> write) {
        return convertValidated(level,origin,result,charge,write,()->matches(level,origin));
    }
    /** Alternate addon shells supply their complete ownership/structure validation. Failed writes restore every tile. */
    public static boolean convertValidated(Level level,BlockPos origin,NodeJarData result,java.util.function.BooleanSupplier charge,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> write,
            java.util.function.BooleanSupplier structureValid) {
        if (level.isClientSide || !structureValid.getAsBoolean()) return false;
        var center=origin.offset(1,0,1);
        var node=level.getBlockEntity(center);
        if (node==null) return false;
        var nodeData=node.saveWithoutMetadata();
        var capture=thaumcraft.api.nodes.NodeCaptureExtensions.plan(
            new thaumcraft.api.nodes.NodeCaptureExtensions.Context(level,center,node.getType(),nodeData,result),TCArcane.NODE_JAR.get().defaultBlockState());
        var before=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var tileData=new LinkedHashMap<BlockPos,net.minecraft.nbt.CompoundTag>();
        for (int row=0;row<4;row++) for (int x=0;x<3;x++) for (int z=0;z<3;z++) {
            var pos=origin.offset(x,2-row,z); before.put(pos,level.getBlockState(pos));
            var tile=level.getBlockEntity(pos);if(tile!=null)tileData.put(pos,tile.saveWithoutMetadata());
        }
        boolean committed=false;
        try {
            var jarState=capture.state();
            if (!write.test(center,jarState) || !level.getBlockState(center).equals(jarState)
                    || !(level.getBlockEntity(center) instanceof NodeJarBlockEntity jar)) return false;
            jar.configure(result);capture.initialize(jar);
            for (var pos:before.keySet()) if (!pos.equals(center)) {
                if (!write.test(pos,Blocks.AIR.defaultBlockState()) || !level.getBlockState(pos).isAir()) return false;
            }
            if (level.getBlockEntity(center)!=jar || !charge.getAsBoolean()) return false;
            committed=true;
        } finally {
            if (!committed) {
                // Restore the shell before the node, including its former capacity and saved activity time.
                for (var entry:before.entrySet()) if (!entry.getKey().equals(center)) restore(level,entry.getKey(),entry.getValue());
                restore(level,center,before.get(center));
                var restored=level.getBlockEntity(center);
                if (restored==null || restored.getType()!=node.getType())
                    throw new IllegalStateException("Cannot restore captured node at "+center);
                restored.load(nodeData); restored.setChanged();
                for(var entry:tileData.entrySet()) if(!entry.getKey().equals(center)) {
                    var tile=level.getBlockEntity(entry.getKey());
                    if(tile==null)throw new IllegalStateException("Cannot restore node jar shell tile at "+entry.getKey());
                    tile.load(entry.getValue());tile.setChanged();
                    var state=level.getBlockState(entry.getKey());level.sendBlockUpdated(entry.getKey(),state,state,2);
                }
                level.sendBlockUpdated(center,before.get(center),before.get(center),2);
            }
        }
        for (var entry:before.entrySet()) {
            var pos=entry.getKey();
            level.updateNeighborsAt(pos,entry.getValue().getBlock());
            level.getBlockState(pos).updateNeighbourShapes(level,pos,3);
        }
        return true;
    }
    private static void restore(Level level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos,state,18);
        if (!level.getBlockState(pos).equals(state)) throw new IllegalStateException("Cannot restore node jar shell at "+pos);
    }
    static boolean matches(Level level,BlockPos origin) {
        for (int row=0;row<4;row++) for (int x=0;x<3;x++) for (int z=0;z<3;z++) {
            var pos=origin.offset(x,2-row,z);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
            var state=level.getBlockState(pos);
            if (row==0) {
                if (!state.is(BlockTags.WOODEN_SLABS) || (state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE)) return false;
            } else if (row==2 && x==1 && z==1) { if (payload(level,pos)==null) return false; }
            else if (!state.is(Blocks.GLASS)) return false;
        }
        return true;
    }
    public static NodeJarData payload(Level level,BlockPos pos) {
        var entity=level.getBlockEntity(pos);
        if (entity instanceof AuraNodeBlockEntity node) return new NodeJarData(node.portableCharge(),node.permanentType(),node.permanentModifier(),node.nodeId(),node.permanentBase());
        if (entity instanceof EnergizedNodeBlockEntity node) return new NodeJarData(node.supply(),node.nodeType(),node.nodeModifier(),node.nodeId());
        return null;
    }
}

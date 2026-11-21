package tc4revival.content;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

public  class InfusionAltar {
    private InfusionAltar() {}
    public static InteractionResult form(ItemStack wand,UseOnContext context) {
        var level=context.getLevel(); var clicked=context.getClickedPos();
        if (thaumcraft.api.crafting.InfusionAltarMaterials.forMatrix(level.getBlockState(clicked).getBlock())==null || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        if (!KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).map(k->k.researchMarkers().contains("INFUSION")).orElse(false)) return InteractionResult.PASS;
        for (int x=-2;x<=0;x++) for (int y=-2;y<=0;y++) for (int z=-2;z<=0;z++) {
            var origin=clicked.offset(x,y,z); if (!matches(level,origin)) continue;
            for (var pos:BlockPos.betweenClosed(origin,origin.offset(2,2,2)))
                if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),wand)) return InteractionResult.FAIL;
            var costs=new LinkedHashMap<Aspect,Integer>();
            for (String name:new String[]{"ignis","terra","ordo","aer","perditio","aqua"}) costs.put(TCAspects.catalog().require(name),25);
            var casting=(CastingWandItem)wand.getItem();
            if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:infusion_matrix",casting,wand,player,costs,true,true,false)) return InteractionResult.PASS;
            if (!convert(level,origin,()->thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:infusion_matrix",casting,wand,player,costs,true,true,true),
                    (pos,state)->level.setBlock(pos,state,2))) return InteractionResult.FAIL;
            level.playSound(null,origin,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    /** Publish activation and charge only after all eight pillar writes have succeeded. */
    static boolean convert(Level level,BlockPos origin,java.util.function.BooleanSupplier charge,
            java.util.function.BiPredicate<BlockPos,net.minecraft.world.level.block.state.BlockState> write) {
        if (!matches(level,origin)) return false;
        var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(origin.offset(1,2,1));
        var material=thaumcraft.api.crafting.InfusionAltarMaterials.forMatrix(matrix.getBlockState().getBlock());
        var before=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for (int dx:new int[]{0,2}) for (int dz:new int[]{0,2}) {
            var base=origin.offset(dx,0,dz);
            before.put(base.above(),level.getBlockState(base.above())); before.put(base,level.getBlockState(base));
        }
        boolean committed=false;
        try {
            for (var pos:before.keySet()) {
                var block=pos.getY()==origin.getY() ? material.pillar().get() : material.pillarTop().get();
                if (!write.test(pos,block.defaultBlockState()) || !level.getBlockState(pos).is(block)) return false;
                if (pos.getY()==origin.getY() && !(level.getBlockEntity(pos) instanceof InfusionPillarBlockEntity)) return false;
            }
            if (level.getBlockEntity(origin.offset(1,2,1))!=matrix || !charge.getAsBoolean()) return false;
            committed=true;
        } finally {
            if (!committed) {
                // These eight inputs are ordinary TC stone blocks; there is no inventory to drop or restore.
                var positions=new java.util.ArrayList<>(before.keySet());
                for (int i=positions.size()-1;i>=0;i--) {
                    var pos=positions.get(i); var state=before.get(pos);
                    if (level.getBlockState(pos)!=state) level.setBlock(pos,state,2);
                    if (level.getBlockState(pos)!=state) throw new IllegalStateException("Cannot restore infusion altar at "+pos);
                }
            }
        }
        for (int dx:new int[]{0,2}) for (int dz:new int[]{0,2}) {
            var base=origin.offset(dx,0,dz); var top=base.above();
            ((InfusionPillarBlockEntity)level.getBlockEntity(base)).orient(2+dx+dz/2);
            level.updateNeighborsAt(base,material.pillar().get());
            level.updateNeighborsAt(top,material.pillarTop().get());
            level.blockEvent(base,material.pillar().get(),1,0);
            level.blockEvent(top,material.pillarTop().get(),1,0);
        }
        matrix.activate(); return true;
    }
    static boolean matches(Level level,BlockPos origin) {
        var material=thaumcraft.api.crafting.InfusionAltarMaterials.forMatrix(level.getBlockState(origin.offset(1,2,1)).getBlock());
        if(material==null)return false;
        for (int y=0;y<3;y++) for (int x=0;x<3;x++) for (int z=0;z<3;z++) {
            var pos=origin.offset(x,y,z); if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
            var state=level.getBlockState(pos); boolean center=x==1 && z==1,corner=x!=1 && z!=1;
            if (y==2 && center) { if (!(level.getBlockEntity(pos) instanceof InfusionMatrixBlockEntity)) return false; }
            else if (y==0 && center) { if (!state.is(material.pedestal().get()) || !(level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity)) return false; }
            else if (y<2 && corner) { if (!state.is(y==0 ? material.lowerInput().get() : material.upperInput().get())) return false; }
            else if (!state.isAir()) return false;
        }
        return true;
    }
}

package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** TC4's ordered scan and truncated float symmetry score, without loading additional chunks. */
public record InfusionLayout(List<BlockPos> pedestals,int symmetry) {
    public static final TagKey<Block> STABILIZERS=TagKey.create(Registries.BLOCK,ResourceLocation.parse("thaumcraft:infusion_stabilizers"));
    public static final TagKey<net.minecraft.world.item.Item> STABILIZER_ITEMS=TagKey.create(Registries.ITEM,STABILIZERS.location());
    public InfusionLayout { pedestals=List.copyOf(pedestals); }
    public static InfusionLayout scan(Level level,BlockPos matrix) {
        var pedestals=new ArrayList<BlockPos>(); var stabilizers=new ArrayList<BlockPos>();
        for (int x=-12;x<=12;x++) for (int z=-12;z<=12;z++) {
            if (x==0 && z==0) continue;
            boolean foundPedestal=false;
            for (int depth=-5;depth<=10;depth++) {
                var pos=matrix.offset(x,-depth,z);
                if (!available(level,pos)) continue;
                if (!foundPedestal && depth>0 && Math.abs(x)<=8 && Math.abs(z)<=8 && level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity) {
                    pedestals.add(pos); foundPedestal=true; continue;
                }
                if (stabilizes(level,pos)) stabilizers.add(pos);
            }
        }
        int score=0;
        for (var pos:pedestals) {
            var pedestal=(ArcanePedestalBlockEntity)level.getBlockEntity(pos);
            boolean occupied=pedestal!=null && !pedestal.isEmpty();
            if (pedestal!=null) score+=occupied ? 3 : 2;
            var mirror=mirror(matrix,pos);
            if (available(level,mirror) && level.getBlockEntity(mirror) instanceof ArcanePedestalBlockEntity other) {
                score-=2; if (occupied && !other.isEmpty()) score--;
            }
        }
        float decorative=0;
        for (var pos:stabilizers) {
            if (stabilizes(level,pos)) decorative+=.1f;
            var mirror=mirror(matrix,pos);
            if (available(level,mirror) && stabilizes(level,mirror)) decorative-=.2f;
        }
        return new InfusionLayout(tc4revival.config.TCConfig.SERVER.orderedInfusion.get()?InfusionOrder.clockwise(matrix,pedestals):pedestals,(int)(score+decorative));
    }
    private static boolean stabilizes(Level level,BlockPos pos) {
        var state=level.getBlockState(pos);
        if (state.is(STABILIZERS) || state.getBlock().asItem().builtInRegistryHolder().is(STABILIZER_ITEMS)) return true;
        if(state.getBlock() instanceof thaumcraft.api.crafting.IInfusionStabiliser block)return block.canStabiliseInfusion(level,pos);
        if(level.getBlockEntity(pos) instanceof thaumcraft.api.crafting.IInfusionStabiliser entity)return entity.canStabiliseInfusion(level,pos);
        return state.is(STABILIZERS);
    }
    private static BlockPos mirror(BlockPos matrix,BlockPos pos) { return new BlockPos(matrix.getX()*2-pos.getX(),pos.getY(),matrix.getZ()*2-pos.getZ()); }
    private static boolean available(Level level,BlockPos pos) { return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos); }
}

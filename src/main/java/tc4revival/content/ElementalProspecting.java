package tc4revival.content;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Blocks;
import tc4revival.research.ScanTargetResolver;
/** Snapshot of the old 17-cube prospecting volume, with server-owned aspect definitions. */
public  class ElementalProspecting {
    public record Mark(int x,int y,int z,int value) {
        public Mark { if(Math.abs(x)>8||Math.abs(y)>8||Math.abs(z)>8||value< -10 || (value<0 && value!= -10 && value!= -5))throw new IllegalArgumentException("Invalid prospecting mark"); }
    }
    private static final TagKey<net.minecraft.world.level.block.Block> ORES=TagKey.create(Registries.BLOCK,ResourceLocation.parse("forge:ores"));
    private ElementalProspecting() {}
    public static List<Mark> scan(ServerLevel level,BlockPos center) {
        var marks=new ArrayList<Mark>();
        for(int x=-8;x<=8;x++)for(int y=-8;y<=8;y++)for(int z=-8;z<=8;z++) {
            var pos=center.offset(x,y,z);if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);if(state.isAir()||state.is(Blocks.BEDROCK))continue;
            int value=-1;
            if(state.getFluidState().is(FluidTags.LAVA))value=-10;
            else if(state.getFluidState().is(FluidTags.WATER))value=-5;
            else if(state.is(ORES))value=ScanTargetResolver.block(level,pos).map(target->Math.toIntExact(target.aspects().total())).orElse(0);
            if(value!= -1)marks.add(new Mark(x,y,z,value));
        }
        return List.copyOf(marks);
    }
}

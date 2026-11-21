package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import tc4revival.content.*;

/** Original Dyes-gated Overworld tower, retaining its terrain gate, layout and 3 + [0,4] archers. */
public final class CrimsonTowerFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation LOOT=new ResourceLocation(ThaumicConcilium.ID,"chests/cultist_tower");
    public CrimsonTowerFeature() {super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();var random=context.random();var origin=context.origin();
        int chance=ConciliumWorldgen.TOWER_CHANCE.get();
        if(!level.getLevel().dimension().equals(Level.OVERWORLD)||!CrimsonContent.dyes()
                ||!ConciliumWorldgen.GENERATE_TOWERS.get()||chance<=0||random.nextInt(chance)!=0)return false;
        return generate(level,origin,random);
    }
    /** Also exposed for isolated structure assertions; natural placement adds dependency, dimension and chance gates. */
    public static boolean generate(WorldGenLevel level,BlockPos origin,RandomSource random) {
        int x=origin.getX(),y=origin.getY(),z=origin.getZ();
        if(y<=level.getMinBuildHeight()||y+12>=level.getMaxBuildHeight())return false;
        // WorldGenRegion bounds prevent writing through an ungenerated neighbor or forcibly loading a live chunk.
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=7;dz++) {
            var pos=new BlockPos(x+dx,y,z+dz);
            if(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4)||!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.above(12)))return false;
        }
        // Retain the source's exact offset: hasValidPlatform(x - 2, y - 1, z + 3).
        for(int dx=-2;dx<=8;dx++)for(int dz=-4;dz<=5;dz++) {
            var state=level.getBlockState(new BlockPos(x+dx,y-1,z+dz));
            if(!state.is(Blocks.DIRT)&&!state.is(Blocks.GRASS_BLOCK)&&!state.is(Blocks.SAND)&&!state.is(Blocks.STONE))return false;
        }
        // Never replace a generated container/device from another structure while laying out this tower.
        for(int dx=-2;dx<=8;dx++)for(int dz=-7;dz<=3;dz++)for(int dy=0;dy<=11;dy++)
            if(level.getBlockState(origin.offset(dx,dy,dz)).hasBlockEntity())return false;
        CrimsonTowerLayout.place(level,x,y,z);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            level.setBlock(origin.offset(3+dx,9,-2+dz),Blocks.RED_TERRACOTTA.defaultBlockState(),2);
        level.setBlock(origin.offset(3,9,-2),Blocks.AIR.defaultBlockState(),2);
        for(var pos:new BlockPos[]{origin.offset(5,9,-2),origin.offset(1,9,-2),origin.offset(3,9,-4),origin.offset(3,9,0)})
            level.setBlock(pos,Blocks.RED_TERRACOTTA.defaultBlockState(),2);
        level.setBlock(origin.offset(3,0,-2),TCArcane.LEVITATOR.get().defaultBlockState(),2);
        // Native wall banners must have a supporting block. The old unsupported y+8 flags hang from the y+10 beam.
        banner(level,origin.offset(3,10,3),Direction.SOUTH);
        banner(level,origin.offset(3,10,-7),Direction.NORTH);
        banner(level,origin.offset(-2,10,-2),Direction.WEST);
        banner(level,origin.offset(8,10,-2),Direction.EAST);
        BlockPos chest=origin.offset(5,10,-2);
        level.setBlock(chest,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.WEST),2);
        if(level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity container)container.setLootTable(LOOT,random.nextLong());
        archer(level,origin.offset(3,11,-3));archer(level,origin.offset(5,11,-3));archer(level,origin.offset(3,11,0));
        int count=random.nextInt(5);
        for(int i=0;i<count;i++) {
            int sx=x-8+random.nextInt(16),sz=z-8+random.nextInt(16);
            int sy=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,sx,sz);
            archer(level,new BlockPos(sx,sy,sz));
        }
        return true;
    }
    private static void banner(WorldGenLevel level,BlockPos pos,Direction facing) {
        level.setBlock(pos,Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING,facing),2);
        if(level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
            banner.fromItem(CrimsonBanner.item(),DyeColor.RED);banner.setChanged();
        }
    }
    private static void archer(WorldGenLevel level,BlockPos pos) {
        if(!level.ensureCanWrite(pos))return;
        var archer=CrimsonContent.ARCHER.get().create(level.getLevel());if(archer==null)return;
        archer.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);archer.setPersistenceRequired();
        net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(archer,level,level.getCurrentDifficultyAt(pos),MobSpawnType.STRUCTURE,null,null);
        level.addFreshEntity(archer);
    }
}

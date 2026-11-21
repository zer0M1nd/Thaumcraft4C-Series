package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
public  class HilltopStones {
    private HilltopStones() {}
    public static boolean generate(net.minecraft.world.level.WorldGenLevel level,BlockPos ground,RandomSource random) {
        if(ground.getY()<85||ground.getY()+5>=level.getMaxBuildHeight())return false;
        for(int[] p:new int[][]{{-2,-2},{0,0},{2,0},{2,2},{0,2}})if(!thaumcraft.api.worldgen.NaturalGenerationRules.surface("thaumcraft:hilltop_stones",level,ground.offset(p[0],0,p[1]),()->LegacySurfaceRuins.valid(level,ground.offset(p[0],0,p[1]))))return false;
        var biome=level.getBiome(ground);var key=biome.unwrapKey().map(k->k.location().getPath()).orElse("");
        Block top=key.equals("mushroom_fields")?Blocks.MYCELIUM:LegacyBiomeProfiles.get(biome).sandySurface()?Blocks.SAND:Blocks.GRASS_BLOCK;
        boolean vines=biome.value().getPrecipitationAt(ground)!=net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
            if(Math.abs(x)==3&&Math.abs(z)==3)continue;var pos=ground.offset(x,0,z);
            level.setBlock(pos,(random.nextBoolean()?TCEldritch.OBSIDIAN_TILE.get():Blocks.OBSIDIAN).defaultBlockState(),3);boolean stop=false;
            for(int y=1;y<5;y++) {
                if(ground.getY()-y<0)continue;var below=pos.below(y);var state=level.getBlockState(below);
                if(state.isAir()||state.is(Blocks.SNOW)||state.is(Blocks.GRASS)||state.is(Blocks.FERN)||state.is(Blocks.DANDELION)||state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS))level.setBlock(below,top.defaultBlockState(),3);
                if(x==0&&z==0&&y==1) {
                    level.setBlock(pos.above(),TCEldritch.OBSIDIAN_TILE.get().defaultBlockState(),3);level.setBlock(pos.above(2),Blocks.CHEST.defaultBlockState(),3);
                    LegacySurfaceRuins.chest(level,pos.above(2),random,true);LegacySurfaceRuins.spawner(level,pos,TCEntities.WISP.get(),random);
                }
                if(stop||!((Math.abs(x)==3&&Math.abs(z%2)==1)||(Math.abs(z)==3&&Math.abs(x%2)==1)))continue;
                var part=pos.above(y);level.setBlock(part,TCArcane.OBSIDIAN_TOTEM.get().at(level,part),3);
                if(y<2||!random.nextBoolean())continue;stop=true;if(!vines)continue;
                for(var side:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH})if(random.nextInt(3)==0&&level.isEmptyBlock(part.relative(side)))growVines(level,part.relative(side),side.getOpposite());
            }
        }
        return true;
    }
    private static void growVines(net.minecraft.world.level.WorldGenLevel level,BlockPos pos,Direction attached) {
        var state=Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(attached),true);level.setBlock(pos,state,3);
        for(int n=0;n<4;n++) {pos=pos.below();if(!level.isEmptyBlock(pos))break;level.setBlock(pos,state,3);}
    }
}

package automagy.content;
import automagy.logic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import tc4revival.content.*;

/** Entire footprint stays within its origin chunk, with no retro-generation. */
public final class NetherSpireFeature extends Feature<NoneFeatureConfiguration> {
    public NetherSpireFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var random=context.random();int chance=automagy.AutomagyConfig.NETHER_SPIRE_CHANCE.get();
        if(chance<=0||!level.getLevel().dimension().equals(Level.NETHER)||random.nextInt(chance)!=0)return false;
        int x=(context.origin().getX()>>4)*16+random.nextInt(6),z=(context.origin().getZ()>>4)*16+random.nextInt(6);
        var y=NetherSpireSite.find(new NetherSpireSite.Column(){
            public int minimumY(){return level.getMinBuildHeight();}public int maximumY(){return level.getMaxBuildHeight();}
            public boolean lavaLayer(int y){return layer(level,x,y,z,true);}public boolean airLayer(int y){return layer(level,x,y,z,false);}
        });
        if(y.isEmpty())return false;generate(level,new BlockPos(x,y.getAsInt(),z),random);return true;
    }
    private static boolean layer(WorldGenLevel level,int x,int y,int z,boolean lava){
        for(int dx=0;dx<10;dx++)for(int dz=0;dz<10;dz++){
            var state=level.getBlockState(new BlockPos(x+dx,y,z+dz));
            if(lava?!state.is(Blocks.LAVA)||!state.getFluidState().isSource():!state.isAir())return false;
        }return true;
    }
    private static void generate(WorldGenLevel level,BlockPos origin,RandomSource random){
        int height=13-random.nextInt(5);
        for(int x=3;x<6;x++)for(int z=3;z<6;z++)column(level,origin.offset(x,0,z),height,random);
        for(int[] offset:new int[][]{{4,2},{6,4},{4,6},{2,4}})column(level,origin.offset(offset[0],0,offset[1]),height,random);
        for(int x:new int[]{3,5})for(int z:new int[]{3,5})totem(level,origin.offset(x,height,z),5,random);
        totem(level,origin.offset(4,height+3,4),5,random);
        for(int[] offset:new int[][]{{4,3},{3,4},{5,4},{4,5}})totem(level,origin.offset(offset[0],height,offset[1]),2,random);
        BlockPos center=origin.offset(4,height,4),nodePos=center.above(2);
        var amounts=new tc4revival.aspect.AspectAmounts();
        FieryNodeRoll.roll(LegacyBiomeProfiles.get(level.getBiome(nodePos)).node().aura(),random::nextInt,automagy.OptionalAspects.nether().map(tc4revival.aspect.Aspect::tag).orElse(null)).forEach((tag,amount)->amounts.add(tc4revival.aspect.TCAspects.catalog().require(tag),amount));
        AuraNodeGeneration.place(level,nodePos,new AuraNodeGeneration.Result(amounts,AuraNodeType.DARK,AuraNodeModifier.BRIGHT));
        level.setBlock(center.above(),Blocks.SPAWNER.defaultBlockState(),2);
        if(level.getBlockEntity(center.above()) instanceof SpawnerBlockEntity spawner)spawner.setEntityId(Content.NETHER_WISP.get(),random);
        level.setBlock(center,Blocks.CHEST.defaultBlockState(),2);
        if(level.getBlockEntity(center) instanceof ChestBlockEntity chest)chest.setLootTable(net.minecraft.resources.ResourceLocation.parse("automagy:chests/nether_spire"),random.nextLong());
    }
    private static void column(WorldGenLevel level,BlockPos pos,int height,RandomSource random){
        var cursor=pos.below();
        while(cursor.getY()>level.getMinBuildHeight()&&level.getFluidState(cursor).is(net.minecraft.tags.FluidTags.LAVA)){level.setBlock(cursor,stone(random),2);cursor=cursor.below();}
        for(int deep=0;deep<3&&cursor.getY()>level.getMinBuildHeight();deep++,cursor=cursor.below()){
            var state=level.getBlockState(cursor);if(!state.is(Blocks.NETHERRACK)&&!state.is(Blocks.SOUL_SAND)&&!state.is(Blocks.GRAVEL))break;
            level.setBlock(cursor,Blocks.OBSIDIAN.defaultBlockState(),2);
        }
        for(int up=0;up<height;up++)level.setBlock(pos.above(up),stone(random),2);
    }
    private static net.minecraft.world.level.block.state.BlockState stone(RandomSource random){
        if(random.nextInt(10)!=0)return Blocks.OBSIDIAN.defaultBlockState();
        return random.nextInt(4)==0?Content.RUNED_OBSIDIAN.get().defaultBlockState().setValue(RunedObsidianBlock.RUNE,random.nextInt(6)):TCEldritch.OBSIDIAN_TILE.get().defaultBlockState();
    }
    private static void totem(WorldGenLevel level,BlockPos pos,int height,RandomSource random){
        for(int up=0;up<height-2;up++)level.setBlock(pos.above(up),TCArcane.OBSIDIAN_TOTEM.get().at(level,pos.above(up)),2);
        level.setBlock(pos.above(height-2),Content.RUNED_OBSIDIAN.get().defaultBlockState().setValue(RunedObsidianBlock.RUNE,random.nextInt(6)),2);
        level.setBlock(pos.above(height-1),Blocks.FIRE.defaultBlockState(),2);
    }
}

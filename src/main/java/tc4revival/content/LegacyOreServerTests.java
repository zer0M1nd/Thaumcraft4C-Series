package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class LegacyOreServerTests {
    @GameTest(template="wand_test_empty")
    public static void oresHarvestFortuneSilkAndExperience(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));var tool=new ItemStack(Items.DIAMOND_PICKAXE);var silk=tool.copy();silk.enchant(Enchantments.SILK_TOUCH,1);tool.enchant(Enchantments.BLOCK_FORTUNE,3);
        for(int metadata=0;metadata<8;metadata++){
            var block=TCArcane.ore(metadata);var state=block.defaultBlockState();level.setBlockAndUpdate(pos,state);
            var silkDrops=Block.getDrops(state,level,pos,null,null,silk);test.assertTrue(silkDrops.size()==1&&silkDrops.get(0).is(block.asItem())&&block.getExpDrop(state,level,RandomSource.create(2),pos,3,1)==0,"Silk touch keeps exact ore and gives no XP");
            for(int attempt=0;attempt<24;attempt++){
                var drops=Block.getDrops(state,level,pos,null,null,tool);int count=drops.stream().mapToInt(ItemStack::getCount).sum();
                test.assertTrue(count>=1&&count<=(metadata==0?1:metadata==7?4:5),"Old fortune quantity bounds");
                var expected=metadata==0?block.asItem():metadata==7?TCItems.material(MaterialType.AMBER):TCItems.shard(ShardType.values()[metadata-1]);test.assertTrue(drops.stream().allMatch(s->s.is(expected)),"Correct ore, amber or primal shard drop");
                if(metadata>0&&metadata<7)test.assertTrue(drops.stream().allMatch(s->s.getCount()==1),"Infused ore retains separate single-shard drop stacks");
                int xp=block.getExpDrop(state,level,level.random,pos,3,0);test.assertTrue(metadata==0?xp==0:metadata==7?xp>=1&&xp<=4:xp>=0&&xp<=3,"Original ore XP ranges");
            }
            test.assertTrue(new ItemStack(block).is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse(LegacyOreBlock.tag(metadata)))),"Modern ore category exists");
        }
        pass(test,"harvest-fortune-silk-xp-tags");
    }
    @GameTest(template="wand_test_empty")
    public static void oresSmeltingAndInfernalBonus(GameTestHelper test){
        var level=test.getLevel();for(int metadata:new int[]{0,7}){
            var input=new ItemStack(TCArcane.ore(metadata));var container=new net.minecraft.world.SimpleContainer(input);var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,container,level).orElseThrow();
            test.assertTrue(recipe.assemble(container,level.registryAccess()).is(TCItems.material(metadata==0?MaterialType.QUICKSILVER:MaterialType.AMBER))&&recipe.getExperience()==1&&recipe.getCookingTime()==200,"Original smelting result, XP and vanilla duration");
        }
        test.assertTrue(InfernalBonusRecipe.find(level,new ItemStack(TCArcane.ore(0))).isPresent(),"Cinnabar category activates existing infernal quicksilver-drop bonus");pass(test,"smelting-infernal-bonus");
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void oresGenerationStoneSubstrate(GameTestHelper test){
        var level=test.getLevel();var anchor=test.absolutePos(new BlockPos(0,0,0));var chunk=new net.minecraft.world.level.ChunkPos(anchor);int bx=chunk.getMinBlockX(),bz=chunk.getMinBlockZ();
        for(int x=0;x<24;x++)for(int z=0;z<24;z++)for(int y=0;y<12;y++)level.setBlock(new BlockPos(bx+x,y,bz+z),Blocks.STONE.defaultBlockState(),2);
        var controlled=new net.minecraft.world.level.levelgen.LegacyRandomSource(7){@Override public int nextInt(int bound){return bound==51?4:bound==16?1:super.nextInt(bound);}};
        var branch=tc4revival.config.TCConfig.SERVER.branchFeatures;boolean previous=branch.get();
        try{branch.set(false);NaturalOreGeneration.generate(level,chunk,controlled,true);}finally{branch.set(previous);}
        test.assertTrue(level.getBlockState(new BlockPos(bx+1,4,bz+1)).is(TCArcane.ore(0)),"Cinnabar attempts place ore in old low stone range when branch is off");
        int infused=0;for(int x=0;x<24;x++)for(int z=0;z<24;z++)for(int y=0;y<12;y++){var block=level.getBlockState(new BlockPos(bx+x,y,bz+z)).getBlock();if(block instanceof LegacyOreBlock ore&&ore.metadata()>0&&ore.metadata()<7)infused++;}
        test.assertTrue(infused>0,"Eight real infused vein attempts produce stone deposits");
        var origin=new BlockPos(bx,30,bz);for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=26;y<36;y++)level.setBlock(new BlockPos(bx+x,y,bz+z),Blocks.DEEPSLATE.defaultBlockState(),2);
        test.assertTrue(!ForestOresFeature.vein(level,RandomSource.create(9),origin,TCArcane.ore(2),6),"New deepslate is not implicitly adapted to old stone-only veins");pass(test,"generation-stone-substrate");
    }
    @GameTest(template="wand_test_empty")
    public static void oresRegenerationMarkerAndSeed(GameTestHelper test){
        var nbt=new net.minecraft.nbt.CompoundTag();test.assertTrue(ChunkRegeneration.eligible(nbt,"A",true)&&!ChunkRegeneration.eligible(nbt,"A",false),"Missing marker only queues when regeneration enabled");
        var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("A",false);nbt.put("Thaumcraft",tag);test.assertTrue(!ChunkRegeneration.eligible(nbt,"A",true)&&ChunkRegeneration.eligible(nbt,"B",true),"Old marker tests presence rather than boolean value; changed key is eligible");
        var level=test.getLevel();var chunk=level.getChunkAt(test.absolutePos(new BlockPos(0,0,0)));ChunkRegeneration.save(new net.minecraftforge.event.level.ChunkDataEvent.Save(chunk,level,nbt));
        test.assertTrue(nbt.getCompound("Thaumcraft").getBoolean(tc4revival.config.TCConfig.SERVER.regenerationKey.get())&&nbt.getCompound("Thaumcraft").getAllKeys().size()==1,"Real chunk save handler writes only current key, as in old TC4");
        long world=123456789L;var pos=new net.minecraft.world.level.ChunkPos(-17,29);var random=new java.util.Random(world);long expected=(random.nextLong()>>3)*pos.x+(random.nextLong()>>3)*pos.z^world;
        test.assertTrue(ChunkRegeneration.seed(world,pos)==expected&&ChunkRegeneration.seed(world,pos)==ChunkRegeneration.seed(world,pos),"Signed shifts and overflow follow original Java seed formula");pass(test,"regeneration-marker-seed");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Legacy ores test passed: {}",name);test.succeed();}
}

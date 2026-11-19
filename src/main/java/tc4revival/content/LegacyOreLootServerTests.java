package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.TC4Revival;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class LegacyOreLootServerTests {
    @GameTest(template="wand_test_empty")
    public static void legacyOreTablesPreserveFortuneAndSilk(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"OreLootTest"));
        for(int metadata=0;metadata<8;metadata++) {
            var block=TCArcane.ore(metadata); var state=block.defaultBlockState();
            var table=level.getServer().getLootData().getLootTable(ResourceLocation.parse("thaumcraft:blocks/"+LegacyOreBlock.path(metadata)));
            for(int fortune=0;fortune<=3;fortune++) {
                var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                if(fortune>0)tool.enchant(Enchantments.BLOCK_FORTUNE,fortune);
                var counts=new java.util.HashSet<Integer>();
                for(int sample=0;sample<48;sample++) {
                    long seed=5000+sample*97L;
                    int expected=metadata==0?1:1+RandomSource.create(seed).nextInt((metadata==7?1:2)+fortune);
                    level.random.setSeed(seed);
                    var drops=Block.getDrops(state,level,pos,null,player,tool);
                    int total=drops.stream().mapToInt(ItemStack::getCount).sum(); counts.add(total);
                    test.assertTrue(total==expected,"Old uniform count matches world random draw, metadata="+metadata+", fortune="+fortune);
                    var item=metadata==0?block.asItem():metadata==7?TCItems.material(MaterialType.AMBER):TCItems.shard(ShardType.values()[metadata-1]);
                    test.assertTrue(drops.stream().allMatch(drop->drop.is(item)),"Correct old material for each ore");
                    test.assertTrue(metadata>0 && metadata<7?drops.size()==expected && drops.stream().allMatch(drop->drop.getCount()==1):drops.size()==1,
                            "Shards remain individual entries; amber remains one stack");
                    var params=new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE,state)
                            .withParameter(LootContextParams.TOOL,tool).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos))
                            .withParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.BLOCK);
                    level.random.setSeed(seed);
                    var direct=table.getRandomItems(params);
                    test.assertTrue(direct.size()==drops.size() && direct.stream().mapToInt(ItemStack::getCount).sum()==expected
                            && direct.stream().allMatch(drop->drop.is(item)),"Generated table itself supplies complete behavior, without block override");
                }
                int upper=metadata==0?1:(metadata==7?1:2)+fortune;
                test.assertTrue(counts.contains(1) && counts.contains(upper),"Sample set reaches inclusive old count bounds");
                tool.enchant(Enchantments.SILK_TOUCH,1);
                var silk=Block.getDrops(state,level,pos,null,player,tool);
                test.assertTrue(silk.size()==1 && silk.get(0).is(block.asItem()) && silk.get(0).getCount()==1,
                        "Silk touch takes priority over fortune and returns one ore");
            }
            for(int sample=0;sample<24;sample++) {
                int xp=state.getExpDrop(level,level.random,pos,3,0);
                test.assertTrue(metadata==0?xp==0:metadata==7?xp>=1&&xp<=4:xp>=0&&xp<=3,"Legacy ore XP bounds remain unchanged");
                test.assertTrue(state.getExpDrop(level,level.random,pos,3,1)==0,"Silk touch awards no XP");
            }
        }
        TC4Revival.LOGGER.info("Legacy ore tables PASS: eight-ores-fortune-uniform-bounds-stack-shape-direct-tables-silk-xp");
        test.succeed();
    }
}
